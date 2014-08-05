
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.common.msg.BootstrapMsg;
import se.sics.gvod.bootstrap.server.peerManager.PeerManager;
import se.sics.gvod.bootstrap.server.peerManager.SimplePeerManager;
import se.sics.gvod.common.msg.GvodNetMsg;
import se.sics.gvod.common.msg.ReqStatus;
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

    private final PeerManager peerManager;
    private final MsgProcessor msgProc;
    private GvodNetMsg.Request currentReq;

    public BootstrapServerComp(BootstrapServerInit init) {
        log.debug("init");
        this.config = init.config;
        this.msgProc = new MsgProcessor();
        this.peerManager = new SimplePeerManager(config.getVodPeerManagerConfig());

        subscribe(handleGvodNetRequest, network);
        msgProc.subscribe(handleBootstrapRequest);
    }

    public Handler<GvodNetMsg.Request> handleGvodNetRequest = new Handler<GvodNetMsg.Request>() {

        @Override
        public void handle(final GvodNetMsg.Request netReq) {
            log.debug("received {}", netReq.toString());
            currentReq = netReq;
            msgProc.process(netReq.payload);
        }
    };

    Handler<BootstrapMsg.Request> handleBootstrapRequest = new Handler<BootstrapMsg.Request>(BootstrapMsg.Request.class) {
        @Override
        public void handle(BootstrapMsg.Request req) {
            BootstrapMsg.Response resp = req.getResponse(peerManager.getSystemSample());
            GvodNetMsg.Response netResp = currentReq.getResponse(resp);
            peerManager.addVodPeer(currentReq.getVodSource());

            log.debug("sending {}", netResp.toString());
            trigger(netResp, network);
        }
    };
}