
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
import se.sics.gvod.bootstrap.server.operations.HeartbeatOp;
import se.sics.gvod.bootstrap.server.operations.JoinOverlayOp;
import se.sics.gvod.bootstrap.server.operations.Operation;
import se.sics.gvod.bootstrap.server.operations.OverlaySampleOp;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerMsg;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerPort;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.peerMngr.AddOverlay;
import se.sics.gvod.common.msg.peerMngr.BootstrapGlobal;
import se.sics.gvod.common.msg.peerMngr.Heartbeat;
import se.sics.gvod.common.msg.peerMngr.JoinOverlay;
import se.sics.gvod.common.msg.peerMngr.OverlaySample;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.network.GVoDNetFrameDecoder;
import se.sics.gvod.network.GVoDNetworkSettings;
import se.sics.gvod.network.netmsg.NetContentMsg;
import se.sics.gvod.network.netmsg.bootstrap.NetAddOverlay;
import se.sics.gvod.network.netmsg.bootstrap.NetBootstrapGlobal;
import se.sics.gvod.network.netmsg.bootstrap.NetHeartbeat;
import se.sics.gvod.network.netmsg.bootstrap.NetJoinOverlay;
import se.sics.gvod.network.netmsg.bootstrap.NetOverlaySample;
import se.sics.gvod.network.serializers.SerializationContext;
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
    private final SerializationContext context;

    private final Map<UUID, Operation> pendingOps;
    private final Map<UUID, UUID> pendingPeerReqs;

    public BootstrapServerComp(BootstrapServerInit init) {
        log.debug("init");
        this.config = init.config;
        this.context = init.context;
        this.pendingOps = new HashMap<UUID, Operation>();
        this.pendingPeerReqs = new HashMap<UUID, UUID>();

        subscribe(handleBootstrapGlobalRequest, network);
        subscribe(handleAddOverlayRequest, network);
        subscribe(handleJoinOverlayRequest, network);
        subscribe(handleOverlaySampleRequest, network);
        subscribe(handleHeartbeat, network);
        subscribe(handlePeerManagerResponse, peerManager);
    }

    public Handler<NetBootstrapGlobal.Request> handleBootstrapGlobalRequest = new Handler<NetBootstrapGlobal.Request>() {

        @Override
        public void handle(NetBootstrapGlobal.Request req) {
            log.debug("{} received {}", config.self, req);
            Operation op = new BootstrapGlobalOp(BootstrapServerComp.this, context, req.content, req.getVodSource());
            pendingOps.put(req.content.id, op);
            op.start();
        }
    };

    public Handler<NetAddOverlay.Request> handleAddOverlayRequest = new Handler<NetAddOverlay.Request>() {

        @Override
        public void handle(NetAddOverlay.Request req) {
            log.debug("{} received{}", config.self, req);
            Operation op = new AddOverlayOp(BootstrapServerComp.this, context, req.content, req.getVodSource());
            pendingOps.put(req.content.id, op);
            op.start();
        }
    };

    public Handler<NetJoinOverlay.Request> handleJoinOverlayRequest = new Handler<NetJoinOverlay.Request>() {

        @Override
        public void handle(NetJoinOverlay.Request req) {
            log.debug("{} received {}", config.self, req);
            Operation op = new JoinOverlayOp(BootstrapServerComp.this, context, req.content, req.getVodSource());
            pendingOps.put(req.content.id, op);
            op.start();
        }
    };

    public Handler<NetOverlaySample.Request> handleOverlaySampleRequest = new Handler<NetOverlaySample.Request>() {

        @Override
        public void handle(NetOverlaySample.Request req) {
            log.debug("{} received {}", config.self, req);
            Operation op = new OverlaySampleOp(BootstrapServerComp.this, context, req.content, req.getVodSource());
            pendingOps.put(req.content.id, op);
            op.start();
        }
    };

    public Handler<NetHeartbeat.OneWay> handleHeartbeat = new Handler<NetHeartbeat.OneWay>() {

        @Override
        public void handle(NetHeartbeat.OneWay heartbeat) {
            log.debug("{} received {}", new Object[]{config.self, heartbeat});
            Operation op = new HeartbeatOp(BootstrapServerComp.this, context, heartbeat.content, heartbeat.getVodSource());
            pendingOps.put(heartbeat.content.id, op);
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
    public void finish(UUID opId, VodAddress src, GvodMsg.Response resp) {
        if (pendingOps.remove(opId) == null) {
            log.error("logic error handling operations");
            System.exit(1);
        }
        cleanRequests(opId);
        log.debug("{} sending {}", new Object[]{config.self, resp});
        if (resp instanceof BootstrapGlobal.Response) {
            BootstrapGlobal.Response gvodResp = (BootstrapGlobal.Response) resp;
            trigger(new NetBootstrapGlobal.Response(config.self, src, resp.id, gvodResp), network);
        } else if (resp instanceof AddOverlay.Response) {
            AddOverlay.Response gvodResp = (AddOverlay.Response) resp;
            trigger(new NetAddOverlay.Response(config.self, src, resp.id, gvodResp), network);
        } else if (resp instanceof JoinOverlay.Response) {
            JoinOverlay.Response gvodResp = (JoinOverlay.Response) resp;
            trigger(new NetJoinOverlay.Response(config.self, src, resp.id, gvodResp), network);
        } else if (resp instanceof OverlaySample.Response) {
            OverlaySample.Response gvodResp = (OverlaySample.Response) resp;
            trigger(new NetOverlaySample.Response(config.self, src, resp.id, gvodResp), network);
        } else {
            log.error("received wrong message - logic error");
            System.exit(1);
        }

    }

    @Override
    public void finish(UUID opId) {
        if (pendingOps.remove(opId) == null) {
            log.error("logic error handling operations");
            System.exit(1);
        }
        cleanRequests(opId);
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
        public final SerializationContext context;

        public BootstrapServerInit(BootstrapServerConfig config) {
            this.config = config;
            this.context = GVoDNetworkSettings.getContext();
        }
    }
}
