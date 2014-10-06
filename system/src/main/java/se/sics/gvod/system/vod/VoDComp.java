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
package se.sics.gvod.system.vod;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.client.BootstrapClientPort;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.AddOverlay;
import se.sics.gvod.common.msg.impl.BootstrapGlobal;
import se.sics.gvod.common.msg.impl.JoinOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.common.util.HashUtil;
import se.sics.gvod.manager.DownloadFileInfo;
import se.sics.gvod.manager.UploadFileInfo;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.system.video.VideoComp;
import se.sics.gvod.system.video.VideoConfig;
import se.sics.gvod.system.video.connMngr.ConnMngr;
import se.sics.gvod.system.video.connMngr.ConnMngrConfig;
import se.sics.gvod.system.video.connMngr.SimpleConnMngr;
import se.sics.gvod.system.video.downloadMngr.DownloadMngr;
import se.sics.gvod.system.video.downloadMngr.SimpleDownloadMngr;
import se.sics.gvod.system.video.playMngr.PlayMngr;
import se.sics.gvod.system.video.storage.CompletePieceTracker;
import se.sics.gvod.system.video.storage.FileMngr;
import se.sics.gvod.system.video.storage.PieceTracker;
import se.sics.gvod.system.video.storage.SimpleFileMngr;
import se.sics.gvod.system.video.storage.SimplePieceTracker;
import se.sics.gvod.system.video.storage.Storage;
import se.sics.gvod.system.video.storage.StorageFactory;
import se.sics.gvod.system.vod.msg.DownloadVideo;
import se.sics.gvod.system.vod.msg.UploadVideo;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VoDComp extends ComponentDefinition {

    //TODO ALEX fix resume. for the moment everyone who joins starts download from 0
    public static int noDownloadResume = 0;

    private static final Logger log = LoggerFactory.getLogger(VoDComp.class);

    Positive<Timer> timer = requires(Timer.class);
    Positive<VodNetwork> network = requires(VodNetwork.class);
    Positive<BootstrapClientPort> bootstrap = requires(BootstrapClientPort.class);
    Negative<VoDPort> myPort = provides(VoDPort.class);

    private final VoDConfig config;

    private final Map<Integer, Component> videoComps;
    private final Map<Integer, PlayMngr> videoPlayMngrs;

    private final Map<UUID, Pair<String, FileMetadata>> pendingUploads;
    private final Map<UUID, DownloadFileInfo> pendingDownloads;

    public VoDComp(VoDInit init) {
        log.debug("init");
        this.config = init.config;
        this.videoComps = new HashMap<Integer, Component>();
        this.videoPlayMngrs = new HashMap<Integer, PlayMngr>();
        this.pendingDownloads = new HashMap<UUID, DownloadFileInfo>();
        this.pendingUploads = new HashMap<UUID, Pair<String, FileMetadata>>();

        subscribe(handleBootstrapGlobalResponse, bootstrap);
        subscribe(handleUploadVideoRequest, myPort);
        subscribe(handleDownloadVideoRequest, myPort);
        subscribe(handleAddOverlayResponse, bootstrap);
        subscribe(handleJoinOverlayResponse, bootstrap);
    }

    public Handler<BootstrapGlobal.Response> handleBootstrapGlobalResponse = new Handler<BootstrapGlobal.Response>() {
        @Override
        public void handle(BootstrapGlobal.Response event) {
        }
    };

    public Handler<UploadVideo.Request> handleUploadVideoRequest = new Handler<UploadVideo.Request>() {

        @Override
        public void handle(UploadVideo.Request req) {
            log.info("{} - {} - overlay: {}", new Object[]{config.selfAddress, req, req.fileInfo.overlayId});
            String video = req.fileInfo.fileName;
            String videoName = video.substring(0, video.indexOf("."));
            String videoExt = video.substring(video.indexOf("."));
            String videoFilePath = config.libDir + File.separator + video;
            String hashFilePath = config.libDir + File.separator + videoName + ".hash";

            File videoFile = new File(videoFilePath);
            File hashFile = new File(hashFilePath);
            if (hashFile.exists()) {
                hashFile.delete();
            }
            try {
                hashFile.createNewFile();
                HashUtil.makeHashes(videoFilePath, hashFilePath, config.hashAlg, config.pieceSize);
            } catch (HashUtil.HashBuilderException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            FileMetadata fileMeta = new FileMetadata((int) videoFile.length(), config.pieceSize, config.hashAlg, (int) hashFile.length());

            trigger(new AddOverlay.Request(req.id, req.fileInfo.overlayId, fileMeta), bootstrap);
            pendingUploads.put(req.id, Pair.with(req.fileInfo.fileName, fileMeta));
        }
    };

    public Handler<DownloadVideo.Request> handleDownloadVideoRequest = new Handler<DownloadVideo.Request>() {

        @Override
        public void handle(DownloadVideo.Request req) {
            log.info("{} - {} - overlay: {}", new Object[]{config.selfAddress, req, req.fileInfo.overlayId});
            trigger(new JoinOverlay.Request(req.id, req.fileInfo.overlayId, noDownloadResume), bootstrap);
            pendingDownloads.put(req.id, req.fileInfo);
        }
    };

    public Handler<AddOverlay.Response> handleAddOverlayResponse = new Handler<AddOverlay.Response>() {

        @Override
        public void handle(AddOverlay.Response resp) {
            log.trace("{} - {}", new Object[]{config.selfAddress, resp});

            if (resp.status == ReqStatus.SUCCESS) {
                try {
                    Pair<String, FileMetadata> fileInfo = pendingUploads.remove(resp.id);
                    Pair<DownloadMngr, PlayMngr> videoMngrs = getUploadVideoMngrs(fileInfo.getValue0(), fileInfo.getValue1());
                    videoPlayMngrs.put(resp.overlayId, videoMngrs.getValue1());
                    startVideoComp(resp.overlayId, videoMngrs.getValue0(), new HashMap<VodAddress, Integer>());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (HashUtil.HashBuilderException ex) {
                    throw new RuntimeException(ex);
                } catch (GVoDConfigException.Missing ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    };

    public Handler<JoinOverlay.Response> handleJoinOverlayResponse = new Handler<JoinOverlay.Response>() {

        @Override
        public void handle(JoinOverlay.Response resp) {
            log.trace("{} - {} - peers:{}", new Object[]{config.selfAddress, resp, resp.overlaySample});

            if (resp.status == ReqStatus.SUCCESS) {
                try {
                    DownloadFileInfo fileInfo = pendingDownloads.remove(resp.id);
                    Pair<DownloadMngr, PlayMngr> videoMngrs = getDownloadVideoMngrs(fileInfo.fileName, resp.fileMeta);
                    videoPlayMngrs.put(resp.overlayId, videoMngrs.getValue1());
                    startVideoComp(resp.overlayId, videoMngrs.getValue0(), resp.overlaySample);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (HashUtil.HashBuilderException ex) {
                    throw new RuntimeException(ex);
                } catch (GVoDConfigException.Missing ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    };

    private void startVideoComp(int overlayId, DownloadMngr videoMngr, Map<VodAddress, Integer> overlaySample) {
        VideoConfig videoConfig;
        ConnMngrConfig connMngrConfig;

        try {
            videoConfig = config.getVideoConfig().setDownloader(false).finalise();
            connMngrConfig = config.getConnMngrConfig().finalise();
        } catch (GVoDConfigException.Missing ex) {
            throw new RuntimeException(ex);
        }

        ConnMngr connMngr = new SimpleConnMngr(connMngrConfig);
        Component video = create(VideoComp.class, new VideoComp.VideoInit(videoConfig, connMngr, videoMngr, overlaySample));
        videoComps.put(overlayId, video);

        connect(video.getNegative(Timer.class), timer);
        connect(video.getNegative(VodNetwork.class), network);

        trigger(Start.event, video.control());
    }

    private Pair<DownloadMngr, PlayMngr> getUploadVideoMngrs(String video, FileMetadata fileMeta) throws IOException, HashUtil.HashBuilderException, GVoDConfigException.Missing {

        String videoName = video.substring(0, video.indexOf("."));
        String videoExt = video.substring(video.indexOf("."));
        String videoFilePath = config.libDir + File.separator + video;
        String hashFilePath = config.libDir + File.separator + videoName + ".hash";

        int hashPieces = fileMeta.hashFileSize / HashUtil.getHashSize(fileMeta.hashAlg) + 1;
        PieceTracker hashPieceTracker = new CompletePieceTracker(hashPieces);
        Storage hashStorage = StorageFactory.getExistingFile(hashFilePath, HashUtil.getHashSize(fileMeta.hashAlg));
        FileMngr hashMngr = new SimpleFileMngr(hashStorage, hashPieceTracker);

        Storage videoStorage = StorageFactory.getExistingFile(videoFilePath, config.pieceSize);
        int filePieces = fileMeta.fileSize / fileMeta.pieceSize + 1;
        PieceTracker videoPieceTracker = new SimplePieceTracker(filePieces);
        FileMngr fileMngr = new SimpleFileMngr(videoStorage, videoPieceTracker);
        DownloadMngr videoDownMngr = new SimpleDownloadMngr(fileMeta, hashMngr, fileMngr);

        PlayMngr videoPlayMngr = new PlayMngr(fileMeta, videoStorage);

        return Pair.with(videoDownMngr, videoPlayMngr);
    }

    private Pair<DownloadMngr, PlayMngr> getDownloadVideoMngrs(String video, FileMetadata fileMeta) throws IOException, HashUtil.HashBuilderException, GVoDConfigException.Missing {
        String videoName = video.substring(0, video.indexOf("."));
        String videoExt = video.substring(video.indexOf("."));
        String videoFilePath = config.libDir + File.separator + video;
        String hashFilePath = config.libDir + File.separator + videoName + ".hash";

        File hashFile = new File(hashFilePath);
        if (hashFile.exists()) {
            hashFile.delete();
        }
        PieceTracker hashPieceTracker = new SimplePieceTracker(fileMeta.hashFileSize);
        Storage hashStorage = StorageFactory.getEmptyFile(hashFilePath, fileMeta.hashFileSize, HashUtil.getHashSize(fileMeta.hashAlg));
        FileMngr hashMngr = new SimpleFileMngr(hashStorage, hashPieceTracker);

        Storage videoStorage = StorageFactory.getEmptyFile(videoFilePath, fileMeta.fileSize, fileMeta.pieceSize);
        PieceTracker videoPieceTracker = new SimplePieceTracker(fileMeta.fileSize);
        FileMngr fileMngr = new SimpleFileMngr(videoStorage, videoPieceTracker);
        DownloadMngr videoDownMngr = new SimpleDownloadMngr(fileMeta, hashMngr, fileMngr);

        PlayMngr videoPlayMngr = new PlayMngr(fileMeta, videoStorage);
        return Pair.with(videoDownMngr, videoPlayMngr);
    }
}
