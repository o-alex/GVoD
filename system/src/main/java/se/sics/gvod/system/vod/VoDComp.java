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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.client.BootstrapClientPort;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.impl.AddOverlayMsg;
import se.sics.gvod.common.msg.impl.BootstrapGlobalMsg;
import se.sics.gvod.common.msg.impl.JoinOverlayMsg;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.system.storage.Storage;
import se.sics.gvod.system.storage.StorageFactory;
import se.sics.gvod.system.video.VideoComp;
import se.sics.gvod.system.video.VideoConfig;
import se.sics.gvod.system.video.VideoFileMeta;
import se.sics.gvod.system.video.connMngr.ConnMngr;
import se.sics.gvod.system.video.connMngr.SimpleConnMngr;
import se.sics.gvod.system.vod.msg.DownloadVideo;
import se.sics.gvod.system.vod.msg.UploadVideo;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.timer.Timer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VoDComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(VoDComp.class);

    Positive<Timer> timer = requires(Timer.class);
    Positive<VodNetwork> network = requires(VodNetwork.class);
    Positive<BootstrapClientPort> bootstrap = requires(BootstrapClientPort.class);
    Negative<VoDPort> myPort = provides(VoDPort.class);

    private final VoDConfig config;

    private final Map<Integer, Component> videoComps;

    public VoDComp(VoDInit init) {
        log.debug("init");
        this.config = init.config;
        this.videoComps = new HashMap<Integer, Component>();

        subscribe(handleBootstrapGlobalResponse, bootstrap);
        subscribe(handleUploadVideoRequest, myPort);
        subscribe(handleDownloadVideoRequest, myPort);
        subscribe(handleAddOverlayResponse, bootstrap);
        subscribe(handleJoinOverlayResponse, bootstrap);
    }

    public Handler<BootstrapGlobalMsg.Response> handleBootstrapGlobalResponse = new Handler<BootstrapGlobalMsg.Response>() {
        @Override
        public void handle(BootstrapGlobalMsg.Response event) {
        }
    };

    public Handler<UploadVideo.Request> handleUploadVideoRequest = new Handler<UploadVideo.Request>() {

        @Override
        public void handle(UploadVideo.Request req) {
            log.debug("{} - {} - overlay: {}", new Object[]{config.selfAddress.toString(), req.toString(), req.fileInfo.overlayId});
            trigger(new AddOverlayMsg.Request(req.reqId, req.overlayId), bootstrap);

            startVideoComp(req.overlayId, req.fileInfo, false);
        }
    };

    public Handler<DownloadVideo.Request> handleDownloadVideoRequest = new Handler<DownloadVideo.Request>() {

        @Override
        public void handle(DownloadVideo.Request req) {
            log.debug("{} - {} - overlay: {}", new Object[]{config.selfAddress.toString(), req.toString(), req.overlayId});
            HashSet<Integer> overlayIds = new HashSet<Integer>();
            overlayIds.add(req.overlayId);
            trigger(new JoinOverlayMsg.Request(req.reqId, overlayIds), bootstrap);
        }
    };

    public Handler<AddOverlayMsg.Response> handleAddOverlayResponse = new Handler<AddOverlayMsg.Response>() {

        @Override
        public void handle(AddOverlayMsg.Response resp) {
            log.trace("{} - {}", new Object[]{config.selfAddress.toString(), resp.toString()});
        }
    };

    public Handler<JoinOverlayMsg.Response> handleJoinOverlayResponse = new Handler<JoinOverlayMsg.Response>() {

        @Override
        public void handle(JoinOverlayMsg.Response resp) {
            log.trace("{} - {} - peers:{}", new Object[]{config.selfAddress.toString(), resp.toString(), resp.overlaySamples.toString()});
        }
    };

    private void startVideoComp(int overlayId, VideoFileMeta vfMeta, boolean downloader) {

        Component video;
        
        try {
            Storage videoStorage;
            if (downloader) {
                videoStorage = StorageFactory.getExistingFile(vfMeta.filePath);
            } else {
                videoStorage = StorageFactory.getEmptyFile(vfMeta.filePath, vfMeta.size);
            }
            ConnMngr connMngr = new SimpleConnMngr(config.getConnMngrConfig().finalise());
            VideoConfig videoConfig = config.getVideoConfig().setDownloader(false).finalise();
            video = create(VideoComp.class, new VideoComp.VideoInit(videoConfig, connMngr, videoStorage));
            videoComps.put(overlayId, video);
        } catch (GVoDConfigException.Missing ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        connect(video.getNegative(Timer.class), timer);
        connect(video.getNegative(VodNetwork.class), network);

        trigger(Start.event, video.control());
    }
}
