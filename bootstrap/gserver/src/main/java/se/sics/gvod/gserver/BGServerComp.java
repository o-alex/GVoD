
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
package se.sics.gvod.gserver;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.common.PeerManagerPort;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.network.nettymsg.GvodNetMsg;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BGServerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(BGServerComp.class);

    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<PeerManagerPort> peerManager = requires(PeerManagerPort.class);

    private final BGServerConfig config;

    private final Map<UUID, VodAddress> pendingReq;

    public BGServerComp(BGServerInit init) {
        log.debug("init");
        this.config = init.config;
        this.pendingReq = new HashMap<UUID, VodAddress>();

        subscribe(handleNetRequest, network);
        subscribe(handlePeerManagerResponse, peerManager);
    }

    public Handler<GvodNetMsg.Request<GvodMsg.Request>> handleNetRequest = new Handler<GvodNetMsg.Request<GvodMsg.Request>>() {

        @Override
        public void handle(GvodNetMsg.Request<GvodMsg.Request> netReq) {
            log.debug("{} received {}", new Object[]{config.self, netReq});
            pendingReq.put(netReq.payload.reqId, netReq.getVodSource());
            trigger(netReq.payload, peerManager);
        }
    };
    
    public Handler<GvodMsg.Response> handlePeerManagerResponse = new Handler<GvodMsg.Response>() {

        @Override
        public void handle(GvodMsg.Response resp) {
            log.debug("{} received {}", new Object[]{config.self, resp});
            VodAddress src = pendingReq.remove(resp.reqId);
            if(src == null) {
                log.debug("{} dropping {}", new Object[]{config.self, resp});
                return;
            }
            trigger(new GvodNetMsg.Response<GvodMsg.Response>(config.self, src, resp), network);
        }
    };
}
