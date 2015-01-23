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

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.common.utility.UtilityUpdate;
import se.sics.gvod.common.utility.UtilityUpdatePort;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.AddOverlay;
import se.sics.gvod.common.msg.peerMngr.BootstrapGlobal;
import se.sics.gvod.common.msg.peerMngr.Heartbeat;
import se.sics.gvod.common.msg.peerMngr.JoinOverlay;
import se.sics.gvod.common.msg.peerMngr.OverlaySample;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.network.netmsg.bootstrap.NetAddOverlay;
import se.sics.gvod.network.netmsg.bootstrap.NetBootstrapGlobal;
import se.sics.gvod.network.netmsg.bootstrap.NetHeartbeat;
import se.sics.gvod.network.netmsg.bootstrap.NetJoinOverlay;
import se.sics.gvod.network.netmsg.bootstrap.NetOverlaySample;
import se.sics.gvod.timer.CancelTimeout;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.TimeoutId;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapClientComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(BootstrapClientComp.class);

    private Negative<BootstrapClientPort> myPort = provides(BootstrapClientPort.class);
    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Positive<UtilityUpdatePort> utilityPort = requires(UtilityUpdatePort.class);

    private BootstrapClientConfig config;

    private final Random rand;

    private final Map<Integer, Integer> overlaysUtility;
    private final Set<VodAddress> bootstrapNodes;
    private final Map<Integer, FileMetadata> pendingAddOverlay;
    
    private final Map<UUID, TimeoutId> pendingRequests;

    public BootstrapClientComp(BootstrapClientInit init) {
        this.config = init.config;
        log.debug("{} init", new Object[]{config.self});

        this.rand = new SecureRandom(config.seed);
        this.overlaysUtility = new HashMap<Integer, Integer>();
        this.bootstrapNodes = new HashSet<VodAddress>();
        this.pendingAddOverlay = new HashMap<Integer, FileMetadata>();
        this.pendingRequests = new HashMap<UUID, TimeoutId>();

        subscribe(handleStart, control);
        subscribe(handleAddOverlayRequest, myPort);
        subscribe(handleJoinOverlayRequest, myPort);
        subscribe(handleOverlaySampleRequest, myPort);
        subscribe(handleBootstrapResponse, network);
        subscribe(handleAddOverlayResponse, network);
        subscribe(handleJoinOverlayResponse, network);
        subscribe(handleOverlaySampleResponse, network);
        subscribe(handleUtilityUpdate, utilityPort);
        subscribe(handleHeartbeat, timer);
        subscribe(handleCaracalReqTimeout, timer);
    }

    public Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            //TODO Alex uncoment and fix later
//            BootstrapGlobal.Request req = new BootstrapGlobal.Request(UUID.randomUUID());
//            NetBootstrapGlobal.Request netReq = new NetBootstrapGlobal.Request(config.self, config.server, req.id, req);
//            log.info("{} contacting caracalDB - sending {}", new Object[]{config.self, netReq.toString()});
//            pendingRequests.put(req.id, scheduleCaracalReqTimeout(req.id));
//            trigger(netReq, network);

            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.heartbeatPeriod, config.heartbeatPeriod);
            spt.setTimeoutEvent(new Heartbeat.PeriodicTimeout(spt));
            trigger(spt, timer);
        }
    };

    public Handler<NetBootstrapGlobal.Response> handleBootstrapResponse = new Handler<NetBootstrapGlobal.Response>() {

        @Override
        public void handle(NetBootstrapGlobal.Response resp) {
            if (resp.content.status == ReqStatus.SUCCESS) {
                log.info("{} contacted caracalDB successfully", config.self);
                log.debug("{} global nodes {}", new Object[]{config.self, resp.content.systemSample});
                cancelCaracalReqTimeout(pendingRequests.remove(resp.id));
                for (VodAddress peer : resp.content.systemSample) {
                    if (bootstrapNodes.size() < config.openViewSize) {
                        bootstrapNodes.add(peer);
                    }
                }
            }
        }
    };

    public Handler<AddOverlay.Request> handleAddOverlayRequest = new Handler<AddOverlay.Request>() {

        @Override
        public void handle(AddOverlay.Request req) {
            log.trace("{} {}", config.self, req);
            log.debug("{} adding overlay:{}", config.self, req.overlayId);
            pendingAddOverlay.put(req.overlayId, req.fileMeta);

            NetAddOverlay.Request netReq = new NetAddOverlay.Request(config.self, config.server, req.id, req);
            log.trace("{} sending {}", new Object[]{config.self, netReq});
            pendingRequests.put(req.id, scheduleCaracalReqTimeout(req.id));
            trigger(netReq, network);
        }
    };

    public Handler<JoinOverlay.Request> handleJoinOverlayRequest = new Handler<JoinOverlay.Request>() {

        @Override
        public void handle(JoinOverlay.Request req) {
            log.trace("{} received {}", config.self, req);
            log.debug("{} joining overlay:{}", config.self, req.overlayId);
            NetJoinOverlay.Request netReq = new NetJoinOverlay.Request(config.self, config.server, req.id, req);
            log.debug("{} sending {}", new Object[]{config.self, netReq});
            pendingRequests.put(req.id, scheduleCaracalReqTimeout(req.id));
            trigger(netReq, network);
        }
    };

    public Handler<OverlaySample.Request> handleOverlaySampleRequest = new Handler<OverlaySample.Request>() {

        @Override
        public void handle(OverlaySample.Request req) {
            log.trace("{} {} - overlay:{}", new Object[]{config.self, req, req.overlayId});

            NetOverlaySample.Request netReq = new NetOverlaySample.Request(config.self, config.server, req.id, req);
            log.debug("{} sending {}", new Object[]{config.self, netReq});
            pendingRequests.put(req.id, scheduleCaracalReqTimeout(req.id));
            trigger(netReq, network);
        }
    };

    public Handler<NetAddOverlay.Response> handleAddOverlayResponse = new Handler<NetAddOverlay.Response>() {

        @Override
        public void handle(NetAddOverlay.Response resp) {
            log.trace("{} {}", new Object[]{config.self.toString(), resp.toString()});
            cancelCaracalReqTimeout(pendingRequests.remove(resp.id));
            trigger(resp.content, myPort);
            FileMetadata fileMeta = pendingAddOverlay.remove(resp.content.overlayId);
            if (fileMeta == null) {
                throw new RuntimeException("missing");
            }
            int downloadPos = fileMeta.fileSize / fileMeta.pieceSize + 1;
            overlaysUtility.put(resp.content.overlayId, downloadPos);
        }
    };

    public Handler<NetJoinOverlay.Response> handleJoinOverlayResponse = new Handler<NetJoinOverlay.Response>() {

        @Override
        public void handle(NetJoinOverlay.Response resp) {
            log.trace("{} {}", new Object[]{config.self.toString(), resp.toString()});
            cancelCaracalReqTimeout(pendingRequests.remove(resp.id));
            trigger(resp.content, myPort);
            int downloadPos = 0;
            overlaysUtility.put(resp.content.overlayId, downloadPos);
        }
    };

    public Handler<NetOverlaySample.Response> handleOverlaySampleResponse = new Handler<NetOverlaySample.Response>() {

                @Override
                public void handle(NetOverlaySample.Response resp) {
                    log.trace("{} {}", new Object[]{config.self.toString(), resp.toString()});
                    cancelCaracalReqTimeout(pendingRequests.remove(resp.id));
                    trigger(resp.content, myPort);
                }
            };

    public Handler<UtilityUpdate> handleUtilityUpdate = new Handler<UtilityUpdate>() {

        @Override
        public void handle(UtilityUpdate update) {
            log.debug("{} {}", new Object[]{config.self, update});
            overlaysUtility.put(update.overlayId, update.downloadPos);
        }
    };

    public Handler<Heartbeat.PeriodicTimeout> handleHeartbeat = new Handler<Heartbeat.PeriodicTimeout>() {

        @Override
        public void handle(Heartbeat.PeriodicTimeout timeout) {
            log.info("{} periodic heartbeat, active overlays:{}", new Object[]{config.self, overlaysUtility});

            Heartbeat.OneWay heartbeat = new Heartbeat.OneWay(UUID.randomUUID(), new HashMap<Integer, Integer>(overlaysUtility));
            NetHeartbeat.OneWay netOneWay = new NetHeartbeat.OneWay(config.self, config.server, heartbeat.id, heartbeat);
            log.debug("{} sending {}", new Object[]{config.self, netOneWay});
            trigger(netOneWay, network);
        }
    };
    
    public Handler<CaracalReqTimeout> handleCaracalReqTimeout = new Handler<CaracalReqTimeout>() {

        @Override
        public void handle(CaracalReqTimeout timeout) {
            log.debug("{} timeout for req:{}", new Object[]{config.self, timeout.reqId});

            TimeoutId tid = pendingRequests.remove(timeout.reqId);
            if(tid == null) {
                log.debug("{} late timeout:{}", new Object[]{config.self, tid});
                return;
            } else {
                log.error("{} caracal timed out - shutting down", config.self);
                Kompics.shutdown();
            }
        }
        
    };
    
    private TimeoutId scheduleCaracalReqTimeout(UUID reqId) {
        ScheduleTimeout st = new ScheduleTimeout(3000);
        Timeout t = new CaracalReqTimeout(st, reqId);
        st.setTimeoutEvent(t);
        log.debug("{} scheduling timeout:{} for caracal req:{}", new Object[]{config.self, t.getTimeoutId(), reqId});
        trigger(st, timer);
        return t.getTimeoutId();
    }

    private void cancelCaracalReqTimeout(TimeoutId tid) {
        log.debug("{} canceling timeout:{}", config.self, tid);
        CancelTimeout cancelSpeedUp = new CancelTimeout(tid);
        trigger(cancelSpeedUp, timer);
    }
}
