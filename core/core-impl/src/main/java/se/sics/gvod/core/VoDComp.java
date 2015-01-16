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
package se.sics.gvod.core;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.client.BootstrapClientPort;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.AddOverlay;
import se.sics.gvod.common.msg.peerMngr.BootstrapGlobal;
import se.sics.gvod.common.msg.peerMngr.JoinOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.common.util.HashUtil;
import se.sics.gvod.common.utility.UtilityUpdatePort;
import se.sics.gvod.croupierfake.CroupierComp;
import se.sics.gvod.croupierfake.CroupierPort;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.network.filters.OverlayFilter;
import se.sics.gvod.core.connMngr.ConnMngrComp;
import se.sics.gvod.core.downloadMngr.DownloadMngrComp;
import se.sics.gvod.core.downloadMngr.DownloadMngrConfig;
import se.sics.gvod.core.connMngr.ConnMngrConfig;
import se.sics.gvod.core.connMngr.ConnMngrPort;
import se.sics.gvod.core.downloadMngr.DownloadMngrPort;
import se.sics.gvod.core.msg.DownloadVideo;
import se.sics.gvod.core.msg.PlayReady;
import se.sics.gvod.core.msg.UploadVideo;
import se.sics.gvod.core.store.storageMngr.FileMngr;
import se.sics.gvod.core.store.storageMngr.HashMngr;
import se.sics.gvod.core.store.storageMngr.StorageMngrFactory;
import se.sics.gvod.timer.Timer;
import se.sics.gvod.videoplugin.VideoPlayerComp;
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

    Negative<UtilityUpdatePort> utilityUpdate = provides(UtilityUpdatePort.class);
    Positive<Timer> timer = requires(Timer.class);
    Positive<VodNetwork> network = requires(VodNetwork.class);
    Positive<BootstrapClientPort> bootstrap = requires(BootstrapClientPort.class);

    Negative<VoDPort> myPort = provides(VoDPort.class);

    private final VoDConfig config;

    private final Map<Integer, Quartet<Component, Component, Component, Component>> videoComps;

    private final Map<UUID, Pair<Pair<String, Integer>, FileMetadata>> pendingUploads;
    private final Map<UUID, Pair<String, Integer>> pendingDownloads;

    public VoDComp(VoDInit init) {
        this.config = init.config;
        log.info("{} lib folder: {}", config.selfAddress, config.libDir);
        this.videoComps = new HashMap<Integer, Quartet<Component, Component, Component, Component>>();
        this.pendingDownloads = new HashMap<UUID, Pair<String, Integer>>();
        this.pendingUploads = new HashMap<UUID, Pair<Pair<String, Integer>, FileMetadata>>();

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
            log.info("{} - {} - videoName:{} overlay:{}", new Object[]{config.selfAddress, req, req.videoName, req.overlayId});
            String videoNameNoExt = req.videoName.substring(0, req.videoName.indexOf("."));
            String videoFilePath = config.libDir + File.separator + req.videoName;
            String hashFilePath = config.libDir + File.separator + videoNameNoExt + ".hash";

            File videoFile = new File(videoFilePath);
            File hashFile = new File(hashFilePath);
            if (hashFile.exists()) {
                hashFile.delete();
            }
            try {
                hashFile.createNewFile();
                int blockSize = config.piecesPerBlock * config.pieceSize;
                HashUtil.makeHashes(videoFilePath, hashFilePath, config.hashAlg, blockSize);
            } catch (HashUtil.HashBuilderException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            FileMetadata fileMeta = new FileMetadata(req.videoName, (int) videoFile.length(), config.pieceSize, config.hashAlg, (int) hashFile.length());
            trigger(new AddOverlay.Request(req.id, req.overlayId, fileMeta), bootstrap);
            pendingUploads.put(req.id, Pair.with(Pair.with(req.videoName, req.overlayId), fileMeta));
        }
    };

    public Handler<DownloadVideo.Request> handleDownloadVideoRequest = new Handler<DownloadVideo.Request>() {

        @Override
        public void handle(DownloadVideo.Request req) {
            log.info("{} - {} - videoName:{} overlay:{}", new Object[]{config.selfAddress, req, req.videoName, req.overlayId});
            trigger(new JoinOverlay.Request(req.id, req.overlayId, noDownloadResume), bootstrap);
            pendingDownloads.put(req.id, Pair.with(req.videoName, req.overlayId));
        }
    };

    public Handler<AddOverlay.Response> handleAddOverlayResponse = new Handler<AddOverlay.Response>() {

        @Override
        public void handle(AddOverlay.Response resp) {
            log.trace("{} - {}", new Object[]{config.selfAddress, resp});

            if (resp.status == ReqStatus.SUCCESS) {
                try {
                    Pair<Pair<String, Integer>, FileMetadata> fileInfo = pendingUploads.remove(resp.id);
                    Pair<FileMngr, HashMngr> videoMngrs = getUploadVideoMngrs(fileInfo.getValue0().getValue0(), fileInfo.getValue1());
                    startVideoComp(resp.overlayId, fileInfo.getValue1(), videoMngrs, false);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (HashUtil.HashBuilderException ex) {
                    throw new RuntimeException(ex);
                } catch (GVoDConfigException.Missing ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                throw new RuntimeException();
            }
        }
    };

    public Handler<JoinOverlay.Response> handleJoinOverlayResponse = new Handler<JoinOverlay.Response>() {

        @Override
        public void handle(JoinOverlay.Response resp) {
            log.trace("{} - {}", new Object[]{config.selfAddress, resp});

            if (resp.status == ReqStatus.SUCCESS) {
                try {
                    Pair<String, Integer> fileInfo = pendingDownloads.remove(resp.id);
                    Pair<FileMngr, HashMngr> videoMngrs = getDownloadVideoMngrs(fileInfo.getValue0(), resp.fileMeta);
                    startVideoComp(resp.overlayId, resp.fileMeta, videoMngrs, true);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (HashUtil.HashBuilderException ex) {
                    throw new RuntimeException(ex);
                } catch (GVoDConfigException.Missing ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                throw new RuntimeException();
            }
        }
    };

    private void startVideoComp(int overlayId, FileMetadata fileMeta, Pair<FileMngr, HashMngr> hashedFileMngr, boolean download) {
        DownloadMngrConfig downloadMngrConfig;
        ConnMngrConfig connMngrConfig;

        try {
            downloadMngrConfig = config.getDownloadMngrConfig(overlayId).finalise();
            connMngrConfig = config.getConnMngrConfig(overlayId).finalise();
        } catch (GVoDConfigException.Missing ex) {
            throw new RuntimeException(ex);
        }

        Component connMngr = create(ConnMngrComp.class, new ConnMngrComp.ConnMngrInit(connMngrConfig));
        Component downloadMngr = create(DownloadMngrComp.class, new DownloadMngrComp.DownloadMngrInit(downloadMngrConfig, hashedFileMngr.getValue0(), hashedFileMngr.getValue1(), download));
        Component croupier = create(CroupierComp.class, new CroupierComp.CroupierInit(overlayId, config.selfAddress));
        Component playMngr = create(VideoPlayerComp.class, new VideoPlayerComp.VideoPlayerInit(new VideoPlayerComp.VideoPlayerConfig(fileMeta.fileName, overlayId, fileMeta.fileSize, 1000, 100 * 1024)));
        videoComps.put(overlayId, Quartet.with(connMngr, downloadMngr, croupier, playMngr));

        connect(croupier.getNegative(Timer.class), timer);
        connect(croupier.getNegative(BootstrapClientPort.class), bootstrap);

        connect(connMngr.getNegative(VodNetwork.class), network, new OverlayFilter(overlayId));
        connect(connMngr.getNegative(Timer.class), timer);
        connect(connMngr.getNegative(CroupierPort.class), croupier.getPositive(CroupierPort.class));

        connect(downloadMngr.getNegative(Timer.class), timer);
        connect(downloadMngr.getNegative(ConnMngrPort.class), connMngr.getPositive(ConnMngrPort.class));

        connect(connMngr.getNegative(UtilityUpdatePort.class), downloadMngr.getPositive(UtilityUpdatePort.class));
        connect(utilityUpdate, downloadMngr.getPositive(UtilityUpdatePort.class));
        
        connect(playMngr.getNegative(Timer.class), timer);
        connect(playMngr.getNegative(DownloadMngrPort.class), downloadMngr.getPositive(DownloadMngrPort.class));

        trigger(Start.event, croupier.control());
        trigger(Start.event, connMngr.control());
        trigger(Start.event, downloadMngr.control());
        trigger(Start.event, playMngr.control());
        
        trigger(new PlayReady(UUID.randomUUID(), (VideoPlayerComp)playMngr.getComponent()), myPort);
    }

    private Pair<FileMngr, HashMngr> getUploadVideoMngrs(String video, FileMetadata fileMeta) throws IOException, HashUtil.HashBuilderException, GVoDConfigException.Missing {

        String videoName = video.substring(0, video.indexOf("."));
        String videoExt = video.substring(video.indexOf("."));
        String videoFilePath = config.libDir + File.separator + video;
        String hashFilePath = config.libDir + File.separator + videoName + ".hash";

//        int nrHashPieces = fileMeta.hashFileSize / HashUtil.getHashSize(fileMeta.hashAlg);
//        PieceTracker hashPieceTracker = new CompletePieceTracker(nrHashPieces);
//        Storage hashStorage = StorageFactory.getExistingFile(hashFilePath);
//        HashMngr hashMngr = new CompleteFileMngr(hashStorage, hashPieceTracker);
        HashMngr hashMngr = StorageMngrFactory.getCompleteHashMngr(hashFilePath, fileMeta.hashAlg, fileMeta.hashFileSize, HashUtil.getHashSize(fileMeta.hashAlg));

//        int filePieces = fileMeta.fileSize / fileMeta.pieceSize + (fileMeta.fileSize % fileMeta.pieceSize == 0 ? 0 : 1);
//        Storage videoStorage = StorageFactory.getExistingFile(videoFilePath, config.pieceSize);
//        PieceTracker videoPieceTracker = new CompletePieceTracker(filePieces);
//        FileMngr fileMngr = new SimpleFileMngr(videoStorage, videoPieceTracker);
        int blockSize = config.piecesPerBlock * config.pieceSize;
        FileMngr fileMngr = StorageMngrFactory.getCompleteFileMngr(videoFilePath, fileMeta.fileSize, blockSize, config.pieceSize);

        return Pair.with(fileMngr, hashMngr);
    }

    private Pair<FileMngr, HashMngr> getDownloadVideoMngrs(String video, FileMetadata fileMeta) throws IOException, HashUtil.HashBuilderException, GVoDConfigException.Missing {
        log.info("{} lib directory {}", config.selfAddress, config.libDir);
        String videoName = video.substring(0, video.indexOf("."));
        String videoExt = video.substring(video.indexOf("."));
        String videoFilePath = config.libDir + File.separator + video;
        String hashFilePath = config.libDir + File.separator + videoName + ".hash";

        File hashFile = new File(hashFilePath);
        if (hashFile.exists()) {
            hashFile.delete();
        }

//        int hashPieces = fileMeta.hashFileSize / HashUtil.getHashSize(fileMeta.hashAlg);
//        PieceTracker hashPieceTracker = new SimplePieceTracker(hashPieces);
//        Storage hashStorage = StorageFactory.getEmptyFile(hashFilePath, fileMeta.hashFileSize, HashUtil.getHashSize(fileMeta.hashAlg));
//        FileMngr hashMngr = new SimpleFileMngr(hashStorage, hashPieceTracker);
        HashMngr hashMngr = StorageMngrFactory.getIncompleteHashMngr(hashFilePath, fileMeta.hashAlg, fileMeta.hashFileSize, HashUtil.getHashSize(fileMeta.hashAlg));

//        Storage videoStorage = StorageFactory.getEmptyFile(videoFilePath, fileMeta.fileSize, fileMeta.pieceSize);
//        int filePieces = fileMeta.fileSize / fileMeta.pieceSize + (fileMeta.fileSize % fileMeta.pieceSize == 0 ? 0 : 1);
//        PieceTracker videoPieceTracker = new SimplePieceTracker(filePieces);
//        FileMngr fileMngr = new SimpleFileMngr(videoStorage, videoPieceTracker);
        int blockSize = config.piecesPerBlock * config.pieceSize;
        FileMngr fileMngr = StorageMngrFactory.getIncompleteFileMngr(videoFilePath, fileMeta.fileSize, blockSize, config.pieceSize);

        return Pair.with(fileMngr, hashMngr);
    }
}
