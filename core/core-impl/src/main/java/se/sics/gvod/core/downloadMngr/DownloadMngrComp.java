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
package se.sics.gvod.core.downloadMngr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.core.connMngr.ConnMngrPort;
import se.sics.gvod.core.connMngr.msg.Ready;
import se.sics.gvod.common.msg.vod.Download;
import se.sics.gvod.common.util.HashUtil;
import se.sics.gvod.common.utility.UtilityUpdate;
import se.sics.gvod.common.utility.UtilityUpdatePort;
import se.sics.gvod.core.downloadMngr.msg.ScheduledSpeedUp;
import se.sics.gvod.core.downloadMngr.msg.ScheduledUtilityUpdate;
import se.sics.gvod.core.storage.FileMngr;
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
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DownloadMngrComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(DownloadMngrComp.class);

    private Negative<UtilityUpdatePort> utilityUpdate = provides(UtilityUpdatePort.class);
    private Negative<DownloadMngrPort> dataPort = provides(DownloadMngrPort.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Positive<ConnMngrPort> connMngr = requires(ConnMngrPort.class);

    private final DownloadMngrConfig config;

    private final FileMngr hashMngr;
    private final FileMngr fileMngr;
    private boolean downloading;

    private Set<Integer> pendingPieces;
    private List<Integer> nextPieces;
    private Set<Integer> pendingHashes;
    private List<Integer> nextHashes;

    private TimeoutId speedUpTId = null;
    private TimeoutId updateSelfTId;

    public DownloadMngrComp(DownloadMngrInit init) {
        this.config = init.config;
        log.info("{} video component init", config.getSelf());

        this.hashMngr = init.hashMngr;
        this.fileMngr = init.fileMngr;
        this.downloading = init.downloader;

        this.pendingPieces = new HashSet<Integer>();
        this.nextPieces = new ArrayList<Integer>();
        this.pendingHashes = new HashSet<Integer>();
        this.nextHashes = new ArrayList<Integer>();

        subscribe(handleStart, control);
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.trace("{} starting...", config.getSelf());

            int downloadPos = fileMngr.contiguousStart();
            int hashPos = hashMngr.contiguousStart();
            log.info("{} video pos:{}, hash pos:{}", new Object[]{config.getSelf(), downloadPos, hashPos});
            trigger(new UtilityUpdate(config.overlayId, downloading, downloadPos), utilityUpdate);

            subscribe(handleConnReady, connMngr);
            subscribe(handleDataRequest, dataPort);
        }
    };

    private Handler<Data.DRequest> handleDataRequest = new Handler<Data.DRequest>() {

        @Override
        public void handle(Data.DRequest req) {
            log.debug("{} received local data request", config.getSelf());
            
        }
        
    };

    private Handler<Ready> handleConnReady = new Handler<Ready>() {
        @Override
        public void handle(Ready event) {
            if (downloading) {
                log.info("{} starting video download", config.getSelf());
                for (int i = 0; i < config.startPieces; i++) {
                    download();
                }
                scheduleSpeedUp(1);
                subscribe(handleDownloadSpeedUp, timer);

                scheduleUpdateSelf();
                subscribe(handleUpdateSelf, timer);
            } else {
                log.info("{} seeding file", config.getSelf());
            }

            subscribe(handleHashRequest, connMngr);
            subscribe(handleHashResponse, connMngr);
            subscribe(handleDownloadDataRequest, connMngr);
            subscribe(handleDownloadDataResponse, connMngr);
        }
    };

    private Handler<Download.HashRequest> handleHashRequest = new Handler<Download.HashRequest>() {

        @Override
        public void handle(Download.HashRequest req) {
            log.trace("{} handle {}", config.getSelf(), req);

            Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
            Set<Integer> missingHashes = new HashSet<Integer>();

            for (Integer hash : req.hashes) {
                if (hashMngr.hasPiece(hash)) {
                    hashes.put(hash, hashMngr.readPiece(hash));
                } else {
                    missingHashes.add(hash);
                }
            }
            log.debug("{} sending hashes {}", config.getSelf(), hashes.keySet());
            trigger(req.success(hashes, missingHashes), connMngr);
        }

    };

    private Handler<Download.HashResponse> handleHashResponse = new Handler<Download.HashResponse>() {

        @Override
        public void handle(Download.HashResponse resp) {
            log.trace("{} handle {}", config.getSelf(), resp);

            switch (resp.status) {
                case SUCCESS:
                    log.debug("{} {} {} hashes:{}", new Object[]{config.getSelf(), resp, resp.status, resp.hashes.keySet()});

                    for (Map.Entry<Integer, byte[]> hash : resp.hashes.entrySet()) {
                        hashMngr.writePiece(hash.getKey(), hash.getValue());
                    }

                    pendingHashes.removeAll(resp.hashes.keySet());
                    nextHashes.addAll(0, resp.missingHashes);

                    download();
                    return;
                case TIMEOUT:
                    log.debug("{} {} {} ", new Object[]{config.getSelf(), resp, resp.status});
                    pendingHashes.removeAll(resp.missingHashes);
                    nextHashes.addAll(0, resp.missingHashes);
                    download();
                    return;
                case BUSY:
                    log.debug("{} download slow down");
                    pendingPieces.removeAll(resp.missingHashes);
                    nextPieces.addAll(0, resp.missingHashes);
                    cancelSpeedUp();
                    scheduleSpeedUp(10);
                    return;
                default:
                    log.warn("{} {} illegal status {}", new Object[]{config.getSelf(), resp, resp.status});
            }
        }
    };

    private Handler<Download.DataRequest> handleDownloadDataRequest = new Handler<Download.DataRequest>() {

        @Override
        public void handle(Download.DataRequest req) {
            log.trace("{} handle {}", config.getSelf(), req);

            if (fileMngr.hasPiece(req.pieceId)) {
                byte[] piece = fileMngr.readPiece(req.pieceId);
                log.debug("{} sending piece {}", new Object[]{config.getSelf(), req.pieceId});
                trigger(req.success(piece), connMngr);
            } else {
                log.debug("{} do not have piece {}", new Object[]{config.getSelf(), req.pieceId});
                trigger(req.missingPiece(), connMngr);
            }
        }
    };

    private Handler<Download.DataResponse> handleDownloadDataResponse = new Handler<Download.DataResponse>() {

        @Override
        public void handle(Download.DataResponse resp) {
            log.trace("{} handle {}", config.getSelf(), resp);

            switch (resp.status) {
                case SUCCESS:
                    log.debug("{} {} {} piece:{}", new Object[]{config.getSelf(), resp, resp.status, resp.pieceId});

                    if (hashMngr.hasPiece(resp.pieceId) && HashUtil.checkHash(config.hashAlg, resp.piece, hashMngr.readPiece(resp.pieceId))) {
                        fileMngr.writePiece(resp.pieceId, resp.piece);
                    } else {
                        log.debug("{} piece:{} - hash problem, dropping piece", config.getSelf(), resp.pieceId);
                        nextPieces.add(0, resp.pieceId);
                    }
                    pendingPieces.remove(resp.pieceId);

                    download();
                    return;
                case TIMEOUT:
                case MISSING:
                    log.debug("{} {} {} piece:{}", new Object[]{config.getSelf(), resp, resp.status, resp.pieceId});
                    pendingPieces.remove(resp.pieceId);
                    nextPieces.add(0, resp.pieceId);
                    download();
                    return;
                case BUSY:
                    log.debug("{} download slow down");
                    pendingPieces.remove(resp.pieceId);
                    nextPieces.add(0, resp.pieceId);
                    cancelSpeedUp();
                    scheduleSpeedUp(10);
                    return;
                default:
                    log.warn("{} {} illegal status {}", new Object[]{config.getSelf(), resp, resp.status});
            }

        }
    };

    private Handler<ScheduledSpeedUp> handleDownloadSpeedUp = new Handler<ScheduledSpeedUp>() {

        @Override
        public void handle(ScheduledSpeedUp event) {
            log.trace("{} handling speedup {}", config.getSelf(), event.getTimeoutId());

            if (speedUpTId == null) {
                log.info("{} late timeout {}", config.getSelf(), speedUpTId);
                return;
            }
            download();
            scheduleSpeedUp(1);
        }

    };

    private Handler<ScheduledUtilityUpdate> handleUpdateSelf = new Handler<ScheduledUtilityUpdate>() {

        @Override
        public void handle(ScheduledUtilityUpdate event) {
            log.trace("{} handle {}", config.getSelf(), event);

            int downloadPos = fileMngr.contiguousStart();
            int hashPos = hashMngr.contiguousStart();
            log.info("{} video pos:{} hash pos:{}", new Object[]{config.getSelf(), downloadPos, hashPos});
            trigger(new UtilityUpdate(config.overlayId, downloading, downloadPos), utilityUpdate);
        }
    };

    private boolean download() {
        if (nextHashes.isEmpty() && nextPieces.isEmpty()) {
            if (fileMngr.isComplete()) {
                finishDownload();
                return false;
            } else {
                getNewPieces();
            }
        }

        if (!downloadHash()) {
            if (!downloadData()) {
                return false;
            }
        }
        return true;
    }

    //TODO get a good way to get new pieces
    private void getNewPieces() {
        int filePos = fileMngr.contiguousStart();
        int hashPos = hashMngr.contiguousStart();

        int fileFactor, hashFactor;
        if (filePos + 2 * config.startPieces < hashPos) {
            fileFactor = 1;
            hashFactor = 1;
        } else {
            fileFactor = 1;
            hashFactor = 2;
        }
        int nrNewPieces = fileFactor * config.startPieces + pendingPieces.size();
        Set<Integer> newNextPieces = fileMngr.nextPiecesNeeded(nrNewPieces, 0);
        newNextPieces.removeAll(pendingPieces);
        nextPieces.addAll(newNextPieces);

        int nrNewHashes = hashFactor * config.startPieces + pendingHashes.size();
        Set<Integer> newNextHashes = hashMngr.nextPiecesNeeded(nrNewHashes, 0);
        newNextHashes.removeAll(pendingHashes);
        nextHashes.addAll(newNextHashes);

    }

    private boolean downloadData() {
        if (nextPieces.isEmpty()) {
            return false;
        }
        int nextPieceId = nextPieces.remove(0);
        log.debug("{} downloading piece {}", config.getSelf(), nextPieceId);
        trigger(new Download.DataRequest(UUID.randomUUID(), config.overlayId, nextPieceId), connMngr);
        pendingPieces.add(nextPieceId);
        return true;
    }

    private boolean downloadHash() {
        if (nextHashes.isEmpty()) {
            return false;
        }
        Set<Integer> hashesToDownload = new HashSet<Integer>();
        int targetPos = nextHashes.get(0);
        for (int i = 0; i < config.hashesPerMsg && !nextHashes.isEmpty(); i++) {
            hashesToDownload.add(nextHashes.remove(0));
        }
        log.debug("{} downloading hashes {}", config.getSelf(), hashesToDownload);
        trigger(new Download.HashRequest(UUID.randomUUID(), targetPos, hashesToDownload), connMngr);
        pendingHashes.addAll(hashesToDownload);
        return true;
    }

    private void scheduleSpeedUp(int periodFactor) {
        ScheduleTimeout st = new ScheduleTimeout(periodFactor * config.speedupPeriod);
        Timeout t = new ScheduledSpeedUp(st);
        st.setTimeoutEvent(t);
        speedUpTId = t.getTimeoutId();
        log.debug("{} scheduling speedup timeout {}", config.getSelf(), speedUpTId);
        trigger(st, timer);
    }

    private void cancelSpeedUp() {
        log.debug("{} canceling speedup timeout {}", config.getSelf(), speedUpTId);
        CancelTimeout cancelSpeedUp = new CancelTimeout(speedUpTId);
        trigger(cancelSpeedUp, timer);
    }

    private void scheduleUpdateSelf() {
        long updateSelfPeriod = config.descriptorUpdate; //get proper value later
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(updateSelfPeriod, updateSelfPeriod);
        Timeout t = new ScheduledUtilityUpdate(spt);
        updateSelfTId = t.getTimeoutId();
        spt.setTimeoutEvent(t);
        log.debug("{} scheduling periodic timeout {}", config.getSelf(), t);
        trigger(spt, timer);
    }

    private void cancelUpdateSelf() {
        log.debug("{} canceling periodic timeout {}", config.getSelf(), updateSelfTId);
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(updateSelfTId);
        trigger(cpt, timer);
    }

    private void finishDownload() {
        log.info("{} finished download", config.getSelf());

        downloading = false;

        //one last update
        trigger(new UtilityUpdate(config.overlayId, downloading, fileMngr.contiguousStart()), utilityUpdate);

        //cancel timeouts
        cancelSpeedUp();
        cancelUpdateSelf();

    }

    public static class DownloadMngrInit extends Init<DownloadMngrComp> {

        public final DownloadMngrConfig config;
        public final FileMngr hashMngr;
        public final FileMngr fileMngr;
        public final boolean downloader;

        public DownloadMngrInit(DownloadMngrConfig config, FileMngr fileMngr, FileMngr hashMngr, boolean downloader) {
            this.config = config;
            this.hashMngr = hashMngr;
            this.fileMngr = fileMngr;
            this.downloader = downloader;
        }
    }
}
