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

import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.client.BootstrapClientPort;
import se.sics.gvod.common.msg.impl.AddOverlayMsg;
import se.sics.gvod.common.msg.impl.BootstrapGlobalMsg;
import se.sics.gvod.common.msg.impl.JoinOverlayMsg;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.system.vod.msg.DownloadVideo;
import se.sics.gvod.system.vod.msg.UploadVideo;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VoDComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(VoDComp.class);

    Positive<VodNetwork> network = requires(VodNetwork.class);
    Positive<BootstrapClientPort> bootstrap = requires(BootstrapClientPort.class);
    Negative<VoDPort> myPort = provides(VoDPort.class);

    private final VoDConfiguration config;

    public VoDComp(VoDInit init) {
        log.debug("init");
        this.config = init.config;

        subscribe(handleStart, control);
        subscribe(handleBootstrapGlobalResponse, bootstrap);
        subscribe(handleUploadVideoRequest, myPort);
        subscribe(handleDownloadVideoRequest, myPort);
        subscribe(handleAddOverlayResponse, bootstrap);
        subscribe(handleJoinOverlayResponse, bootstrap);
    }

    public Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            
        }
    };
    
    public Handler<BootstrapGlobalMsg.Response> handleBootstrapGlobalResponse = new Handler<BootstrapGlobalMsg.Response>() {
        @Override
        public void handle(BootstrapGlobalMsg.Response event) {
        }
    };

    public Handler<UploadVideo.Request> handleUploadVideoRequest = new Handler<UploadVideo.Request>() {

        @Override
        public void handle(UploadVideo.Request req) {
            log.debug("{} - {} - overlay: {}", new Object[]{config.self.toString(), req.toString(), req.overlayId});
            trigger(new AddOverlayMsg.Request(req.reqId, req.overlayId), bootstrap);
        }
    };
    
    public Handler<DownloadVideo.Request> handleDownloadVideoRequest = new Handler<DownloadVideo.Request>() {

        @Override
        public void handle(DownloadVideo.Request req) {
            log.debug("{} - {} - overlay: {}", new Object[]{config.self.toString(), req.toString(), req.overlayId});
            HashSet<Integer> overlayIds = new HashSet<>();
            overlayIds.add(req.overlayId);
            trigger(new JoinOverlayMsg.Request(req.reqId, overlayIds), bootstrap);
        }
    };
    
    public Handler<AddOverlayMsg.Response> handleAddOverlayResponse = new Handler<AddOverlayMsg.Response>() {

        @Override
        public void handle(AddOverlayMsg.Response resp) {
            log.trace("{} - {}", new Object[]{config.self.toString(), resp.toString()});
        }
    };
    public Handler<JoinOverlayMsg.Response> handleJoinOverlayResponse = new Handler<JoinOverlayMsg.Response>() {

        @Override
        public void handle(JoinOverlayMsg.Response resp) {
            log.trace("{} - {} - peers:{}", new Object[]{config.self.toString(), resp.toString(), resp.overlaySample.toString()});
        }
    };
}
