
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
package se.sics.gvod.bootstrap.cclient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.operations.CaracalMsg;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.gvod.bootstrap.cclient.operations.AddFileMetadataOp;
import se.sics.gvod.bootstrap.cclient.operations.AddOverlayPeerOp;
import se.sics.gvod.bootstrap.cclient.operations.GetFileMetadataOp;
import se.sics.gvod.bootstrap.cclient.operations.GetOverlaySampleOp;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerMsg;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerPort;
import se.sics.gvod.bootstrap.server.peermanager.msg.AddFileMetadata;
import se.sics.gvod.bootstrap.server.peermanager.msg.GetFileMetadata;
import se.sics.gvod.bootstrap.server.peermanager.msg.GetOverlaySample;
import se.sics.gvod.bootstrap.server.peermanager.msg.JoinOverlay;
import se.sics.gvod.common.util.Operation;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BCClientComp extends ComponentDefinition implements CaracalOpManager {

    private static final Logger log = LoggerFactory.getLogger(BCClientComp.class);

    private Positive<Network> network = requires(Network.class);
    private Positive<PeerManagerPort> peerManager = requires(PeerManagerPort.class);

    private final BCClientConfig config;

    private final Map<UUID, Operation<CaracalOp>> pendingOps;
    private final Map<UUID, UUID> pendingCaracalOps;

    public BCClientComp(BCClientInit init) {
        log.debug("init");
        this.config = init.config;
        this.pendingOps = new HashMap<UUID, Operation<CaracalOp>>();
        this.pendingCaracalOps = new HashMap<UUID, UUID>();

        subscribe(handleCaracalResponse, network);
        subscribe(handleJoinOverlay, peerManager);
        subscribe(handleGetOverlaySample, peerManager);
        subscribe(handleAddFileMetadata, peerManager);
        subscribe(handleGetFileMetadata, peerManager);
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

    public Handler<JoinOverlay.Request> handleJoinOverlay = new Handler<JoinOverlay.Request>() {

        @Override
        public void handle(JoinOverlay.Request req) {
            log.debug("{} received {}", new Object[]{config.self, req});
            Operation op = new AddOverlayPeerOp(BCClientComp.this, req);
            pendingOps.put(req.id, op);
            op.start();
        }
    };

    public Handler<GetOverlaySample.Request> handleGetOverlaySample = new Handler<GetOverlaySample.Request>() {

        @Override
        public void handle(GetOverlaySample.Request req) {
            log.debug("{} received {}", new Object[]{config.self, req});
            Operation op = new GetOverlaySampleOp(BCClientComp.this, req, config.sampleSize);
            pendingOps.put(req.id, op);
            op.start();
        }
    };
    
    public Handler<AddFileMetadata.Request> handleAddFileMetadata = new Handler<AddFileMetadata.Request>() {

        @Override
        public void handle(AddFileMetadata.Request req) {
            log.debug("{} received {}", new Object[]{config.self, req});
            Operation op = new AddFileMetadataOp(BCClientComp.this, req);
            pendingOps.put(req.id, op);
            op.start();
        }
    };
    
    public Handler<GetFileMetadata.Request> handleGetFileMetadata = new Handler<GetFileMetadata.Request>() {

        @Override
        public void handle(GetFileMetadata.Request req) {
            log.debug("{} received {}", new Object[]{config.self, req});
            Operation op = new GetFileMetadataOp(BCClientComp.this, req);
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
    public void sendCaracalReq(UUID opId, CaracalOp req) {
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

    public static class BCClientInit extends Init<BCClientComp> {

        public final BCClientConfig config;

        public BCClientInit(BCClientConfig config) {
            this.config = config;
        }
    }
}
