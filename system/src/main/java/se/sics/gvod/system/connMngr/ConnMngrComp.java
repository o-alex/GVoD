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
package se.sics.gvod.system.connMngr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.util.MsgProcessor;
import se.sics.gvod.croupierfake.CroupierPort;
import se.sics.gvod.croupierfake.CroupierSample;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.common.util.VodDescriptor;
import se.sics.gvod.network.nettymsg.OverlayNetMsg;
import se.sics.gvod.common.msg.vod.Connection;
import se.sics.gvod.system.connMngr.msg.Ready;
import se.sics.gvod.common.msg.vod.Download;
import se.sics.gvod.common.utility.UtilityUpdate;
import se.sics.gvod.common.utility.UtilityUpdatePort;
import se.sics.gvod.system.connMngr.msg.DownloadDataTimeout;
import se.sics.gvod.system.connMngr.msg.DownloadHashTimeout;
import se.sics.gvod.system.connMngr.msg.ScheduleConnUpdate;
import se.sics.gvod.timer.CancelTimeout;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.TimeoutId;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ConnMngrComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(ConnMngrComp.class);

    private Negative<ConnMngrPort> myPort = provides(ConnMngrPort.class);
    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Positive<CroupierPort> croupier = requires(CroupierPort.class);
    private Positive<UtilityUpdatePort> utilityUpdate = requires(UtilityUpdatePort.class);

    private final ConnMngrConfig config;
    private final MsgProcessor msgProc;

    private LocalVodDescriptor selfDesc;

    private Map<VodAddress, DownloaderVodDescriptor> downloadersConn;
    private Map<VodAddress, VodDescriptor> pendingUploadersConn;
    private Map<VodAddress, UploaderVodDescriptor> uploadersConn;

    private Map<VodAddress, Map<UUID, Pair<Download.DataRequest, TimeoutId>>> pendingDataTraffic; //<source, <reqId, <req, timeoutId>>>
    private Map<VodAddress, Map<UUID, Pair<Download.HashRequest, TimeoutId>>> pendingHashTraffic;
    private Map<Integer, Set<VodAddress>> pendingUploadReq;

    private TimeoutId connUpdateTId;

    private boolean ready;

    public ConnMngrComp(ConnMngrInit init) {
        this.config = init.config;
        log.info("{} creating...", config.getSelf());

        this.selfDesc = null;
        this.msgProc = new MsgProcessor();
        this.downloadersConn = new HashMap<VodAddress, DownloaderVodDescriptor>();
        this.pendingUploadersConn = new HashMap<VodAddress, VodDescriptor>();
        this.uploadersConn = new HashMap<VodAddress, UploaderVodDescriptor>();
        this.pendingDataTraffic = new HashMap<VodAddress, Map<UUID, Pair<Download.DataRequest, TimeoutId>>>();
        this.pendingHashTraffic = new HashMap<VodAddress, Map<UUID, Pair<Download.HashRequest, TimeoutId>>>();
        this.pendingUploadReq = new HashMap<Integer, Set<VodAddress>>();
        this.ready = false;

        subscribe(handleUpdateUtility, utilityUpdate);
    }

    private Handler<UtilityUpdate> handleUpdateUtility = new Handler<UtilityUpdate>() {

        @Override
        public void handle(UtilityUpdate event) {
            log.trace("{} updating self descriptor", config.getSelf(), selfDesc);

            if (selfDesc == null) {
                selfDesc = new LocalVodDescriptor(new VodDescriptor(event.downloadPos), event.downloading);
                start();
            } else {

                if (selfDesc.downloading && !event.downloading) {
                    log.debug("{} completed - closing download connections", config.getSelf());
                    for (VodAddress partner : uploadersConn.keySet()) {
                        Connection.Close cl = new Connection.Close(UUID.randomUUID());
                        trigger(new OverlayNetMsg.OneWay(config.getSelf(), partner, config.overlayId, cl), network);
                    }
                    for (VodAddress partner : pendingUploadersConn.keySet()) {
                        Connection.Close cl = new Connection.Close(UUID.randomUUID());
                        trigger(new OverlayNetMsg.OneWay(config.getSelf(), partner, config.overlayId, cl), network);
                    }
                    uploadersConn = new HashMap<VodAddress, UploaderVodDescriptor>();
                    pendingUploadersConn = new HashMap<VodAddress, VodDescriptor>();

                    log.debug("{} cleaning timeouts", config.getSelf());
                    for (Map<UUID, Pair<Download.DataRequest, TimeoutId>> partnerTIds : pendingDataTraffic.values()) {
                        for (Pair<Download.DataRequest, TimeoutId> downReq : partnerTIds.values()) {
                            CancelTimeout ct = new CancelTimeout(downReq.getValue1());
                            trigger(ct, timer);
                        }
                    }

                    CancelTimeout ct = new CancelTimeout(connUpdateTId);
                    trigger(ct, timer);

                    pendingDataTraffic = null;
                }
                selfDesc = new LocalVodDescriptor(new VodDescriptor(event.downloadPos), event.downloading);
            }
        }
    };

    private void start() {

        log.debug("{} starting...", config.getSelf());
        if (selfDesc.downloading) {
            connUpdateTId = scheduleConnectionUpdate();
        } else {
            ready = true;
            trigger(new Ready(UUID.randomUUID()), myPort);
        }

        subscribe(handleNetRequest, network);
        subscribe(handleNetResponse, network);
        subscribe(handleNetOneWay, network);

        subscribe(handleScheduledConnectionUpdate, timer);
        subscribe(handleCroupierSample, croupier);

        msgProc.subscribe(handleConnectionRequest);
        msgProc.subscribe(handleConnectionResponse);
        msgProc.subscribe(handleConnectionUpdate);
        msgProc.subscribe(handleConnectionClose);

//        subscribe(handleLocalHashRequest, myPort);
//        subscribe(handleLocalHashResponse, myPort);
        subscribe(handleLocalDataRequest, myPort);
        subscribe(handleLocalDataResponse, myPort);
        subscribe(handleDownloadDataTimeout, timer);
        subscribe(handleDownloadHashTimeout, timer);

//        msgProc.subscribe(handleNetHashRequest);
//        msgProc.subscribe(handleNetHashResponse);
        msgProc.subscribe(handleNetDataRequest);
        msgProc.subscribe(handleNetDataResponse);

    }

    private Handler<OverlayNetMsg.OneWay> handleNetOneWay = new Handler<OverlayNetMsg.OneWay>() {

        @Override
        public void handle(OverlayNetMsg.OneWay netReq) {
            log.trace("{} received {}", config.getSelf(), netReq.toString());
            msgProc.trigger(netReq.getVodSource(), netReq.payload);
        }
    };

    private Handler<OverlayNetMsg.Request> handleNetRequest = new Handler<OverlayNetMsg.Request>() {

        @Override
        public void handle(OverlayNetMsg.Request netReq) {
            log.trace("{} received {}", config.getSelf(), netReq.toString());
            msgProc.trigger(netReq.getVodSource(), netReq.payload);
        }
    };

    private Handler<OverlayNetMsg.Response> handleNetResponse = new Handler<OverlayNetMsg.Response>() {

        @Override
        public void handle(OverlayNetMsg.Response netResp) {
            log.trace("{} received {}", config.getSelf(), netResp.toString());
            msgProc.trigger(netResp.getVodSource(), netResp.payload);
        }
    };

    // CONNECTION MANAGEMENT
    private Handler<CroupierSample> handleCroupierSample = new Handler<CroupierSample>() {

        @Override
        public void handle(CroupierSample event) {
            log.debug("{} handle new samples {}", config.getSelf(), event.sample);

            for (Map.Entry<VodAddress, VodDescriptor> e : event.sample.entrySet()) {
                if (e.getValue().downloadPos < selfDesc.vodDesc.downloadPos) {
                    continue;
                }
                if (uploadersConn.containsKey(e.getKey()) || pendingUploadersConn.containsKey(e.getKey())) {
                    continue;
                }
                log.debug("{} opening connection to {}", config.getSelf(), e.getKey());
                pendingUploadersConn.put(e.getKey(), e.getValue());
                Connection.Request req = new Connection.Request(UUID.randomUUID(), selfDesc.vodDesc);
                trigger(new OverlayNetMsg.Request(config.getSelf(), e.getKey(), config.overlayId, req), network);
            }
        }
    };

    private Handler<ScheduleConnUpdate> handleScheduledConnectionUpdate = new Handler<ScheduleConnUpdate>() {

        @Override
        public void handle(ScheduleConnUpdate event) {
            log.trace("{} handle {}", config.getSelf(), event);
            for (VodAddress partner : downloadersConn.keySet()) {
                Connection.Update upd = new Connection.Update(UUID.randomUUID(), selfDesc.vodDesc);
                trigger(new OverlayNetMsg.OneWay(config.getSelf(), partner, config.overlayId, upd), network);
            }

            for (VodAddress partner : uploadersConn.keySet()) {
                Connection.Update upd = new Connection.Update(UUID.randomUUID(), selfDesc.vodDesc);
                trigger(new OverlayNetMsg.OneWay(config.getSelf(), partner, config.overlayId, upd), network);
            }
        }
    };

    private MsgProcessor.Handler<Connection.Request> handleConnectionRequest
            = new MsgProcessor.Handler<Connection.Request>(Connection.Request.class) {

                @Override
                public void handle(VodAddress src, Connection.Request req) {
                    log.trace("{} handle {}", new Object[]{config.getSelf(), req});

                    if (downloadersConn.containsKey(src)) {
                        return;
                    }

                    log.debug("{} new connection to downloader {}", config.getSelf(), src);
                    downloadersConn.put(src, new DownloaderVodDescriptor(req.desc, config.defaultMaxPipeline));
                    Connection.Response resp = req.accept();
                    trigger(new OverlayNetMsg.Response(config.getSelf(), src, config.overlayId, resp), network);
                }
            };

    private MsgProcessor.Handler<Connection.Response> handleConnectionResponse
            = new MsgProcessor.Handler<Connection.Response>(Connection.Response.class) {

                @Override
                public void handle(VodAddress src, Connection.Response resp) {
                    log.trace("{} handle {}", new Object[]{config.getSelf(), resp});

                    if (!resp.status.equals(ReqStatus.SUCCESS)) {
                        log.debug("{} connection req status {}", config.getSelf(), resp.status);
                        pendingUploadersConn.remove(src);
                        return;
                    }

                    if (!pendingUploadersConn.containsKey(src)) {
                        log.debug("{} closing connection to {}", config.getSelf(), src);
                        trigger(new OverlayNetMsg.OneWay(config.getSelf(), src, config.overlayId, new Connection.Close(UUID.randomUUID())), network);
                        return;
                    }

                    log.debug("{} new connection to uploader {}", config.getSelf(), src);
                    uploadersConn.put(src, new UploaderVodDescriptor(pendingUploadersConn.remove(src), config.defaultMaxPipeline));

                    if (!ready) {
                        log.debug("{} first uploader connection, can start serving", config.getSelf());
                        ready = true;
                        trigger(new Ready(UUID.randomUUID()), myPort);
                    }
                }
            };

    private MsgProcessor.Handler<Connection.Update> handleConnectionUpdate
            = new MsgProcessor.Handler<Connection.Update>(Connection.Update.class) {

                @Override
                public void handle(VodAddress src, Connection.Update event) {
                    log.trace("{} handle {}", new Object[]{config.getSelf(), event});

                    if (downloadersConn.containsKey(src)) {
                        downloadersConn.get(src).updateDesc(event.desc);
                    }

                    if (uploadersConn.containsKey(src)) {
                        uploadersConn.get(src).updateDesc(event.desc);
                    }
                }
            };

    private MsgProcessor.Handler<Connection.Close> handleConnectionClose
            = new MsgProcessor.Handler<Connection.Close>(Connection.Close.class) {

                @Override
                public void handle(VodAddress src, Connection.Close event) {
                    log.debug("{} handle {}", new Object[]{config.getSelf(), event});
                    downloadersConn.remove(src);
                    pendingUploadersConn.remove(src);
                    uploadersConn.remove(src);
                }
            };

    //DOWNLOAD MANAGEMENT - partener load management
    private Handler<Download.DataRequest> handleLocalDataRequest = new Handler<Download.DataRequest>() {

        @Override
        public void handle(Download.DataRequest req) {
            log.trace("{} handle local {}", config.getSelf(), req);

            Map.Entry<VodAddress, UploaderVodDescriptor> uploader = getUploader(req.pieceId);
            if (uploader == null) {
                log.debug("{} no candidate for piece {}", new Object[]{config.getSelf(), req.pieceId});
                trigger(req.busy(), myPort);
                return;
            }
            uploader.getValue().useSlot();

            Map<UUID, Pair<Download.DataRequest, TimeoutId>> partnerReq = pendingDataTraffic.get(uploader.getKey());
            if (partnerReq == null) {
                partnerReq = new HashMap<UUID, Pair<Download.DataRequest, TimeoutId>>();
                pendingDataTraffic.put(uploader.getKey(), partnerReq);
            }
            partnerReq.put(req.id, Pair.with(req, scheduleDownloadRequestTimeout(uploader.getKey(), req.id)));
            trigger(new OverlayNetMsg.Request(config.getSelf(), uploader.getKey(), config.overlayId, req), network);
        }

    };

    private Map.Entry<VodAddress, UploaderVodDescriptor> getUploader(int pieceId) {
        Iterator<Map.Entry<VodAddress, UploaderVodDescriptor>> it = uploadersConn.entrySet().iterator();
        Map.Entry<VodAddress, UploaderVodDescriptor> candidate = null;
        //get first viable candidate
        while (it.hasNext()) {
            candidate = it.next();
            if (candidate.getValue().isViable(pieceId)) {
                break;
            }
            candidate = null;
        }
        //get best candidate
        while (it.hasNext()) {
            Map.Entry<VodAddress, UploaderVodDescriptor> nextC = it.next();
            if (nextC.getValue().betterCandidate(candidate.getValue(), pieceId)) {
                candidate = nextC;
            }
        }

        return candidate;
    }

    private MsgProcessor.Handler<Download.DataRequest> handleNetDataRequest
            = new MsgProcessor.Handler<Download.DataRequest>(Download.DataRequest.class) {

                @Override
                public void handle(VodAddress src, Download.DataRequest req) {
                    log.trace("{} handling network {}", new Object[]{config.getSelf(), req});
                    log.debug("{} requested unique pieces {}", pendingUploadReq.size());

                    DownloaderVodDescriptor downDesc = downloadersConn.get(src);
                    if (downDesc == null) {
                        log.debug("{} no connection open, dropping req {} from {}", new Object[]{config.getSelf(), req, src});
                        return;
                    }
                    if (!downDesc.isViable()) {
                        //TODO should not happen yet, but will need to be treated later, when downloader has no more slots available
                        log.info("{} no more slots for peer:{}", config.getSelf(), src);
                        return;
                    }
                    downDesc.useSlot();

                    Set<VodAddress> requesters = pendingUploadReq.get(req.pieceId);
                    if (requesters == null) {
                        requesters = new HashSet<VodAddress>();
                        pendingUploadReq.put(req.pieceId, requesters);
                        trigger(req, myPort);
                    }
                    requesters.add(src);
                }
            };

    private Handler<Download.DataResponse> handleLocalDataResponse = new Handler<Download.DataResponse>() {

        @Override
        public void handle(Download.DataResponse resp) {
            log.trace("{} handle local {}", config.getSelf(), resp);

            Set<VodAddress> requesters = pendingUploadReq.get(resp.pieceId);
            if (requesters == null) {
                log.debug("{} no requesters for piece {}", config.getSelf(), resp.pieceId);
                return;
            }

            for (VodAddress src : requesters) {
                DownloaderVodDescriptor down = downloadersConn.get(src);
                if (down != null) {
                    log.debug("{} sending piece {} to {}", new Object[]{config.getSelf(), resp.pieceId, src});
                    trigger(new OverlayNetMsg.Response(config.getSelf(), src, config.overlayId, resp), network);
                    down.freeSlot();
                }
            }
            pendingUploadReq.remove(resp.pieceId);
        }
    };

    private MsgProcessor.Handler<Download.DataResponse> handleNetDataResponse
            = new MsgProcessor.Handler<Download.DataResponse>(Download.DataResponse.class) {

                @Override
                public void handle(VodAddress src, Download.DataResponse resp) {
                    log.trace("{} handle net {}", new Object[]{config.getSelf(), resp});
                    UploaderVodDescriptor up = uploadersConn.get(src);
                    if (up != null) {
                        up.freeSlot();
                    }

                    Pair<Download.DataRequest, TimeoutId> req = pendingDataTraffic.get(src).remove(resp.id);
                    cancelDownloadReqTimeout(req.getValue0().id, req.getValue1());

                    trigger(resp, myPort);
                }
            };

    private Handler<DownloadDataTimeout> handleDownloadDataTimeout = new Handler<DownloadDataTimeout>() {

        @Override
        public void handle(DownloadDataTimeout timeout) {
            log.trace("{} handle {}", config.getSelf(), timeout);

            Map<UUID, Pair<Download.DataRequest, TimeoutId>> targetDataTraffic = pendingDataTraffic.get(timeout.target);
            if(targetDataTraffic == null) {
                log.debug("{} timeout:{} for req:{} from:{} - possibly late", new Object[]{config.getSelf(), timeout.getTimeoutId(), timeout.reqId, timeout.target});
                return;
            }
            Pair<Download.DataRequest, TimeoutId> req = targetDataTraffic.get(timeout.reqId);
            if(req == null) {
                log.debug("{} timeout:{} for req:{} from:{} - possibly late", new Object[]{config.getSelf(), timeout.getTimeoutId(), timeout.reqId, timeout.target});
                return;
            }
            uploadersConn.get(timeout.target).freeSlot();
            trigger(req.getValue0().timeout(), myPort);
            
            //cleaning
            targetDataTraffic.remove(timeout.reqId);
            if(targetDataTraffic.isEmpty()) {
                pendingDataTraffic.remove(timeout.target);
            }
        }
    };
    
    private Handler<DownloadHashTimeout> handleDownloadHashTimeout = new Handler<DownloadHashTimeout>() {

        @Override
        public void handle(DownloadHashTimeout timeout) {
            log.trace("{} handle {}", config.getSelf(), timeout);

            Map<UUID, Pair<Download.HashRequest, TimeoutId>> targetHashTraffic = pendingHashTraffic.get(timeout.target);
            if(targetHashTraffic == null) {
                log.debug("{} timeout:{} for req:{} from:{} - possibly late", new Object[]{config.getSelf(), timeout.getTimeoutId(), timeout.reqId, timeout.target});
                return;
            }
            Pair<Download.HashRequest, TimeoutId> req = targetHashTraffic.get(timeout.reqId);
            if(req == null) {
                log.debug("{} timeout:{} for req:{} from:{} - possibly late", new Object[]{config.getSelf(), timeout.getTimeoutId(), timeout.reqId, timeout.target});
                return;
            }
            uploadersConn.get(timeout.target).freeSlot();
            trigger(req.getValue0().timeout(), myPort);
            
            //cleaning
            targetHashTraffic.remove(timeout.reqId);
            if(targetHashTraffic.isEmpty()) {
                pendingDataTraffic.remove(timeout.target);
            }
        }
    };

    private TimeoutId scheduleConnectionUpdate() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.updatePeriod, config.updatePeriod);
        Timeout t = new ScheduleConnUpdate(spt);
        spt.setTimeoutEvent(t);
        trigger(spt, timer);
        return t.getTimeoutId();
    }

    private TimeoutId scheduleDownloadRequestTimeout(VodAddress target, UUID reqId) {
        ScheduleTimeout st = new ScheduleTimeout(config.reqTimeoutPeriod);
        Timeout t = new DownloadDataTimeout(st, target, reqId);
        st.setTimeoutEvent(t);
        trigger(st, timer);
        log.trace("{} schedule req:{} timeout:{}", new Object[]{config.getSelf(), reqId, t.getTimeoutId()});
        return t.getTimeoutId();
    }

    private void cancelDownloadReqTimeout(UUID reqId, TimeoutId tid) {
        log.trace("{} canceling timeout:{} for req:{}", new Object[]{config.getSelf(), tid, reqId});
        CancelTimeout ct = new CancelTimeout(tid);
        trigger(ct, timer);
    }

    public static class ConnMngrInit extends Init<ConnMngrComp> {

        public final ConnMngrConfig config;

        public ConnMngrInit(ConnMngrConfig config) {
            this.config = config;
        }
    }
}
