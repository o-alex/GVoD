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
package se.sics.gvod.core.connMngr;

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
import se.sics.gvod.croupierfake.CroupierPort;
import se.sics.gvod.croupierfake.CroupierSample;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.common.util.VodDescriptor;
import se.sics.gvod.common.msg.vod.Connection;
import se.sics.gvod.common.msg.vod.Download;
import se.sics.gvod.common.utility.UtilityUpdate;
import se.sics.gvod.common.utility.UtilityUpdatePort;
import se.sics.gvod.network.netmsg.vod.NetConnection;
import se.sics.gvod.network.netmsg.vod.NetDownload;
import se.sics.gvod.core.connMngr.msg.DownloadDataTimeout;
import se.sics.gvod.core.connMngr.msg.DownloadHashTimeout;
import se.sics.gvod.core.connMngr.msg.Ready;
import se.sics.gvod.core.connMngr.msg.ScheduleConnUpdate;
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

    private LocalVodDescriptor selfDesc;

    private Map<VodAddress, DownloaderVodDescriptor> downloadersConn;
    private Map<VodAddress, VodDescriptor> pendingUploadersConn;
    private Map<VodAddress, UploaderVodDescriptor> uploadersConn;

    private Map<VodAddress, Map<UUID, Pair<Download.DataRequest, TimeoutId>>> pendingDownloadingData; //<source, <reqId, <req, timeoutId>>>
    private Map<VodAddress, Map<UUID, Pair<Download.HashRequest, TimeoutId>>> pendingDownloadingHash;
    private Map<Integer, Set<VodAddress>> pendingUploadingData;
    private Map<Integer, Set<VodAddress>> pendingUploadingHash;

    private TimeoutId connUpdateTId;

    private boolean ready;

    public ConnMngrComp(ConnMngrInit init) {
        this.config = init.config;
        log.info("{} starting ...", config.getSelf());

        this.selfDesc = null;
        this.downloadersConn = new HashMap<VodAddress, DownloaderVodDescriptor>();
        this.pendingUploadersConn = new HashMap<VodAddress, VodDescriptor>();
        this.uploadersConn = new HashMap<VodAddress, UploaderVodDescriptor>();
        this.pendingDownloadingData = new HashMap<VodAddress, Map<UUID, Pair<Download.DataRequest, TimeoutId>>>();
        this.pendingDownloadingHash = new HashMap<VodAddress, Map<UUID, Pair<Download.HashRequest, TimeoutId>>>();
        this.pendingUploadingData = new HashMap<Integer, Set<VodAddress>>();
        this.pendingUploadingHash = new HashMap<Integer, Set<VodAddress>>();
        this.ready = false;

        subscribe(handleUpdateUtility, utilityUpdate);
    }

    private Handler<UtilityUpdate> handleUpdateUtility = new Handler<UtilityUpdate>() {

        @Override
        public void handle(UtilityUpdate event) {
            log.info("{} updating self descriptor", config.getSelf(), selfDesc);

            if (selfDesc == null) {
                selfDesc = new LocalVodDescriptor(new VodDescriptor(event.downloadPos), event.downloading);
                start();
            } else {

                if (selfDesc.downloading && !event.downloading) {
                    log.debug("{} completed - closing download connections", config.getSelf());
                    for (VodAddress partner : uploadersConn.keySet()) {
                        Connection.Close cl = new Connection.Close(UUID.randomUUID());
                        trigger(new NetConnection.Close(config.getSelf(), partner, cl.id, config.overlayId, cl), network);
                    }
                    for (VodAddress partner : pendingUploadersConn.keySet()) {
                        Connection.Close cl = new Connection.Close(UUID.randomUUID());
                        trigger(new NetConnection.Close(config.getSelf(), partner, cl.id, config.overlayId, cl), network);
                    }
                    uploadersConn = new HashMap<VodAddress, UploaderVodDescriptor>();
                    pendingUploadersConn = new HashMap<VodAddress, VodDescriptor>();

                    log.debug("{} cleaning timeouts", config.getSelf());
                    for (Map<UUID, Pair<Download.DataRequest, TimeoutId>> partnerTIds : pendingDownloadingData.values()) {
                        for (Pair<Download.DataRequest, TimeoutId> downReq : partnerTIds.values()) {
                            CancelTimeout ct = new CancelTimeout(downReq.getValue1());
                            trigger(ct, timer);
                        }
                    }

                    CancelTimeout ct = new CancelTimeout(connUpdateTId);
                    trigger(ct, timer);

                    pendingDownloadingData = null;
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

        subscribe(handleScheduledConnectionUpdate, timer);
        subscribe(handleCroupierSample, croupier);

        subscribe(handleConnectionRequest, network);
        subscribe(handleConnectionResponse, network);
        subscribe(handleConnectionUpdate, network);
        subscribe(handleConnectionClose, network);

        subscribe(handleLocalHashRequest, myPort);
        subscribe(handleLocalHashResponse, myPort);
        subscribe(handleLocalDataRequest, myPort);
        subscribe(handleLocalDataResponse, myPort);
        subscribe(handleDownloadDataTimeout, timer);
        subscribe(handleDownloadHashTimeout, timer);

        subscribe(handleNetHashRequest, network);
        subscribe(handleNetHashResponse, network);
        subscribe(handleNetDataRequest, network);
        subscribe(handleNetDataResponse, network);

    }

    // CONNECTION MANAGEMENT
    private Handler<CroupierSample> handleCroupierSample = new Handler<CroupierSample>() {

        @Override
        public void handle(CroupierSample event) {
            log.info("{} internal state check downloadersConn:{} pendingUploadersConn:{} uploadersConn:{} pendingDownData:{} pendinfDownHash:{} pendingUpData:{} pendingUpHash", 
                    new Object[]{config.getSelf(), downloadersConn.size(), pendingUploadersConn.size(), uploadersConn.size(), pendingDownloadingData.size(), 
                        pendingDownloadingHash.size(), pendingUploadingData.size(), pendingUploadingHash.size()});
            
            log.debug("{} handle new samples {}", config.getSelf(), event.sample);

            for (Map.Entry<VodAddress, VodDescriptor> e : event.sample.entrySet()) {
                if (e.getValue().downloadPos < selfDesc.vodDesc.downloadPos) {
                    continue;
                }
                if (uploadersConn.containsKey(e.getKey()) || pendingUploadersConn.containsKey(e.getKey())) {
                    continue;
                }
                log.info("{} opening connection to {}", config.getSelf(), e.getKey());
                pendingUploadersConn.put(e.getKey(), e.getValue());
                Connection.Request req = new Connection.Request(UUID.randomUUID(), selfDesc.vodDesc);
                trigger(new NetConnection.Request(config.getSelf(), e.getKey(), req.id, config.overlayId, req), network);
            }
        }
    };

    private Handler<ScheduleConnUpdate> handleScheduledConnectionUpdate = new Handler<ScheduleConnUpdate>() {

        @Override
        public void handle(ScheduleConnUpdate event) {
            log.trace("{} handle {}", config.getSelf(), event);
            for (VodAddress partner : downloadersConn.keySet()) {
                Connection.Update upd = new Connection.Update(UUID.randomUUID(), selfDesc.vodDesc);
                trigger(new NetConnection.Update(config.getSelf(), partner, upd.id, config.overlayId, upd), network);
            }

            for (VodAddress partner : uploadersConn.keySet()) {
                Connection.Update upd = new Connection.Update(UUID.randomUUID(), selfDesc.vodDesc);
                trigger(new NetConnection.Update(config.getSelf(), partner, upd.id, config.overlayId, upd), network);
            }
        }
    };

    private Handler<NetConnection.Request> handleConnectionRequest = new Handler<NetConnection.Request>() {

        @Override
        public void handle(NetConnection.Request req) {
            log.trace("{} handle {}", new Object[]{config.getSelf(), req});

            if (downloadersConn.containsKey(req.getVodSource())) {
                return;
            }

            log.debug("{} new connection to downloader {}", config.getSelf(), req.getVodSource());
            downloadersConn.put(req.getVodSource(), new DownloaderVodDescriptor(req.content.desc, config.defaultMaxPipeline));
            Connection.Response resp = req.content.accept();
            trigger(new NetConnection.Response(config.getSelf(), req.getVodSource(), UUID.randomUUID(), config.overlayId, resp), network);
        }
    };

    private Handler<NetConnection.Response> handleConnectionResponse = new Handler<NetConnection.Response>() {

        @Override
        public void handle(NetConnection.Response resp) {
            log.trace("{} handle {}", new Object[]{config.getSelf(), resp.getVodSource()});

            if (!resp.content.status.equals(ReqStatus.SUCCESS)) {
                log.debug("{} connection req status {}", config.getSelf(), resp.content.status);
                pendingUploadersConn.remove(resp.getVodSource());
                return;
            }

            if (!pendingUploadersConn.containsKey(resp.getVodSource())) {
                log.info("{} closing connection to {}", config.getSelf(), resp.getVodSource());
                Connection.Close close = new Connection.Close(UUID.randomUUID());
                trigger(new NetConnection.Close(config.getSelf(), resp.getVodSource(), UUID.randomUUID(), config.overlayId, close), network);
                return;
            }

            log.debug("{} new connection to uploader {}", config.getSelf(), resp.getVodSource());
            uploadersConn.put(resp.getVodSource(), new UploaderVodDescriptor(pendingUploadersConn.remove(resp.getVodSource()), config.defaultMaxPipeline));

            if (!ready) {
                log.debug("{} first uploader connection, can start serving", config.getSelf());
                ready = true;
                trigger(new Ready(UUID.randomUUID()), myPort);
            }
        }
    };

    private Handler<NetConnection.Update> handleConnectionUpdate = new Handler<NetConnection.Update>() {

                @Override
                public void handle(NetConnection.Update event) {
                    log.trace("{} handle {}", new Object[]{config.getSelf(), event.getVodSource()});

                    if (downloadersConn.containsKey(event.getVodSource())) {
                        downloadersConn.get(event.getVodSource()).updateDesc(event.content.desc);
                    }

                    if (uploadersConn.containsKey(event.getVodSource())) {
                        uploadersConn.get(event.getVodSource()).updateDesc(event.content.desc);
                    }
                }
            };

    private Handler<NetConnection.Close> handleConnectionClose = new Handler<NetConnection.Close>() {

                @Override
                public void handle(NetConnection.Close event) {
                    log.debug("{} handle {}", new Object[]{config.getSelf(), event});
                    downloadersConn.remove(event.getVodSource());
                    pendingUploadersConn.remove(event.getVodSource());
                    uploadersConn.remove(event.getVodSource());
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

            Map<UUID, Pair<Download.DataRequest, TimeoutId>> partnerReq = pendingDownloadingData.get(uploader.getKey());
            if (partnerReq == null) {
                partnerReq = new HashMap<UUID, Pair<Download.DataRequest, TimeoutId>>();
                pendingDownloadingData.put(uploader.getKey(), partnerReq);
            }
            partnerReq.put(req.id, Pair.with(req, scheduleDownloadRequestTimeout(uploader.getKey(), req.id)));
            trigger(new NetDownload.DataRequest(config.getSelf(), uploader.getKey(), req.id, config.overlayId, req), network);
        }

    };

    private Handler<NetDownload.DataRequest> handleNetDataRequest = new Handler<NetDownload.DataRequest>() {

                @Override
                public void handle(NetDownload.DataRequest req) {
                    log.trace("{} handling network {}", new Object[]{config.getSelf(), req});
                    log.debug("{} requested unique pieces {}", pendingUploadingData.size());

                    DownloaderVodDescriptor downDesc = downloadersConn.get(req.getVodSource());
                    if (downDesc == null) {
                        log.debug("{} no connection open, dropping req {} from {}", new Object[]{config.getSelf(), req, req.getVodSource()});
                        return;
                    }
                    if (!downDesc.isViable()) {
                        //TODO should not happen yet, but will need to be treated later, when downloader has no more slots available
                        log.info("{} no more slots for peer:{}", config.getSelf(), req.getVodSource());
                        return;
                    }
                    downDesc.useSlot();

                    Set<VodAddress> requesters = pendingUploadingData.get(req.content.pieceId);
                    if (requesters == null) {
                        requesters = new HashSet<VodAddress>();
                        pendingUploadingData.put(req.content.pieceId, requesters);
                        trigger(req.content, myPort);
                    }
                    requesters.add(req.getVodSource());
                }
            };

    private Handler<Download.DataResponse> handleLocalDataResponse = new Handler<Download.DataResponse>() {

        @Override
        public void handle(Download.DataResponse resp) {
            log.trace("{} handle local {}", config.getSelf(), resp);

            Set<VodAddress> requesters = pendingUploadingData.get(resp.pieceId);
            if (requesters == null) {
                log.debug("{} no requesters for piece {}", config.getSelf(), resp.pieceId);
                return;
            }

            for (VodAddress src : requesters) {
                DownloaderVodDescriptor down = downloadersConn.get(src);
                if (down != null) {
                    log.debug("{} sending piece {} to {}", new Object[]{config.getSelf(), resp.pieceId, src});
                    trigger(new NetDownload.DataResponse(config.getSelf(), src, resp.id, config.overlayId, resp), network);
                    down.freeSlot();
                }
            }
            pendingUploadingData.remove(resp.pieceId);
        }
    };

    private Handler<NetDownload.DataResponse> handleNetDataResponse = new Handler<NetDownload.DataResponse>() {

                @Override
                public void handle(NetDownload.DataResponse resp) {
                    log.trace("{} handle net {}", new Object[]{config.getSelf(), resp});
                    UploaderVodDescriptor up = uploadersConn.get(resp.getVodSource());
                    if (up != null) {
                        up.freeSlot();
                    }

                    Map<UUID, Pair<Download.DataRequest, TimeoutId>> aux = pendingDownloadingData.get(resp.getVodSource());
                    if(aux == null) {
                        log.debug("{} data posibly late", config.getSelf());
                        //TODO Alex fix this;
                        return;
                    }
                    Pair<Download.DataRequest, TimeoutId> req = aux.remove(resp.content.id);
                    if(req == null) {
                        log.debug("{} data posibly late", config.getSelf());
                        //TODO Alex fix this;
                        return;
                    }
                    cancelDownloadReqTimeout(req.getValue0().id, req.getValue1());
                    trigger(resp.content, myPort);
                }
            };

    private Handler<Download.HashRequest> handleLocalHashRequest = new Handler<Download.HashRequest>() {

        @Override
        public void handle(Download.HashRequest req) {
            log.trace("{} handle local {}", config.getSelf(), req);

            Map.Entry<VodAddress, UploaderVodDescriptor> uploader = getUploader(req.targetPos);
            if (uploader == null) {
                log.debug("{} no candidate for position {}", new Object[]{config.getSelf(), req.targetPos});
                trigger(req.busy(), myPort);
                return;
            }
            uploader.getValue().useSlot();

            Map<UUID, Pair<Download.HashRequest, TimeoutId>> partnerReq = pendingDownloadingHash.get(uploader.getKey());
            if (partnerReq == null) {
                partnerReq = new HashMap<UUID, Pair<Download.HashRequest, TimeoutId>>();
                pendingDownloadingHash.put(uploader.getKey(), partnerReq);
            }
            partnerReq.put(req.id, Pair.with(req, scheduleDownloadRequestTimeout(uploader.getKey(), req.id)));
            trigger(new NetDownload.HashRequest(config.getSelf(), uploader.getKey(), req.id, config.overlayId, req), network);
        }

    };

    private Handler<NetDownload.HashRequest> handleNetHashRequest = new Handler<NetDownload.HashRequest>() {

                @Override
                public void handle(NetDownload.HashRequest req) {
                    log.trace("{} handling network {}", new Object[]{config.getSelf(), req});

                    DownloaderVodDescriptor downDesc = downloadersConn.get(req.getVodSource());
                    if (downDesc == null) {
                        log.debug("{} no connection open, dropping req {} from {}", new Object[]{config.getSelf(), req, req.getVodSource()});
                        return;
                    }
                    if (!downDesc.isViable()) {
                        //TODO should not happen yet, but will need to be treated later, when downloader has no more slots available
                        log.info("{} no more slots for peer:{}", config.getSelf(), req.getVodSource());
                        return;
                    }
                    downDesc.useSlot();

                    Set<VodAddress> requesters = pendingUploadingHash.get(req.content.targetPos);
                    if (requesters == null) {
                        requesters = new HashSet<VodAddress>();
                        pendingUploadingHash.put(req.content.targetPos, requesters);
                        trigger(req.content, myPort);
                    }
                    requesters.add(req.getVodSource());
                }
            };

    private Handler<Download.HashResponse> handleLocalHashResponse = new Handler<Download.HashResponse>() {

        @Override
        public void handle(Download.HashResponse resp) {
            log.trace("{} handle local {}", config.getSelf(), resp);

            Set<VodAddress> requesters = pendingUploadingHash.get(resp.targetPos);
            if (requesters == null) {
                log.debug("{} no requesters for piece {}", config.getSelf(), resp.targetPos);
                return;
            }

            for (VodAddress src : requesters) {
                DownloaderVodDescriptor down = downloadersConn.get(src);
                if (down != null) {
                    log.debug("{} sending hash set {} to {}", new Object[]{config.getSelf(), resp.targetPos, src});
                    trigger(new NetDownload.HashResponse(config.getSelf(), src, resp.id, config.overlayId, resp), network);
                    down.freeSlot();
                }
            }
            pendingUploadingHash.remove(resp.targetPos);
        }
    };

    private Handler<NetDownload.HashResponse> handleNetHashResponse = new Handler<NetDownload.HashResponse>() {

                @Override
                public void handle(NetDownload.HashResponse resp) {
                    log.debug("{} handle net {} {}", new Object[]{config.getSelf(), resp, resp.content.status});
                    UploaderVodDescriptor up = uploadersConn.get(resp.getVodSource());
                    if (up != null) {
                        up.freeSlot();
                    }

                    Pair<Download.HashRequest, TimeoutId> req = pendingDownloadingHash.get(resp.getVodSource()).remove(resp.content.id);
                    cancelDownloadReqTimeout(req.getValue0().id, req.getValue1());

                    trigger(resp.content, myPort);
                }
            };

    private Handler<DownloadDataTimeout> handleDownloadDataTimeout = new Handler<DownloadDataTimeout>() {

        @Override
        public void handle(DownloadDataTimeout timeout) {
            log.trace("{} handle {}", config.getSelf(), timeout);

            Map<UUID, Pair<Download.DataRequest, TimeoutId>> targetDataTraffic = pendingDownloadingData.get(timeout.target);
            if (targetDataTraffic == null) {
                log.debug("{} timeout:{} for req:{} from:{} - possibly late", new Object[]{config.getSelf(), timeout.getTimeoutId(), timeout.reqId, timeout.target});
                return;
            }
            Pair<Download.DataRequest, TimeoutId> req = targetDataTraffic.get(timeout.reqId);
            if (req == null) {
                log.debug("{} timeout:{} for req:{} from:{} - possibly late", new Object[]{config.getSelf(), timeout.getTimeoutId(), timeout.reqId, timeout.target});
                return;
            }
            uploadersConn.get(timeout.target).freeSlot();
            trigger(req.getValue0().timeout(), myPort);

            //cleaning
            targetDataTraffic.remove(timeout.reqId);
            if (targetDataTraffic.isEmpty()) {
                pendingDownloadingData.remove(timeout.target);
            }
        }
    };

    private Handler<DownloadHashTimeout> handleDownloadHashTimeout = new Handler<DownloadHashTimeout>() {

        @Override
        public void handle(DownloadHashTimeout timeout) {
            log.trace("{} handle {}", config.getSelf(), timeout);

            Map<UUID, Pair<Download.HashRequest, TimeoutId>> targetHashTraffic = pendingDownloadingHash.get(timeout.target);
            if (targetHashTraffic == null) {
                log.debug("{} timeout:{} for req:{} from:{} - possibly late", new Object[]{config.getSelf(), timeout.getTimeoutId(), timeout.reqId, timeout.target});
                return;
            }
            Pair<Download.HashRequest, TimeoutId> req = targetHashTraffic.get(timeout.reqId);
            if (req == null) {
                log.debug("{} timeout:{} for req:{} from:{} - possibly late", new Object[]{config.getSelf(), timeout.getTimeoutId(), timeout.reqId, timeout.target});
                return;
            }
            uploadersConn.get(timeout.target).freeSlot();
            trigger(req.getValue0().timeout(), myPort);

            //cleaning
            targetHashTraffic.remove(timeout.reqId);
            if (targetHashTraffic.isEmpty()) {
                pendingDownloadingHash.remove(timeout.target);
            }
        }
    };

    private Map.Entry<VodAddress, UploaderVodDescriptor> getUploader(int pieceId) {
        Iterator<Map.Entry<VodAddress, UploaderVodDescriptor>> it = uploadersConn.entrySet().iterator();
        Map.Entry<VodAddress, UploaderVodDescriptor> candidate = null;
        //get first viable candidate
        while (it.hasNext()) {
            candidate = it.next();
            int blockPos = pieceId / config.piecesPerBlock;
            if (candidate.getValue().isViable(blockPos)) {
                break;
            }
            candidate = null;
        }
        //get best candidate
        while (it.hasNext()) {
            Map.Entry<VodAddress, UploaderVodDescriptor> nextC = it.next();
            int blockPos = pieceId / config.piecesPerBlock;
            if (nextC.getValue().betterCandidate(candidate.getValue(), blockPos)) {
                candidate = nextC;
            }
        }

        return candidate;
    }

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
