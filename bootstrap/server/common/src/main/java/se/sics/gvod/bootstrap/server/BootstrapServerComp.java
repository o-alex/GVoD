
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
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.server.operations.AddOverlayOp;
import se.sics.gvod.bootstrap.server.operations.BootstrapGlobalOp;
import se.sics.gvod.bootstrap.server.operations.JoinOverlayOp;
import se.sics.gvod.bootstrap.server.operations.Operation;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerMsg;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerPort;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.impl.AddOverlayMsg;
import se.sics.gvod.common.msg.impl.BootstrapGlobalMsg;
import se.sics.gvod.common.msg.impl.JoinOverlayMsg;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.network.nettymsg.GvodNetMsg;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapServerComp extends ComponentDefinition implements PeerOpManager {

    private static final Logger log = LoggerFactory.getLogger(BootstrapServerComp.class);

    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<PeerManagerPort> peerManager = requires(PeerManagerPort.class);

    private final BootstrapServerConfig config;

    private final Map<UUID, Operation> pendingOps;
    private final Map<UUID, UUID> pendingPeerReqs;

    public BootstrapServerComp(BootstrapServerInit init) {
        log.debug("init");
        this.config = init.config;
        this.pendingOps = new HashMap<UUID, Operation>();
        this.pendingPeerReqs = new HashMap<UUID, UUID>();

        subscribe(handleNetRequest, network);
        subscribe(handlePeerManagerResponse, peerManager);
    }

    public Handler<GvodNetMsg.Request<GvodMsg.Request>> handleNetRequest = new Handler<GvodNetMsg.Request<GvodMsg.Request>>() {

        @Override
        public void handle(GvodNetMsg.Request<GvodMsg.Request> netReq) {
            log.debug("{} received {}", new Object[]{config.self, netReq});
            Operation op = null;
            if(netReq.payload instanceof BootstrapGlobalMsg.Request) {
                BootstrapGlobalMsg.Request gvodReq = (BootstrapGlobalMsg.Request)netReq.payload;
                op = new BootstrapGlobalOp(BootstrapServerComp.this, gvodReq, netReq.getVodSource());
            } else if(netReq.payload instanceof AddOverlayMsg.Request) {
                AddOverlayMsg.Request gvodReq = (AddOverlayMsg.Request)netReq.payload;
                op = new AddOverlayOp(BootstrapServerComp.this, gvodReq, netReq.getVodSource());
            } else if(netReq.payload instanceof JoinOverlayMsg.Request) {
                JoinOverlayMsg.Request gvodReq = (JoinOverlayMsg.Request)netReq.payload;
                op = new JoinOverlayOp(BootstrapServerComp.this, gvodReq, netReq.getVodSource());
            } else {
                throw new RuntimeException("wrong message");
            }
            pendingOps.put(netReq.payload.reqId, op);
            op.start();
        }
    };
    
    public Handler<PeerManagerMsg.Response> handlePeerManagerResponse = new Handler<PeerManagerMsg.Response>() {

        @Override
        public void handle(PeerManagerMsg.Response resp) {
            log.debug("{} received {}", new Object[]{config.self, resp});
            Operation op = pendingOps.get(pendingPeerReqs.remove(resp.id));
            if (op == null) {
                log.debug("{} dropping {}", new Object[]{config.self, resp});
                return;
            }
            op.handle(resp);
        }
    };

    //**********OperationManager
    @Override
    public void finish(VodAddress src, GvodMsg.Response resp) {
        if (pendingOps.remove(resp.reqId) == null) {
            throw new RuntimeException("pendingOp should not be null");
        }
        cleanRequests(resp.reqId);
        log.debug("{} sending {}", new Object[]{config.self, resp});
        trigger(new GvodNetMsg.Response<GvodMsg.Response>(config.self, src, resp), network);
    }

    @Override
    public void sendPeerManagerReq(UUID opId, PeerManagerMsg.Request req) {
        log.debug("{} sending {}", new Object[]{config.self, req});
        pendingPeerReqs.put(req.id, opId);
        trigger(req, peerManager);
    }

    //***************************
    public void cleanRequests(UUID opId) {
        Iterator<Map.Entry<UUID, UUID>> it = pendingPeerReqs.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().equals(opId)) {
                it.remove();
            }
        }
    }
    
    public static class BootstrapServerInit extends Init<BootstrapServerComp> {

        public final BootstrapServerConfig config;

        public BootstrapServerInit(BootstrapServerConfig config) {
            this.config = config;
        }
    }
}
