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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.util.MsgProcessor;
import se.sics.gvod.croupierfake.CroupierPort;
import se.sics.gvod.croupierfake.CroupierSample;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.common.util.VodDescriptor;
import se.sics.gvod.network.nettymsg.MyNetMsg;
import se.sics.gvod.network.tags.ContextTag;
import se.sics.gvod.network.tags.OverlayTag;
import se.sics.gvod.network.tags.Tag;
import se.sics.gvod.network.tags.TagType;
import se.sics.gvod.system.connMngr.msg.Connection;
import se.sics.gvod.system.connMngr.msg.Ready;
import se.sics.gvod.system.downloadMngr.msg.UpdateSelf;
import se.sics.gvod.system.downloadMngr.msg.Download;
import se.sics.gvod.system.downloadMngr.msg.DownloadControl;
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

    private final ConnMngrConfig config;
    private final MsgProcessor msgProc;

    private LocalVodDescriptor selfDesc;

    private Map<VodAddress, DownloaderVodDescriptor> downloadersConn;
    private Map<VodAddress, VodDescriptor> pendingUploadersConn;
    private Map<VodAddress, UploaderVodDescriptor> uploadersConn;

    private Map<VodAddress, Map<Integer, TimeoutId>> pendingDownloads;
    private Map<Integer, Set<VodAddress>> pendingUploadReq;

    private TimeoutId connUpdateTId;
    private Map<TagType, Tag> tags;

    private boolean ready;

    public ConnMngrComp(ConnMngrInit init) {
        this.config = init.config;
        log.info("{} creating...", config.getSelf());

        this.selfDesc = null;
        this.msgProc = new MsgProcessor();
        this.downloadersConn = new HashMap<VodAddress, DownloaderVodDescriptor>();
        this.pendingUploadersConn = new HashMap<VodAddress, VodDescriptor>();
        this.uploadersConn = new HashMap<VodAddress, UploaderVodDescriptor>();
        this.pendingDownloads = new HashMap<VodAddress, Map<Integer, TimeoutId>>();
        this.pendingUploadReq = new HashMap<Integer, Set<VodAddress>>();
        this.tags = new HashMap<TagType, Tag>();
        this.tags.put(TagType.OVERLAY, new OverlayTag(config.overlayId));
        this.tags.put(TagType.CONTEXT, ContextTag.VIDEO);
        this.ready = false;

        subscribe(handleUpdateSelf, myPort);
    }

    private Handler<UpdateSelf> handleUpdateSelf = new Handler<UpdateSelf>() {

        @Override
        public void handle(UpdateSelf event) {
            log.trace("{} updating self descriptor", config.getSelf(), selfDesc);

            if (selfDesc == null) {
                selfDesc = event.selfDesc;
                start();
            } else {

                if (selfDesc.downloading && !event.selfDesc.downloading) {
                    log.debug("{} completed - closing download connections", config.getSelf());
                    for (VodAddress partner : uploadersConn.keySet()) {
                        Connection.Close cl = new Connection.Close(UUID.randomUUID());
                        trigger(new MyNetMsg.OneWay(config.getSelf(), partner, tags, cl), network);
                    }
                    for (VodAddress partner : pendingUploadersConn.keySet()) {
                        Connection.Close cl = new Connection.Close(UUID.randomUUID());
                        trigger(new MyNetMsg.OneWay(config.getSelf(), partner, tags, cl), network);
                    }
                    uploadersConn = new HashMap<VodAddress, UploaderVodDescriptor>();
                    pendingUploadersConn = new HashMap<VodAddress, VodDescriptor>();

                    log.debug("{} cleaning timeouts", config.getSelf());
                    for (Map<Integer, TimeoutId> partnerTIds : pendingDownloads.values()) {
                        for (TimeoutId tId : partnerTIds.values()) {
                            CancelTimeout ct = new CancelTimeout(tId);
                            trigger(ct, timer);
                        }
                    }

                    CancelTimeout ct = new CancelTimeout(connUpdateTId);
                    trigger(ct, timer);

                    pendingDownloads = null;
                }
                selfDesc = event.selfDesc;
            }
        }
    };

    private void start() {

        log.debug("{} starting...", config.getSelf());
        if (selfDesc.downloading) {
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.updatePeriod, config.updatePeriod);
            Timeout t = new Connection.UpdateTimeout(spt);
            connUpdateTId = t.getTimeoutId();
            spt.setTimeoutEvent(t);
            trigger(spt, timer);
        } else {
            ready = true;
            trigger(new Ready(UUID.randomUUID()), myPort);
        }

        subscribe(handleNetRequest, network);
        subscribe(handleNetResponse, network);
        subscribe(handleNetOneWay, network);

        subscribe(handleConnectionUpdateTimeout, timer);
        subscribe(handleCroupierSample, croupier);

        msgProc.subscribe(handleConnectionRequest);
        msgProc.subscribe(handleConnectionResponse);
        msgProc.subscribe(handleConnectionUpdate);
        msgProc.subscribe(handleConnectionClose);

        subscribe(handleLocalDownloadRequest, myPort);
        subscribe(handleLocalDownloadResponse, myPort);
        subscribe(handleDownloadTimeout, timer);

        msgProc.subscribe(handleNetDownloadRequest);
        msgProc.subscribe(handleNetDownloadResponse);

    }

    private Handler<MyNetMsg.OneWay> handleNetOneWay = new Handler<MyNetMsg.OneWay>() {

        @Override
        public void handle(MyNetMsg.OneWay netReq) {
            log.trace("{} received {}", config.getSelf(), netReq.toString());
            msgProc.trigger(netReq.getVodSource(), netReq.payload);
        }
    };

    private Handler<MyNetMsg.Request> handleNetRequest = new Handler<MyNetMsg.Request>() {

        @Override
        public void handle(MyNetMsg.Request netReq) {
            log.trace("{} received {}", config.getSelf(), netReq.toString());
            msgProc.trigger(netReq.getVodSource(), netReq.payload);
        }
    };

    private Handler<MyNetMsg.Response> handleNetResponse = new Handler<MyNetMsg.Response>() {

        @Override
        public void handle(MyNetMsg.Response netResp) {
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
                trigger(new MyNetMsg.Request(config.getSelf(), e.getKey(), tags, req), network);
            }
        }
    };

    private Handler<Connection.UpdateTimeout> handleConnectionUpdateTimeout = new Handler<Connection.UpdateTimeout>() {

        @Override
        public void handle(Connection.UpdateTimeout event) {
            log.trace("{} handle {}", config.getSelf(), event);
            for (VodAddress partner : downloadersConn.keySet()) {
                Connection.Update upd = new Connection.Update(UUID.randomUUID(), selfDesc.vodDesc);
                trigger(new MyNetMsg.OneWay(config.getSelf(), partner, tags, upd), network);
            }

            for (VodAddress partner : uploadersConn.keySet()) {
                Connection.Update upd = new Connection.Update(UUID.randomUUID(), selfDesc.vodDesc);
                trigger(new MyNetMsg.OneWay(config.getSelf(), partner, tags, upd), network);
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
                    trigger(new MyNetMsg.Response(config.getSelf(), src, tags, resp), network);
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
                        trigger(new MyNetMsg.OneWay(config.getSelf(), src, tags, new Connection.Close(UUID.randomUUID())), network);
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
    private Handler<Download.Request> handleLocalDownloadRequest = new Handler<Download.Request>() {

        @Override
        public void handle(Download.Request req) {
            log.trace("{} handle local {}", config.getSelf(), req);

            Map.Entry<VodAddress, UploaderVodDescriptor> uploader = getUploader(req.pieceId);
            if (uploader == null) {
                log.debug("{} no candidate for piece {}", new Object[]{config.getSelf(), req.pieceId});
                trigger(new DownloadControl.SlowDown(req.id, req.pieceId), myPort);
                return;
            }
            uploader.getValue().useSlot();

            ScheduleTimeout st = new ScheduleTimeout(config.reqTimeoutPeriod);
            Timeout t = new Download.ReqTimeout(st, req.pieceId);
            st.setTimeoutEvent(t);
            trigger(st, timer);

            Map<Integer, TimeoutId> partnerReq = pendingDownloads.get(uploader.getKey());
            if (partnerReq == null) {
                partnerReq = new HashMap<Integer, TimeoutId>();
                pendingDownloads.put(uploader.getKey(), partnerReq);
            }
            partnerReq.put(req.pieceId, t.getTimeoutId());
            trigger(new MyNetMsg.Request(config.getSelf(), uploader.getKey(), tags, req), network);
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

    private MsgProcessor.Handler<Download.Request> handleNetDownloadRequest
            = new MsgProcessor.Handler<Download.Request>(Download.Request.class) {

                @Override
                public void handle(VodAddress src, Download.Request req) {
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

    private Handler<Download.Response> handleLocalDownloadResponse = new Handler<Download.Response>() {

        @Override
        public void handle(Download.Response resp) {
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
                    trigger(new MyNetMsg.Response(config.getSelf(), src, tags, resp), network);
                    down.freeSlot();
                }
            }
            pendingUploadReq.remove(resp.pieceId);
        }
    };

    private MsgProcessor.Handler<Download.Response> handleNetDownloadResponse
            = new MsgProcessor.Handler<Download.Response>(Download.Response.class) {

                @Override
                public void handle(VodAddress src, Download.Response resp) {
                    log.trace("{} handle net {}", new Object[]{config.getSelf(), resp});
                    UploaderVodDescriptor up = uploadersConn.get(src);
                    if (up != null) {
                        up.freeSlot();
                    }
                    
                    TimeoutId tid = pendingDownloads.get(src).remove(resp.pieceId);
                    cancelDownloadReqTimeout(tid);
                    
                    trigger(resp, myPort);
                }
            };

    private Handler<Download.ReqTimeout> handleDownloadTimeout = new Handler<Download.ReqTimeout>() {

        @Override
        public void handle(Download.ReqTimeout event) {
            log.trace("{} handle {}", config.getSelf(), event);

            boolean found = false;
            Iterator<Map.Entry<VodAddress, Map<Integer, TimeoutId>>> partnerIt = pendingDownloads.entrySet().iterator();
            while (partnerIt.hasNext()) {
                Map.Entry<VodAddress, Map<Integer, TimeoutId>> partner = partnerIt.next();
                Iterator<Map.Entry<Integer, TimeoutId>> pieceIt = partner.getValue().entrySet().iterator();
                while (pieceIt.hasNext()) {
                    Map.Entry<Integer, TimeoutId> piece = pieceIt.next();
                    if (piece.getValue().equals(event.getTimeoutId())) {
                        found = true;
                        pieceIt.remove();
                        uploadersConn.get(partner.getKey()).freeSlot();
                        break;
                    }
                }
                if (found) {
                    if (partner.getValue().isEmpty()) {
                        partnerIt.remove();
                        break;
                    }
                    trigger(event, myPort);
                } else {
                    log.warn("{} uncanceled timeout", config.getSelf());
                }
            }
        }
    };

    private void cancelDownloadReqTimeout(TimeoutId tid) {
        log.debug("{} canceling download req timeout {}", config.getSelf(), tid);
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
