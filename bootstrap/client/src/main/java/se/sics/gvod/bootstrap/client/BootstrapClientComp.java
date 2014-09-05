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
import se.sics.gvod.bootstrap.client.utility.UtilityUpdate;
import se.sics.gvod.bootstrap.client.utility.UtilityUpdatePort;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.AddOverlay;
import se.sics.gvod.common.msg.impl.BootstrapGlobal;
import se.sics.gvod.common.msg.impl.Heartbeat;
import se.sics.gvod.common.msg.impl.JoinOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.common.util.MsgProcessor;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.network.nettymsg.GvodNetMsg;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapClientComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(BootstrapClientComp.class);

    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Negative<BootstrapClientPort> myPort = provides(BootstrapClientPort.class);
    private Negative<UtilityUpdatePort> utilityPort = provides(UtilityUpdatePort.class);

    private BootstrapClientConfig config;

    private final MsgProcessor msgProc;
    private final Random rand;

    private final Map<Integer, Integer> overlaysUtility;
    private final Set<VodAddress> bootstrapNodes;
    private final Map<Integer, FileMetadata> pendingAddOverlay;

    public BootstrapClientComp(BootstrapClientInit init) {
        this.config = init.config;
        log.debug("{} init", new Object[]{config.self});

        this.msgProc = new MsgProcessor();
        this.rand = new SecureRandom(config.seed);
        this.overlaysUtility = new HashMap<Integer, Integer>();
        this.bootstrapNodes = new HashSet<VodAddress>();
        this.pendingAddOverlay = new HashMap<Integer, FileMetadata>();

        subscribe(handleStart, control);
        subscribe(handleNetResponse, network);
        subscribe(handleAddOverlayRequest, myPort);
        subscribe(handleJoinOverlayRequest, myPort);
        msgProc.subscribe(handleBootstrapResponse);
        msgProc.subscribe(handleAddOverlayResponse);
        msgProc.subscribe(handleJoinOverlayResponse);
        subscribe(handleUtilityUpdate, utilityPort);
        subscribe(handleHeartbeat, timer);
    }

    public Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            BootstrapGlobal.Request req = new BootstrapGlobal.Request(UUID.randomUUID());
            GvodNetMsg.Request netReq = new GvodNetMsg.Request(config.self, config.server, req);
            log.debug("{} sending {}", new Object[]{config.self, netReq.toString()});
            trigger(netReq, network);

            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.heartbeatPeriod, config.heartbeatPeriod);
            spt.setTimeoutEvent(new Heartbeat.PeriodicTimeout(spt));
            trigger(spt, timer);
        }
    };

    public Handler<GvodNetMsg.Response<GvodMsg.Response>> handleNetResponse = new Handler<GvodNetMsg.Response<GvodMsg.Response>>() {

        @Override
        public void handle(GvodNetMsg.Response<GvodMsg.Response> netResp) {
            log.debug("received {}", netResp.toString());
            msgProc.trigger(netResp.getVodSource(), netResp.payload);
        }
    };

    public MsgProcessor.Handler<BootstrapGlobal.Response> handleBootstrapResponse
            = new MsgProcessor.Handler<BootstrapGlobal.Response>(BootstrapGlobal.Response.class) {

                @Override
                public void handle(VodAddress src, BootstrapGlobal.Response resp) {
                    if (resp.status == ReqStatus.SUCCESS) {
                        log.debug("{} global nodes {}", new Object[]{config.self, resp.systemSample});
                        for (VodAddress peer : resp.systemSample) {
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
            log.trace("{} {} - overlay:{}", new Object[]{config.self, req, req.overlayId});
            pendingAddOverlay.put(req.overlayId, req.fileMeta);

            GvodNetMsg.Request<AddOverlay.Request> netReq = new GvodNetMsg.Request(config.self, config.server, req);
            log.trace("{} sending {}", new Object[]{config.self, netReq});
            trigger(netReq, network);
        }
    };

    public Handler<JoinOverlay.Request> handleJoinOverlayRequest = new Handler<JoinOverlay.Request>() {

        @Override
        public void handle(JoinOverlay.Request req) {
            log.trace("{} {} - overlay:{}", new Object[]{config.self, req, req.overlayId});

            GvodNetMsg.Request<JoinOverlay.Request> netReq = new GvodNetMsg.Request(config.self, config.server, req);
            log.debug("{} sending {}", new Object[]{config.self, netReq});
            trigger(netReq, network);
        }
    };

    public MsgProcessor.Handler<AddOverlay.Response> handleAddOverlayResponse
            = new MsgProcessor.Handler<AddOverlay.Response>(AddOverlay.Response.class
            ) {

                @Override
                public void handle(VodAddress src, AddOverlay.Response resp) {
                    log.trace("{} {}", new Object[]{config.self.toString(), resp.toString()});
                    trigger(resp, myPort);
                    FileMetadata fileMeta = pendingAddOverlay.remove(resp.overlayId);
                    if (fileMeta == null) {
                        throw new RuntimeException("missing");
                    }
                    int downloadPos = fileMeta.size / fileMeta.pieceSize + 1;
                    overlaysUtility.put(resp.overlayId, downloadPos);
                }
            };
    public MsgProcessor.Handler<JoinOverlay.Response> handleJoinOverlayResponse
            = new MsgProcessor.Handler<JoinOverlay.Response>(JoinOverlay.Response.class
            ) {

                @Override
                public void handle(VodAddress src, JoinOverlay.Response resp) {
                    log.trace("{} {}", new Object[]{config.self.toString(), resp.toString()});
                    trigger(resp, myPort);

                    int downloadPos = 0;
                    overlaysUtility.put(resp.overlayId, downloadPos);
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
            log.trace("{} periodic heartbeat", new Object[]{config.self});

            Heartbeat.OneWay heartbeat = new Heartbeat.OneWay(UUID.randomUUID(), new HashMap<Integer, Integer>(overlaysUtility));
            GvodNetMsg.OneWay<Heartbeat.OneWay> netOneWay = new GvodNetMsg.OneWay<Heartbeat.OneWay>(config.self, config.server, heartbeat);
            log.debug("{} sending {}", new Object[]{config.self, netOneWay});
            trigger(netOneWay, network);
        }
    };
}
