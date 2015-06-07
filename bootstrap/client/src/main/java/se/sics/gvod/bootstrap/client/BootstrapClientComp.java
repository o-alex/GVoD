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
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.util.network.ContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapClientComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(BootstrapClientComp.class);

    private Negative<BootstrapClientPort> myPort = provides(BootstrapClientPort.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Positive<UtilityUpdatePort> utilityPort = requires(UtilityUpdatePort.class);

    private BootstrapClientConfig config;

    private final Random rand;

    private final Map<Integer, Integer> overlaysUtility;
    private final Set<DecoratedAddress> bootstrapNodes;
    private final Map<Integer, FileMetadata> pendingAddOverlay;

    private final Map<UUID, UUID> pendingRequests;
    private final Set<UUID> overlaySamples = new HashSet<UUID>();

    public BootstrapClientComp(BootstrapClientInit init) {
        this.config = init.config;
        log.debug("{} init", new Object[]{config.self});

        this.rand = new SecureRandom(config.seed);
        this.overlaysUtility = new HashMap<Integer, Integer>();
        this.bootstrapNodes = new HashSet<DecoratedAddress>();
        this.pendingAddOverlay = new HashMap<Integer, FileMetadata>();
        this.pendingRequests = new HashMap<UUID, UUID>();

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

    Handler handleStart = new Handler<Start>() {

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

    ClassMatchedHandler handleBootstrapResponse = new ClassMatchedHandler<BootstrapGlobal.Response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, BootstrapGlobal.Response>>() {

        @Override
        public void handle(BootstrapGlobal.Response content, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, BootstrapGlobal.Response> container) {
            if (content.status == ReqStatus.SUCCESS) {
                log.info("{} contacted caracalDB successfully", config.self);
                log.debug("{} global nodes {}", new Object[]{config.self, content.systemSample});
                cancelCaracalReqTimeout(pendingRequests.remove(content.id));
                for (DecoratedAddress peer : content.systemSample) {
                    if (bootstrapNodes.size() < config.openViewSize) {
                        bootstrapNodes.add(peer);
                    }
                }
            }
        }
    };

    Handler handleAddOverlayRequest = new Handler<AddOverlay.Request>() {

        @Override
        public void handle(AddOverlay.Request reqContent) {
            log.trace("{} {}", config.self, reqContent);
            log.debug("{} adding overlay:{}", config.self, reqContent.overlayId);
            pendingAddOverlay.put(reqContent.overlayId, reqContent.fileMeta);

            DecoratedHeader<DecoratedAddress> requestHeader = new DecoratedHeader(new BasicHeader(config.self, config.server, Transport.UDP), null, null);
            ContentMsg request = new BasicContentMsg(requestHeader, reqContent);

            log.trace("{} sending{} to:{}", new Object[]{config.self, reqContent, config.server});
            pendingRequests.put(reqContent.id, scheduleCaracalReqTimeout(reqContent.id));
            trigger(request, network);
        }
    };

    ClassMatchedHandler handleAddOverlayResponse = new ClassMatchedHandler<AddOverlay.Response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, AddOverlay.Response>>() {

        @Override
        public void handle(AddOverlay.Response content, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, AddOverlay.Response> container) {
            log.trace("{} net received:{}", new Object[]{config.self.toString(), content.toString()});
            cancelCaracalReqTimeout(pendingRequests.remove(content.id));
            trigger(content, myPort);
            FileMetadata fileMeta = pendingAddOverlay.remove(content.overlayId);
            if (fileMeta == null) {
                log.error("fileMeta missing");
                System.exit(1);
            }
            int downloadPos = fileMeta.fileSize / fileMeta.pieceSize + 1;
            overlaysUtility.put(content.overlayId, downloadPos);
        }
    };

    Handler handleJoinOverlayRequest = new Handler<JoinOverlay.Request>() {

        @Override
        public void handle(JoinOverlay.Request reqContent) {
            log.trace("{} {}", config.self, reqContent);
            log.debug("{} joining overlay:{}", config.self, reqContent.overlayId);
            DecoratedHeader<DecoratedAddress> requestHeader = new DecoratedHeader(new BasicHeader(config.self, config.server, Transport.UDP), null, null);
            ContentMsg request = new BasicContentMsg(requestHeader, reqContent);

            log.debug("{} sending {}", new Object[]{config.self, request});
            pendingRequests.put(reqContent.id, scheduleCaracalReqTimeout(reqContent.id));
            trigger(request, network);
        }
    };

    ClassMatchedHandler handleJoinOverlayResponse = new ClassMatchedHandler<JoinOverlay.Response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, JoinOverlay.Response>>() {

        @Override
        public void handle(JoinOverlay.Response content, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, JoinOverlay.Response> container) {
            log.trace("{} net received:{}", new Object[]{config.self.toString(), content.toString()});
            cancelCaracalReqTimeout(pendingRequests.remove(content.id));
            trigger(content, myPort);
            int downloadPos = 0;
            overlaysUtility.put(content.overlayId, downloadPos);
        }
    };

    Handler handleOverlaySampleRequest = new Handler<OverlaySample.Request>() {

        @Override
        public void handle(OverlaySample.Request reqContent) {
            log.trace("{} {} - overlay:{}", new Object[]{config.self, reqContent, reqContent.overlayId});

            DecoratedHeader<DecoratedAddress> requestHeader = new DecoratedHeader(new BasicHeader(config.self, config.server, Transport.UDP), null, null);
            ContentMsg request = new BasicContentMsg(requestHeader, reqContent);

            log.debug("{} sending {}", new Object[]{config.self, request});
            pendingRequests.put(reqContent.id, scheduleCaracalReqTimeout(reqContent.id));
            overlaySamples.add(reqContent.id);
            trigger(request, network);
        }
    };

    ClassMatchedHandler handleOverlaySampleResponse = new ClassMatchedHandler<OverlaySample.Response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, OverlaySample.Response>>() {

        @Override
        public void handle(OverlaySample.Response content, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, OverlaySample.Response> container) {
            log.trace("{} net received {}", new Object[]{config.self.toString(), container.toString()});
            cancelCaracalReqTimeout(pendingRequests.remove(content.id));
            overlaySamples.remove(content.id);
            trigger(content, myPort);
        }
    };

    Handler handleUtilityUpdate = new Handler<UtilityUpdate>() {

        @Override
        public void handle(UtilityUpdate update) {
            log.debug("{} {}", new Object[]{config.self, update});
            overlaysUtility.put(update.overlayId, update.downloadPos);
        }
    };

    Handler handleHeartbeat = new Handler<Heartbeat.PeriodicTimeout>() {

        @Override
        public void handle(Heartbeat.PeriodicTimeout timeout) {
            log.info("{} periodic heartbeat, active overlays:{}", new Object[]{config.self, overlaysUtility});

            Heartbeat.OneWay heartbeat = new Heartbeat.OneWay(UUID.randomUUID(), new HashMap<Integer, Integer>(overlaysUtility));
            DecoratedHeader<DecoratedAddress> requestHeader = new DecoratedHeader(new BasicHeader(config.self, config.server, Transport.UDP), null, null);
            ContentMsg request = new BasicContentMsg(requestHeader, heartbeat);

            log.debug("{} sending {}", new Object[]{config.self, request});
            trigger(request, network);
        }
    };

    public Handler<CaracalReqTimeout> handleCaracalReqTimeout = new Handler<CaracalReqTimeout>() {

        @Override
        public void handle(CaracalReqTimeout timeout) {
            log.debug("{} timeout for req:{}", new Object[]{config.self, timeout.reqId});

            UUID tid = pendingRequests.remove(timeout.reqId);
            if (tid == null) {
                log.debug("{} late timeout:{}", new Object[]{config.self, tid});
                return;
            } else {
                if (overlaySamples.contains(timeout.reqId)) {
                    //sometime caracal times out here... fix later .. this is only necessary for croupier fake, so we can skip one in a while
                    log.warn("{} caracal timed out on a sample request", config.self);
                    overlaySamples.remove(timeout.reqId);
                } else {
                    log.error("{} caracal timed out - shutting down", config.self);
                    System.exit(1);
                }
            }
        }

    };

    private UUID scheduleCaracalReqTimeout(UUID reqId) {
        ScheduleTimeout st = new ScheduleTimeout(3000);
        Timeout t = new CaracalReqTimeout(st, reqId);
        st.setTimeoutEvent(t);
        log.debug("{} scheduling timeout:{} for caracal req:{}", new Object[]{config.self, t.getTimeoutId(), reqId});
        trigger(st, timer);
        return t.getTimeoutId();
    }

    private void cancelCaracalReqTimeout(UUID tid) {
        log.debug("{} canceling timeout:{}", config.self, tid);
        CancelTimeout cancelSpeedUp = new CancelTimeout(tid);
        trigger(cancelSpeedUp, timer);
    }
}
