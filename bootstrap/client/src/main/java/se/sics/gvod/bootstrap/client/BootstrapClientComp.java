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
package se.sics.gvod.bootstrap.client;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.client.msg.AddOverlayMsg;
import se.sics.gvod.bootstrap.common.msg.BootstrapMsg;
import se.sics.gvod.bootstrap.client.msg.JoinOverlayMsg;
import se.sics.gvod.common.msg.GvodNetMsg;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.util.MsgProcessor;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapClientComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(BootstrapClientComp.class);

    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Negative<BootstrapClientPort> myPort = provides(BootstrapClientPort.class);
   
    private MsgProcessor msgProc;
    private BootstrapClientConfig config;
    
    private final Set<Integer> overlayIds;
    
    public BootstrapClientComp(BootstrapClientInit init) {
        log.debug("init");
        this.config = init.config;
        this.msgProc = new MsgProcessor();
        this.overlayIds = new HashSet<>();

        subscribe(handleBootstrapRequest, myPort);
        subscribe(handleJoinOverlayRequest, myPort);
        subscribe(handleAddOverlayRequest, myPort);
        subscribe(handleGvodNetResponse, network);
        msgProc.subscribe(handleBootstrapResponse);
    }

    public Handler<BootstrapMsg.Request> handleBootstrapRequest = new Handler<BootstrapMsg.Request>() {

        @Override
        public void handle(BootstrapMsg.Request req) {
            log.trace("received {}", req.toString());
            GvodNetMsg.Request netReq = new GvodNetMsg.Request (
                    new VodAddress(config.self, -1),
                    new VodAddress(config.server, -1),
                    req);
            log.debug("sending {}", netReq.toString());
            trigger(netReq, network);
        }
    };
    
    public Handler<AddOverlayMsg.Request> handleAddOverlayRequest = new Handler<AddOverlayMsg.Request>() {

        @Override
        public void handle(AddOverlayMsg.Request req) {
            log.trace("received {}", req.toString());
            overlayIds.add(req.overlayId);
            GvodNetMsg.Request netReq = new GvodNetMsg.Request(
                    new VodAddress(config.self, -1),
                    new VodAddress(config.server, -1),
                    req);
            log.debug("sending {}", netReq.toString());
            trigger(netReq, network);
        }

    };

    public Handler<JoinOverlayMsg.Request> handleJoinOverlayRequest = new Handler<JoinOverlayMsg.Request>() {

        @Override
        public void handle(JoinOverlayMsg.Request req) {
            log.trace("received {}", req.toString());
            overlayIds.addAll(req.overlayId);
            GvodNetMsg.Request netReq = new GvodNetMsg.Request(
                    new VodAddress(config.self, -1),
                    new VodAddress(config.server, -1),
                    req);
            log.debug("sending {}", netReq.toString());
            trigger(netReq, network);
        }

    };

    public Handler<GvodNetMsg.Response> handleGvodNetResponse = new Handler<GvodNetMsg.Response>() {

        @Override
        public void handle(GvodNetMsg.Response netResp) {
            log.debug("received {} ", netResp.toString());
            msgProc.process(netResp.payload);
        }
    };
    
    public Handler<BootstrapMsg.Response> handleBootstrapResponse = new Handler<BootstrapMsg.Response>(BootstrapMsg.Response.class) {

        @Override
        public void handle(BootstrapMsg.Response resp) {
            if(resp.status == ReqStatus.SUCCESS) {
                log.info("bootstraped");
                trigger(resp, myPort);
            }
        }
    };
}