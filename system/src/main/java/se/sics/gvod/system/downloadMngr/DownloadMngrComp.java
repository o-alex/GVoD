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
package se.sics.gvod.system.downloadMngr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.util.VodDescriptor;
import se.sics.gvod.system.connMngr.ConnMngrPort;
import se.sics.gvod.system.connMngr.LocalVodDescriptor;
import se.sics.gvod.system.connMngr.msg.Ready;
import se.sics.gvod.common.msg.vod.Download;
import se.sics.gvod.system.downloadMngr.msg.DownloadControl;
import se.sics.gvod.system.downloadMngr.msg.UpdateSelf;
import se.sics.gvod.system.video.storage.FileMngr;
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
import se.sics.kompics.Positive;
import se.sics.kompics.Start;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DownloadMngrComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(DownloadMngrComp.class);

    private Positive<Timer> timer = requires(Timer.class);
    private Positive<ConnMngrPort> connMngr = requires(ConnMngrPort.class);

    private final DownloadMngrConfig config;

    private final FileMngr hashMngr;
    private final FileMngr fileMngr;
    private boolean downloading;

    private Set<Integer> pendingPieces;
    private List<Integer> nextPieces;

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

        subscribe(handleStart, control);
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.trace("{} starting...", config.getSelf());

            VodDescriptor newSelf = new VodDescriptor(fileMngr.contiguousStart());
            LocalVodDescriptor localSelf = new LocalVodDescriptor(newSelf, downloading);

            log.info("{} download at piece: {}", config.getSelf(), newSelf.downloadPos);
            trigger(new UpdateSelf(UUID.randomUUID(), localSelf), connMngr);

            subscribe(handleConnReady, connMngr);
        }
    };

    private Handler<Ready> handleConnReady = new Handler<Ready>() {
        @Override
        public void handle(Ready event) {
            if (downloading) {
                log.info("{} starting video download", config.getSelf());

                getNewPieces();
                for (int i = 0; i < config.startPieces; i++) {
                    downloadPiece();
                }

                scheduleSpeedUp(10000);
                subscribe(handleDownloadSpeedUp, timer);

                schedulePeriodicUpdateSelf();
                subscribe(handleUpdateSelf, timer);
            } else {
                log.info("{} seeding file", config.getSelf());
            }

            subscribe(handleDownloadRequest, connMngr);
            subscribe(handleDownloadResponse, connMngr);
            subscribe(handleDownloadSlowDown, connMngr);
            subscribe(handleDownloadTimeout, connMngr);

        }
    };

    private Handler<Download.Request> handleDownloadRequest = new Handler<Download.Request>() {

        @Override
        public void handle(Download.Request req) {
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

    private Handler<Download.Response> handleDownloadResponse = new Handler<Download.Response>() {

        @Override
        public void handle(Download.Response resp) {
            log.trace("{} handle {}", config.getSelf(), resp);

            if (!resp.status.equals(ReqStatus.SUCCESS)) {
                log.debug("{} req for piece {} returned with status {}", new Object[]{config.getSelf(), resp.pieceId, resp.status});
                return;
            }
            //TODO check hash
            fileMngr.writePiece(resp.pieceId, resp.piece);

            if (nextPieces.isEmpty()) {
                if (fileMngr.isComplete()) {
                    finishDownload();
                    return;
                }
                getNewPieces();
            }
            log.debug("{} next pieces to download {}", config.getSelf(), nextPieces);
            if (!nextPieces.isEmpty()) {
                downloadPiece();
            }
        }
    };

    private Handler<Download.ReqTimeout> handleDownloadTimeout = new Handler<Download.ReqTimeout>() {

        @Override
        public void handle(Download.ReqTimeout timeout) {
            log.trace("{} handling piece {} timeout", config.getSelf(), timeout.pieceId);

            pendingPieces.remove(timeout.pieceId);
            nextPieces.add(0, timeout.pieceId);
        }
    };

    private Handler<DownloadControl.SlowDown> handleDownloadSlowDown = new Handler<DownloadControl.SlowDown>() {

        @Override
        public void handle(DownloadControl.SlowDown event) {
            log.trace("{} handling {}", config.getSelf(), event);

            pendingPieces.remove(event.canceledPiece);
            nextPieces.add(0, event.canceledPiece);

            cancelSpeedUp();
            scheduleSpeedUp(10000);
        }
    };

    private Handler<DownloadControl.ScheduledSpeedUp> handleDownloadSpeedUp = new Handler<DownloadControl.ScheduledSpeedUp>() {

        @Override
        public void handle(DownloadControl.ScheduledSpeedUp event) {
            log.trace("{} handling speedup {}", config.getSelf(), event.getTimeoutId());

            if (speedUpTId == null) {
                log.info("{} late timeout {}", config.getSelf(), speedUpTId);
                return;
            }
            if (nextPieces.isEmpty()) {
                if (fileMngr.isComplete()) {
                    finishDownload();
                    return;
                }
                getNewPieces();
            }

            if (!nextPieces.isEmpty()) {
                downloadPiece();
            }
            scheduleSpeedUp(10000);
        }

    };

    private Handler<UpdateSelf.UpdateTimeout> handleUpdateSelf = new Handler<UpdateSelf.UpdateTimeout>() {

        @Override
        public void handle(UpdateSelf.UpdateTimeout event) {
            log.trace("{} handle {}", config.getSelf(), event);

            VodDescriptor newSelf = new VodDescriptor(fileMngr.contiguousStart());
            LocalVodDescriptor localSelf = new LocalVodDescriptor(newSelf, downloading);
            log.info("{} download at piece: {}", config.getSelf(), newSelf.downloadPos);
            trigger(new UpdateSelf(UUID.randomUUID(), localSelf), connMngr);
        }
    };

    //TODO get a good way to get new pieces
    private void getNewPieces() {
        int nrNewPieces = 2 * (config.startPieces + pendingPieces.size());
        Set<Integer> newNextPieces = fileMngr.nextPiecesNeeded(nrNewPieces, 0);
        newNextPieces.removeAll(pendingPieces);

        nextPieces = new ArrayList<Integer>(newNextPieces);
    }

    private void downloadPiece() {
        int nextPieceId = nextPieces.remove(0);
        log.debug("{} downloading piece {}", config.getSelf(), nextPieceId);
        trigger(new Download.Request(UUID.randomUUID(), config.overlayId, nextPieceId), connMngr);
        pendingPieces.add(nextPieceId);
    }

    private void scheduleSpeedUp(int periodFactor) {
        ScheduleTimeout st = new ScheduleTimeout(periodFactor);
        Timeout t = new DownloadControl.ScheduledSpeedUp(st);
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

    private void schedulePeriodicUpdateSelf() {
        long updateSelfPeriod = config.descriptorUpdate; //get proper value later
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(updateSelfPeriod, updateSelfPeriod);
        Timeout t = new UpdateSelf.UpdateTimeout(spt);
        updateSelfTId = t.getTimeoutId();
        spt.setTimeoutEvent(t);
        log.debug("{} scheduling periodic timeout {}", config.getSelf(), t);
        trigger(spt, timer);
    }

    private void cancelPeriodicUpdateSelf() {
        log.debug("{} canceling periodic timeout {}", config.getSelf(), updateSelfTId);
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(updateSelfTId);
        trigger(cpt, timer);
    }
    
    private void finishDownload() {
        log.info("{} finished download", config.getSelf());

        downloading = false;

        //one last update
        VodDescriptor newSelf = new VodDescriptor(fileMngr.contiguousStart());
        LocalVodDescriptor localSelf = new LocalVodDescriptor(newSelf, downloading);
        trigger(new UpdateSelf(UUID.randomUUID(), localSelf), connMngr);

        //cancel timeouts
        cancelSpeedUp();
        cancelPeriodicUpdateSelf();
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
