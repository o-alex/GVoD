
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.Key;
import se.sics.caracaldb.global.ForwardMessage;
import se.sics.caracaldb.global.Sample;
import se.sics.caracaldb.global.SampleRequest;
import se.sics.caracaldb.global.SchemaData;
import se.sics.caracaldb.operations.CaracalMsg;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.caracaldb.utils.ByteArrayFormatter;
import se.sics.gvod.bootstrap.cclient.operations.AddFileMetadataOp;
import se.sics.gvod.bootstrap.cclient.operations.AddOverlayPeerOp;
import se.sics.gvod.bootstrap.cclient.operations.CleanupOp;
import se.sics.gvod.bootstrap.cclient.operations.GetFileMetadataOp;
import se.sics.gvod.bootstrap.cclient.operations.GetOverlaySampleOp;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerMsg;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerPort;
import se.sics.gvod.bootstrap.server.peermanager.msg.CaracalReady;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMAddFileMetadata;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMGetFileMetadata;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMGetOverlaySample;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMJoinOverlay;
import se.sics.gvod.common.util.Operation;
import se.sics.gvod.timer.CancelPeriodicTimeout;
import se.sics.gvod.timer.CancelTimeout;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.TimeoutId;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CaracalPeerStoreComp extends ComponentDefinition implements CaracalOpManager {

    private static final Logger log = LoggerFactory.getLogger(CaracalPeerStoreComp.class);

    private Positive<Network> network = requires(Network.class);
    private Negative<PeerManagerPort> myPort = provides(PeerManagerPort.class);
    private Positive<Timer> timer = requires(Timer.class);

    private final CaracalPeerStoreConfig config;

    private final Map<UUID, Operation<CaracalOp>> pendingOps;
    private final Map<UUID, UUID> pendingCaracalOps;

    private TimeoutId sampleTimeout = null;
    private TimeoutId caracalPeriodicCleanupTid = null;
    private final List<Address> caracalNodes;
    private SchemaData schemaData = null;
    private Random rand = new Random();

    public CaracalPeerStoreComp(CaracalPeerStoreInit init) {
        this.config = init.config;
        log.info("{} initializing", config.self);

        this.pendingOps = new HashMap<UUID, Operation<CaracalOp>>();
        this.pendingCaracalOps = new HashMap<UUID, UUID>();
        this.caracalNodes = new ArrayList<Address>();

        subscribe(handleStart, control);

        subscribe(handleSample, network);
        subscribe(handleSampleTimeout, timer);

        subscribe(handleCaracalResponse, network);
        subscribe(handleJoinOverlay, myPort);
        subscribe(handleGetOverlaySample, myPort);
        subscribe(handleAddFileMetadata, myPort);
        subscribe(handleGetFileMetadata, myPort);
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} starting...", config.self);
            sendBootSampleRequest();
        }
    };

    private void sendBootSampleRequest() {
        log.info("{} sending boot sample request to caracal", config.self);
        scheduleCaracalSampleTimeout();
        trigger(new SampleRequest(config.self, config.caracalServer, 10, true), network);
    }

    private Handler<Sample> handleSample = new Handler<Sample>() {

        @Override
        public void handle(Sample sample) {
            log.info("{} received caracal sample - caracal contacted successfully");
            schemaData = SchemaData.deserialise(sample.schemaData);
            if (schemaData.getId("gvod.heartbeat") == null || schemaData.getId("gvod.metadata") == null) {
                log.error("{} caracal sample response does not contain gvod schemas - cannot communicate - shutdown");
                Kompics.shutdown();
            }
            log.info("{} schema prefixes - heartbeat:{} metadata:{}", new Object[]{config.self, ByteArrayFormatter.toHexString(schemaData.getId("gvod.heartbeat")), ByteArrayFormatter.toHexString(schemaData.getId("gvod.metadata"))});
            caracalNodes.addAll(sample.nodes);
            cancelCaracalReqTimeout(sampleTimeout);
            trigger(new CaracalReady(), myPort);
            schedulePeriodicHeartbeatCleanup();

            subscribe(handleHeartbeatCleanup, timer);
        }
    };

    private Handler<CaracalSampleTimeout> handleSampleTimeout = new Handler<CaracalSampleTimeout>() {

        @Override
        public void handle(CaracalSampleTimeout event) {
            if (sampleTimeout != null) {
                log.warn("{} caracal unreachable or still booting up - shutting down gvod", config.self);
                Kompics.shutdown();
            } else {
                log.debug("{} late sample timeout", config.self);
            }
        }

    };

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

    public Handler<PMJoinOverlay.Request> handleJoinOverlay = new Handler<PMJoinOverlay.Request>() {

        @Override
        public void handle(PMJoinOverlay.Request req) {
            log.debug("{} received {}", new Object[]{config.self, req});
            Operation op = new AddOverlayPeerOp(CaracalPeerStoreComp.this, req, schemaData);
            pendingOps.put(req.id, op);
            op.start();
        }
    };

    public Handler<PMGetOverlaySample.Request> handleGetOverlaySample = new Handler<PMGetOverlaySample.Request>() {

        @Override
        public void handle(PMGetOverlaySample.Request req) {
            log.debug("{} received {}", new Object[]{config.self, req});
            Operation op = new GetOverlaySampleOp(CaracalPeerStoreComp.this, req, schemaData, config.sampleSize);
            pendingOps.put(req.id, op);
            op.start();
        }
    };

    public Handler<PMAddFileMetadata.Request> handleAddFileMetadata = new Handler<PMAddFileMetadata.Request>() {

        @Override
        public void handle(PMAddFileMetadata.Request req) {
            log.debug("{} received {}", new Object[]{config.self, req});
            Operation op = new AddFileMetadataOp(CaracalPeerStoreComp.this, req, schemaData);
            pendingOps.put(req.id, op);
            op.start();
        }
    };

    public Handler<PMGetFileMetadata.Request> handleGetFileMetadata = new Handler<PMGetFileMetadata.Request>() {

        @Override
        public void handle(PMGetFileMetadata.Request req) {
            log.debug("{} received {}", new Object[]{config.self, req});
            Operation op = new GetFileMetadataOp(CaracalPeerStoreComp.this, req, schemaData);
            pendingOps.put(req.id, op);
            op.start();
        }
    };

    public Handler<CaracalCleanupTimeout> handleHeartbeatCleanup = new Handler<CaracalCleanupTimeout>() {

        @Override
        public void handle(CaracalCleanupTimeout event) {
            log.info("{} sending cleanup to caracal", config.self);
            Operation op = new CleanupOp(CaracalPeerStoreComp.this, schemaData, rand);
            pendingOps.put(op.getId(), op);
            op.start();
        }

    };

    private void scheduleCaracalSampleTimeout() {
        ScheduleTimeout st = new ScheduleTimeout(3000);
        Timeout t = new CaracalSampleTimeout(st);
        st.setTimeoutEvent(t);
        log.debug("{} scheduling timeout:{} for caracal sample", new Object[]{config.self, t.getTimeoutId()});
        trigger(st, timer);
        sampleTimeout = t.getTimeoutId();
    }

    private void cancelCaracalReqTimeout(TimeoutId tid) {
        log.debug("{} canceling timeout:{}", config.self, tid);
        CancelTimeout cancelSpeedUp = new CancelTimeout(tid);
        trigger(cancelSpeedUp, timer);
        sampleTimeout = null;
    }

    private void schedulePeriodicHeartbeatCleanup() {
        //TODO Alex move to config
        long cleanupPeriod = 30l * 1000; //30s to ms
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(cleanupPeriod, cleanupPeriod);
        Timeout t = new CaracalCleanupTimeout(spt);
        caracalPeriodicCleanupTid = t.getTimeoutId();
        spt.setTimeoutEvent(t);
        log.debug("{} scheduling periodic caracal heartbeat cleanup {}", config.self, t);
        trigger(spt, timer);
    }

    private void cancelPeriodicHeartbeatCleanup() {
        log.debug("{} canceling periodic timeout {}", config.self, caracalPeriodicCleanupTid);
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(caracalPeriodicCleanupTid);
        trigger(cpt, timer);
    }

    //**********OperationManager
    @Override
    public void finish(UUID id, PeerManagerMsg.Response resp) {
        if (pendingOps.remove(id) == null) {
            throw new RuntimeException("pendingOp should not be null");
        }
        cleanRequests(id);
        if (resp == null) {
            //cleanup resp;
            pendingOps.remove(id);
            log.info("cleanup successfull");
        } else {
            log.debug("{} sending {}", new Object[]{config.self, resp});
            trigger(resp, myPort);
        }
    }

    @Override
    public void sendCaracalReq(UUID opId, Key forwardTo, CaracalOp req) {
        pendingCaracalOps.put(req.id, opId);
//        Address caracalServer = caracalNodes.get(rand.nextInt(caracalNodes.size()));
        Address caracalServer = config.caracalServer;
        CaracalMsg cmsg = new CaracalMsg(config.self, caracalServer, req);
        log.debug("{} sending:{} to:{}", new Object[]{config.self, req, caracalServer});
        trigger(new ForwardMessage(config.self, caracalServer, forwardTo, cmsg), network);
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

    public static class CaracalPeerStoreInit extends Init<CaracalPeerStoreComp> {

        public final CaracalPeerStoreConfig config;

        public CaracalPeerStoreInit(CaracalPeerStoreConfig config) {
            this.config = config;
        }
    }
}
