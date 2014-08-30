
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
package se.sics.gvod.cserver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.operations.CaracalMsg;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.gvod.bootstrap.common.PeerManagerMsg;
import se.sics.gvod.bootstrap.common.PeerManagerPort;
import se.sics.gvod.bootstrap.common.msg.AddOverlay;
import se.sics.gvod.bootstrap.common.msg.AddOverlayPeer;
import se.sics.gvod.bootstrap.common.msg.GetOverlaySample;
import se.sics.gvod.cserver.operations.AddOverlayOp;
import se.sics.gvod.cserver.operations.AddOverlayPeerOp;
import se.sics.gvod.cserver.operations.GetOverlaySampleOp;
import se.sics.gvod.cserver.operations.Operation;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BCServerComp extends ComponentDefinition implements OperationManager {

    private static final Logger log = LoggerFactory.getLogger(BCServerComp.class);

    private Positive<Network> network = requires(Network.class);
    private Positive<PeerManagerPort> peerManager = requires(PeerManagerPort.class);

    private final BCServerConfig config;

    private final Map<UUID, Operation> pendingOps;
    private final Map<UUID, UUID> pendingCaracalOps;

    public BCServerComp(BCServerInit init) {
        log.debug("init");
        this.config = init.config;
        this.pendingOps = new HashMap<UUID, Operation>();
        this.pendingCaracalOps = new HashMap<UUID, UUID>();

        subscribe(handleCaracalResponse, network);
        subscribe(handleAddOverlay, peerManager);
        subscribe(handleAddOverlayPeer, peerManager);
        subscribe(handleGetOverlaySample, peerManager);
    }

    public Handler<CaracalMsg> handleCaracalResponse = new Handler<CaracalMsg>() {

        @Override
        public void handle(CaracalMsg resp) {
            log.debug("{} received {}", new Object[]{config.self, resp.op});
            UUID opId = pendingCaracalOps.remove(resp.op.id);
            if (opId == null) {
                log.debug("{} dropping {}", new Object[]{config.self, resp.op});
            }
            pendingOps.get(opId).handle(resp.op);
        }
    };

    public Handler<AddOverlay.Request> handleAddOverlay = new Handler<AddOverlay.Request>() {

        @Override
        public void handle(AddOverlay.Request req) {
            log.debug("{} received {}", new Object[]{config.self, req});
            Operation op = new AddOverlayOp(BCServerComp.this, req);
            pendingOps.put(req.id, op);
            op.start();
        }
    };
    
    public Handler<AddOverlayPeer.Request> handleAddOverlayPeer = new Handler<AddOverlayPeer.Request>() {

        @Override
        public void handle(AddOverlayPeer.Request req) {
            log.debug("{} received {}", new Object[]{config.self, req});
            Operation op = new AddOverlayPeerOp(BCServerComp.this, req);
            pendingOps.put(req.id, op);
            op.start();
        }
    };
    
    public Handler<GetOverlaySample.Request> handleGetOverlaySample = new Handler<GetOverlaySample.Request>() {

        @Override
        public void handle(GetOverlaySample.Request req) {
            log.debug("{} received {}", new Object[]{config.self, req});
            Operation op = new GetOverlaySampleOp(BCServerComp.this, req, config.sampleSize);
            pendingOps.put(req.id, op);
            op.start();
        }
    };

    //**********OperationManager
    @Override
    public void finish(PeerManagerMsg.Response resp) {
        if (pendingOps.remove(resp.id) == null) {
            throw new RuntimeException("pendingOp should not be null");
        }
        cleanRequests(resp.id);
        log.debug("{} sending {}", new Object[]{config.self, resp});
        trigger(resp, peerManager);
    }

    @Override
    public void sendCaracalOp(UUID opId, CaracalOp req) {
        log.debug("{} sending {}", new Object[]{config.self, req});
        pendingCaracalOps.put(opId, req.id);
        trigger(req, network);
    }

    //***************************
    public void cleanRequests(UUID opId) {
        Iterator<Map.Entry<UUID, UUID>> it = pendingCaracalOps.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().equals(opId)) {
                it.remove();
            }
        }
    }
}
