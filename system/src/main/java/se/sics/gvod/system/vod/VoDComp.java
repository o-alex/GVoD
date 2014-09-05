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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.client.BootstrapClientPort;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.AddOverlay;
import se.sics.gvod.common.msg.impl.BootstrapGlobal;
import se.sics.gvod.common.msg.impl.JoinOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.manager.DownloadFileInfo;
import se.sics.gvod.manager.UploadFileInfo;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.system.video.VideoComp;
import se.sics.gvod.system.video.VideoConfig;
import se.sics.gvod.system.video.connMngr.ConnMngr;
import se.sics.gvod.system.video.connMngr.ConnMngrConfig;
import se.sics.gvod.system.video.connMngr.SimpleConnMngr;
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

    private final Map<UUID, UploadFileInfo> pendingUploads;
    private final Map<UUID, DownloadFileInfo> pendingDownloads;

    public VoDComp(VoDInit init) {
        log.debug("init");
        this.config = init.config;
        this.videoComps = new HashMap<Integer, Component>();
        this.pendingDownloads = new HashMap<UUID, DownloadFileInfo>();
        this.pendingUploads = new HashMap<UUID, UploadFileInfo>();

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
            File f = new File(req.fileInfo.libDir + File.pathSeparator + req.fileInfo.fileName);
            trigger(new AddOverlay.Request(req.id, req.fileInfo.overlayId, new FileMetadata((int) f.length(), config.pieceSize)), bootstrap);
            pendingUploads.put(req.id, req.fileInfo);
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
                    UploadFileInfo fileInfo = pendingUploads.remove(resp.id);
                    Storage videoStorage = StorageFactory.getExistingFile(fileInfo.libDir + File.pathSeparator + fileInfo.fileName, config.pieceSize);
                    startVideoComp(fileInfo.overlayId, videoStorage, new HashMap<VodAddress, Integer>());
                } catch (IOException ex) {
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
                    Storage videoStorage = StorageFactory.getEmptyFile(fileInfo.libDir + File.pathSeparator + fileInfo.fileName, resp.fileMeta.size, resp.fileMeta.pieceSize);
                    startVideoComp(fileInfo.overlayId, videoStorage, resp.overlaySample);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    };

    private void startVideoComp(int overlayId, Storage videoStorage, Map<VodAddress, Integer> overlaySample) {
        VideoConfig videoConfig;
        ConnMngrConfig connMngrConfig;

        try {
            videoConfig = config.getVideoConfig().setDownloader(false).finalise();
            connMngrConfig = config.getConnMngrConfig().finalise();
        } catch (GVoDConfigException.Missing ex) {
            throw new RuntimeException(ex);
        }

        ConnMngr connMngr = new SimpleConnMngr(connMngrConfig);
        Component video = create(VideoComp.class, new VideoComp.VideoInit(videoConfig, connMngr, videoStorage, overlaySample));
        videoComps.put(overlayId, video);

        connect(video.getNegative(Timer.class), timer);
        connect(video.getNegative(VodNetwork.class), network);

        trigger(Start.event, video.control());
    }
}
