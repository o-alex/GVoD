/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.gvod.manager;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.common.utility.UtilityUpdatePort;
import se.sics.gvod.manager.util.FileStatus;
import se.sics.gvod.core.VoDPort;
import se.sics.gvod.core.msg.DownloadVideo;
import se.sics.gvod.core.msg.PlayReady;
import se.sics.gvod.core.msg.UploadVideo;
import se.sics.gvod.videoplugin.VideoPlayer;
import se.sics.gvod.videoplugin.jwplayer.BaseHandler;
import se.sics.gvod.videoplugin.jwplayer.JwHttpServer;
import se.sics.gvod.videoplugin.jwplayer.Mp4Handler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VoDManagerImpl extends ComponentDefinition implements VoDManager {

    private static final Logger log = LoggerFactory.getLogger(VoDManager.class);

    private final Positive<VoDPort> vodPort = requires(VoDPort.class);
    private final Positive<UtilityUpdatePort> utilityPort = requires(UtilityUpdatePort.class);

    private final VoDManagerConfig config;
    private final Random rand = new Random();

    private final Map<String, FileStatus> videos;
    private final Map<String, VideoPlayer> videoPlayers;
    private Integer videoPort = null;
    private InetSocketAddress httpAddr;

    public VoDManagerImpl(VoDManagerInit init) {
        this.config = init.config;
        this.videos = new ConcurrentHashMap<String, FileStatus>();
        this.videoPlayers = new ConcurrentHashMap<String, VideoPlayer>();
        reloadLibrary();

        subscribe(handlePlayReady, vodPort);
    }

    private Handler<PlayReady> handlePlayReady = new Handler<PlayReady>() {

        @Override
        public void handle(PlayReady event) {
            log.info("video:{} ready to play", event.videoPlayer.getVideoName());
            videoPlayers.put(event.videoPlayer.getVideoName(), event.videoPlayer);
        }
    };

    @Override
    public void reloadLibrary() {
        log.info("loading library folder:{}", config.libDir);

        File dir = new File(config.libDir);
        if (!dir.isDirectory()) {
            log.error("library path is invalid");
            System.exit(1);
        }

        FileFilter mp4Filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (!file.isFile()) {
                    return false;
                }
                return file.getName().endsWith(".mp4");
            }
        };

        for (File video : dir.listFiles(mp4Filter)) {
            log.info("library - loading video: {}", video.getName());
            if (!videos.containsKey(video.getName())) {
                videos.put(video.getName(), FileStatus.NONE);
            }
        }
    }

    @Override
    public Map<String, FileStatus> getFiles() {
        log.info("returning library content");
        return new HashMap<String, FileStatus>(videos);
    }

    @Override
    public boolean pendingUpload(String videoName) {
        FileStatus videoStatus = videos.get(videoName);
        if (videoStatus == null || !videoStatus.equals(FileStatus.NONE)) {
            log.warn("{} video {} not found in library {}", new Object[]{config.selfAddress, videoName, config.libDir});
            return false;
        }
        log.info("{} video {} found - pending upload", config.selfAddress, videoName);
        videos.put(videoName, FileStatus.PENDING);
        return true;
    }

    @Override
    public boolean uploadVideo(String videoName, int overlayId) {
        FileStatus videoStatus = videos.get(videoName);
        if (videoStatus == null || !videoStatus.equals(FileStatus.PENDING)) {
            log.warn("{} video not pending upload - cannot initiate upload", new Object[]{config.selfAddress, videoName, config.libDir});
            return false;
        }
        log.info("{} video {} found - uploading", config.selfAddress, videoName);
        videos.put(videoName, FileStatus.UPLOADING);
        trigger(new UploadVideo.Request(videoName, overlayId), vodPort);
        return true;
    }

    @Override
    public boolean downloadVideo(String videoName, int overlayId) {
        FileStatus videoStatus = videos.get(videoName);
        if (videoStatus != null) {
            return false;
        }
        videos.put(videoName, FileStatus.DOWNLOADING);
        trigger(new DownloadVideo.Request(videoName, overlayId), vodPort);
        do {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                log.error("threading problem in VoDManagerImpl");
                System.exit(1);
            }
        } while (!videoPlayers.containsKey(videoName));
        return true;
    }

    @Override
    public Integer playVideo(String videoName) {
        VideoPlayer videoPlayer = videoPlayers.get(videoName);
        if (videoPlayer == null) {
            log.info("player for video:{} is not ready yet", videoName);
            return null;
        } else {
            log.info("setting up player for video:{}", videoName);
        }

        if (videoPort == null) {
            do {
                videoPort = tryPort(10000 + rand.nextInt(40000));
            } while (videoPort == -1);
        }
        httpAddr = new InetSocketAddress(videoPort);
        setupPlayerHttpConnection(videoPlayer, videoName);

        return videoPort;
    }

    @Override
    public void stopVideo(String videoName) {
        VideoPlayer videoPlayer = videoPlayers.get(videoName);
        if (videoPlayer == null) {
            log.info("player for video:{} is not ready yet - weird stop message", videoName);
            return;
        } else {
            log.info("stopping play for video:{}", videoName);
            videoPlayer.stop();
            //TODO Alex close player http server in order not to keep ports used.
            return;
        }
    }

    private void setupPlayerHttpConnection(VideoPlayer playMngr, String videoName) {
        String httpPath = "http://127.0.0.1:" + videoPort + "/" + videoName + "/" + videoName;
        log.info("{} starting player http connection {}", new Object[]{config.selfAddress, httpPath});
        String fileName = "/" + videoName + "/";
        BaseHandler handler = new Mp4Handler(playMngr);
        try {
            JwHttpServer.startOrUpdate(httpAddr, fileName, handler);
        } catch (IOException ex) {
            log.error("http server error");
            System.exit(1);
        }
    }

    private int tryPort(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
        } catch (IOException e) {
            return -1;
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                    log.error("error while picking port");
                    System.exit(1);
                }
            }
        }
        return port;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    public static class VoDManagerInit extends Init<VoDManagerImpl> {

        public final VoDManagerConfig config;

        public VoDManagerInit(VoDManagerConfig config) {
            this.config = config;
        }
    }
}
