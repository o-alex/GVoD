
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
import java.util.UUID;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.common.msg.impl.AddOverlayMsg;
import se.sics.gvod.common.msg.impl.BootstrapGlobalMsg;
import se.sics.gvod.bootstrap.server.peerManager.PeerManager;
import se.sics.gvod.bootstrap.server.peerManager.SimplePeerManager;
import se.sics.gvod.common.msg.GvodNetMsg;
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

    private final PeerManager peerManager;

    public BootstrapServerComp(BootstrapServerInit init) {
        log.debug("init");
        this.config = init.config;
        this.peerManager = new SimplePeerManager(config.getVodPeerManagerConfig());

        subscribe(handleBootstrapRequest, network);
        subscribe(handleAddOverlayRequest, network);
    }

    public Handler<GvodNetMsg.Request<BootstrapGlobalMsg.Request>> handleBootstrapRequest = new Handler<GvodNetMsg.Request<BootstrapGlobalMsg.Request>>() {

        @Override
        public void handle(final GvodNetMsg.Request<BootstrapGlobalMsg.Request> netReq) {
            log.debug("received {}", netReq.toString());
            BootstrapGlobalMsg.Response resp = netReq.payload.success(peerManager.getSystemSample());
            peerManager.addVodPeer(netReq.getVodSource());

            GvodNetMsg.Response netResp = netReq.getResponse(resp);
            log.debug("sending {}", netResp.toString());
            trigger(netResp, network);
        }
    };

    public Handler<GvodNetMsg.Request<AddOverlayMsg.Request>> handleAddOverlayRequest = new Handler<GvodNetMsg.Request<AddOverlayMsg.Request>>() {

        @Override
        public void handle(GvodNetMsg.Request<AddOverlayMsg.Request> netReq) {
            log.debug("received {}", netReq.toString());

            AddOverlayMsg.Response resp;
            try {
                peerManager.addOverlay(netReq.payload.overlayId, netReq.getVodSource());
                resp = netReq.payload.success();
            } catch (PeerManager.PMException ex) {
                resp = netReq.payload.fail();
            }

            GvodNetMsg.Response netResp = netReq.getResponse(resp);
            log.debug("sending {}", netResp.toString());
            trigger(netResp, network);
        }
    };

}
