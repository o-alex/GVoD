
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
package se.sics.gvod.bootstrap.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.server.peerManager.PeerManager;
import se.sics.gvod.bootstrap.server.peerManager.SimplePeerManager;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.network.nettymsg.GvodNetMsg;
import se.sics.gvod.common.msg.impl.AddOverlayMsg;
import se.sics.gvod.common.msg.impl.BootstrapGlobalMsg;
import se.sics.gvod.common.msg.impl.JoinOverlayMsg;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.common.util.MsgProcessor;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapServerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(BootstrapServerComp.class);

    private Positive<VodNetwork> network = requires(VodNetwork.class);

    private final HostConfiguration config;

    private final MsgProcessor msgProc;

    private final PeerManager peerManager;

    public BootstrapServerComp(BootstrapServerInit init) {
        log.debug("init");
        this.config = init.config;
        this.msgProc = new MsgProcessor();
        try {
            this.peerManager = new SimplePeerManager(config.getVodPeerManagerConfig().finalise());
        } catch (GVoDConfigException.Missing ex) {
            throw new RuntimeException(ex);
        }

        subscribe(handleNetRequest, network);
        msgProc.subscribe(handleBootstrapRequest);
        msgProc.subscribe(handleAddOverlayRequest);
        msgProc.subscribe(handleJoinOverlayRequest);
    }

    public Handler<GvodNetMsg.Request<GvodMsg.Request>> handleNetRequest = new Handler<GvodNetMsg.Request<GvodMsg.Request>>() {

        @Override
        public void handle(GvodNetMsg.Request<GvodMsg.Request> netReq) {
            log.debug("{} received {}", new Object[]{config.self.toString(), netReq.toString()});
            msgProc.trigger(netReq.getVodSource(), netReq.payload);
        }
    };

    public MsgProcessor.Handler<BootstrapGlobalMsg.Request> handleBootstrapRequest = new MsgProcessor.Handler<BootstrapGlobalMsg.Request>(BootstrapGlobalMsg.Request.class) {

        @Override
        public void handle(VodAddress src, BootstrapGlobalMsg.Request req) {
            BootstrapGlobalMsg.Response resp = req.success(peerManager.getSystemSample());
            peerManager.addVodPeer(src);

            GvodNetMsg.Response<BootstrapGlobalMsg.Response> netResp = new GvodNetMsg.Response<BootstrapGlobalMsg.Response>(config.self, src, resp);
            log.debug("{} sending {}", new Object[]{config.self.toString(), netResp.toString()});
            trigger(netResp, network);
        }
    };

    public MsgProcessor.Handler<AddOverlayMsg.Request> handleAddOverlayRequest = new MsgProcessor.Handler<AddOverlayMsg.Request>(AddOverlayMsg.Request.class) {

        @Override
        public void handle(VodAddress src, AddOverlayMsg.Request req) {
            AddOverlayMsg.Response resp;
            try {
                peerManager.addOverlay(req.overlayId);
                peerManager.addOverlayPeer(req.overlayId, src);
                resp = req.success();
            } catch (PeerManager.PMException ex) {
                resp = req.fail();
            }

            GvodNetMsg.Response<AddOverlayMsg.Response> netResp = new GvodNetMsg.Response<AddOverlayMsg.Response>(config.self, src, resp);
            log.debug("{} sending {}", new Object[]{config.self.toString(), netResp.toString()});
            trigger(netResp, network);
        }
    };

    public MsgProcessor.Handler<JoinOverlayMsg.Request> handleJoinOverlayRequest = new MsgProcessor.Handler<JoinOverlayMsg.Request>(JoinOverlayMsg.Request.class) {

        @Override
        public void handle(VodAddress src, JoinOverlayMsg.Request req) {
            JoinOverlayMsg.Response resp;
            try {
                Map<Integer, Set<VodAddress>> overlaySamples = new HashMap<Integer, Set<VodAddress>>();
                for (Integer overlayId : req.overlayIds) {
                    overlaySamples.put(overlayId, peerManager.getOverlaySample(overlayId));
                    peerManager.addOverlayPeer(overlayId, src);
                }
                resp = req.success(overlaySamples);
            } catch (PeerManager.PMException ex) {
                resp = req.fail();
            }

            GvodNetMsg.Response<JoinOverlayMsg.Response> netResp = new GvodNetMsg.Response<JoinOverlayMsg.Response>(config.self, src, resp);
            log.debug("{} sending {}", new Object[]{config.self.toString(), netResp.toString()});
            trigger(netResp, network);
        }
    };
}
