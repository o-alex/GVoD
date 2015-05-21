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

import com.google.common.util.concurrent.SettableFuture;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.common.utility.UtilityUpdatePort;
import se.sics.gvod.core.VoDPort;
import se.sics.gvod.core.msg.DownloadVideo;
import se.sics.gvod.core.msg.GetLibrary;
import se.sics.gvod.core.msg.PlayReady;
import se.sics.gvod.core.msg.UploadVideo;
import se.sics.gvod.core.util.ResponseStatus;
import se.sics.gvod.manager.util.FileStatus;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.p2ptoolbox.videostream.VideoStreamManager;
import se.sics.p2ptoolbox.videostream.http.BaseHandler;
import se.sics.p2ptoolbox.videostream.http.JwHttpServer;
import se.sics.p2ptoolbox.videostream.http.RangeCapableMp4Handler;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VoDManagerImpl extends ComponentDefinition implements VoDManager {

    private static final Logger LOG = LoggerFactory.getLogger(VoDManager.class);

    private final Positive<VoDPort> vodPort = requires(VoDPort.class);
    private final Positive<UtilityUpdatePort> utilityPort = requires(UtilityUpdatePort.class);

    private final VoDManagerConfig config;
    private final Random rand = new Random();
    private final String logPrefix;

    private final Map<String, VideoStreamManager> vsMngrs;
    private final Set<String> videoPaths;
    private Integer videoPort = null;
    private InetSocketAddress httpAddr;

    private Map<UUID, SettableFuture> pendingJobs;
    private Set<String> pendingUploads;
    private Map<String, FileStatus> videos;

    public VoDManagerImpl(VoDManagerInit init) {
        this.config = init.config;
        this.logPrefix = config.selfAddress.toString();
        LOG.info("{} initiating...", logPrefix);

        this.vsMngrs = new ConcurrentHashMap<String, VideoStreamManager>();
        this.videoPaths = new HashSet<String>();

        this.pendingJobs = new HashMap<UUID, SettableFuture>();
        this.pendingUploads = new HashSet<String>();
        this.videos = new HashMap<String, FileStatus>();

        subscribe(handlePlayReady, vodPort);
        subscribe(handleGetLibraryResponse, vodPort);
    }

    @Override
    public void getFiles(SettableFuture<Map<String, FileStatus>> myFuture) {
        LOG.debug("{} get files request", logPrefix);
        GetLibrary.Request req = new GetLibrary.Request(UUID.randomUUID());
        pendingJobs.put(req.reqId, myFuture);
        trigger(req, vodPort);
    }

    private Handler handleGetLibraryResponse = new Handler<GetLibrary.Response>() {
        @Override
        public void handle(GetLibrary.Response resp) {
            SettableFuture<Map<String, FileStatus>> myFuture = (SettableFuture<Map<String, FileStatus>>) pendingJobs.remove(resp.reqId);
            if (resp.respStatus.equals(ResponseStatus.SUCCESS)) {
                LOG.debug("{} get files response", logPrefix);
                videos = convert(resp.fileStatusMap);
                for (String pendingFile : pendingUploads) {
                    videos.put(pendingFile, FileStatus.PENDING);
                }
                if (myFuture != null) {
                    myFuture.set(videos);
                }
            } else {
                LOG.warn("{} bad status", logPrefix);
                if (myFuture != null) {
                    myFuture.setException(new RuntimeException("bad status " + resp.respStatus));
                }
            }
        }
    };

    private Map<String, FileStatus> convert(Map<String, Pair<se.sics.gvod.core.util.FileStatus, Integer>> fileStatusMap) {
        Map<String, FileStatus> convertedFileStatusMap = new HashMap<String, FileStatus>();
        for (Map.Entry<String, Pair<se.sics.gvod.core.util.FileStatus, Integer>> e : fileStatusMap.entrySet()) {
            FileStatus fileStatus = null;
            switch (e.getValue().getValue0()) {
                case PAUSED:
                    fileStatus = FileStatus.DOWNLOADING;
                    break;
                case PENDING_DOWNLOAD:
                    fileStatus = FileStatus.DOWNLOADING;
                    break;
                case DOWNLOADING:
                    fileStatus = FileStatus.DOWNLOADING;
                    break;
                case PENDING_UPLOAD:
                    fileStatus = FileStatus.PENDING;
                    break;
                case UPLOADING:
                    fileStatus = FileStatus.UPLOADING;
                    break;
                case NONE:
                    fileStatus = FileStatus.NONE;
                    break;
                default:
                    break;
            }
            convertedFileStatusMap.put(e.getKey(), fileStatus);
        }
        return convertedFileStatusMap;
    }

    @Override
    public void pendingUpload(String videoName, SettableFuture<Boolean> myFuture) {
        FileStatus videoStatus = videos.get(videoName);
        if(videoStatus == null) {
            LOG.warn("{} video:{} not found in library:{}", new Object[]{logPrefix, videoName, config.libDir});
            myFuture.set(false);
        }
        if (!videoStatus.equals(FileStatus.NONE)) {
            LOG.warn("{} video:{} has status:{} - cannot upload", new Object[]{logPrefix, videoName, videoStatus});
            myFuture.set(false);
            return;
        }
        LOG.debug("{} video:{} - pending upload", logPrefix, videoName);
        videos.put(videoName, FileStatus.PENDING);
        pendingUploads.add(videoName);
        myFuture.set(true);
        return;
    }

    @Override
    public void uploadVideo(String videoName, int overlayId, SettableFuture<Boolean> myFuture) {
        if (pendingUploads.remove(videoName == null)) {
            LOG.warn("{} video not pending upload - cannot initiate upload", new Object[]{config.selfAddress, videoName, config.libDir});
            myFuture.set(false);
            return;
        }
        LOG.info("{} video:{} - upload request", logPrefix, videoName);
        videos.put(videoName, FileStatus.PENDING);
        UploadVideo.Request req = new UploadVideo.Request(videoName, overlayId);
        trigger(req, vodPort);
        pendingJobs.put(req.id, myFuture);
    }

    @Override
    public void downloadVideo(String videoName, int overlayId, SettableFuture<Boolean> myFuture) {
        FileStatus videoStatus = videos.get(videoName);
        if (videoStatus == FileStatus.DOWNLOADING || videoStatus == FileStatus.UPLOADING || videoStatus == FileStatus.PENDING) {
            myFuture.set(true);
        }
        LOG.info("{} video:{} - download request", logPrefix, videoName);
        videos.put(videoName, FileStatus.PENDING);
        DownloadVideo.Request req = new DownloadVideo.Request(videoName, overlayId);
        trigger(req, vodPort);
        pendingJobs.put(req.id, myFuture);
    }

    private Handler<PlayReady> handlePlayReady = new Handler<PlayReady>() {

        @Override
        public void handle(PlayReady resp) {
            LOG.info("{} video:{} ready to play", logPrefix, resp.videoName);
            vsMngrs.put(resp.videoName, resp.vsMngr);
            SettableFuture<Boolean> myFuture = pendingJobs.remove(resp.id);
            if (myFuture == null) {
                LOG.error("missing job");
                throw new RuntimeException("missing job");
            }
            myFuture.set(true);
        }
    };

    @Override
    public void playVideo(String videoName, int overlayId, SettableFuture<Integer> myFuture) {
        LOG.info("{} play video:{}", logPrefix, videoName);
        if (videoPort == null) {
            do {
                videoPort = tryPort(10000 + rand.nextInt(40000));
            } while (videoPort == -1);
        }

        VideoStreamManager videoPlayer = vsMngrs.get(videoName);
        if (videoPlayer == null) {
            LOG.error("logic error on video manager - video player");
            System.exit(1);
        }

        if (videoPaths.contains(videoName)) {
            LOG.info("return play");
            myFuture.set(videoPort);
            return;
        }

        LOG.info("setting up player for video:{}", videoName);
        httpAddr = new InetSocketAddress(videoPort);
        setupPlayerHttpConnection(videoPlayer, videoName);
        videoPaths.add(videoName);

        LOG.info("return play");
        myFuture.set(videoPort);
        return;
    }

    @Override
    public void stopVideo(String videoName, SettableFuture<Boolean> myFuture) {
        LOG.info("received stop");
        VideoStreamManager vsMngr = vsMngrs.get(videoName);
        if (vsMngr == null) {
            LOG.info("player for video:{} is not ready yet - weird stop message", videoName);
            myFuture.set(true);
            return;
        } else {
            LOG.info("stopping play for video:{}", videoName);
            vsMngr.stop();
            LOG.info("return stop");
            myFuture.set(true);
            return;
        }
    }

    private void setupPlayerHttpConnection(VideoStreamManager vsMngr, String videoName) {
        LOG.info("{} starting player http connection http://127.0.0.1:{}/{}/{}", new Object[]{config.selfAddress, videoPort, videoName, videoName});
        String fileName = "/" + videoName + "/";
        BaseHandler handler = new RangeCapableMp4Handler(vsMngr);
        try {
            JwHttpServer.startOrUpdate(httpAddr, fileName, handler);
        } catch (IOException ex) {
            LOG.error("http server error");
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
                    LOG.error("error while picking port");
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
