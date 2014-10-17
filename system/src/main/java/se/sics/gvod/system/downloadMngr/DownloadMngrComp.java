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
import se.sics.gvod.common.util.VodDescriptor;
import se.sics.gvod.system.connMngr.ConnMngrPort;
import se.sics.gvod.system.connMngr.LocalVodDescriptor;
import se.sics.gvod.system.downloadMngr.msg.Download;
import se.sics.gvod.system.downloadMngr.msg.DownloadControl;
import se.sics.gvod.system.downloadMngr.msg.UpdateSelf;
import se.sics.gvod.system.video.storage.FileMngr;
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

    private TimeoutId speedUpTId;
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
        subscribe(handleDownloadRequest, connMngr);
        subscribe(handleDownloadResponse, connMngr);
        subscribe(handleDownloadSlowDown, connMngr);
        subscribe(handleDownloadSpeedUp, connMngr);
        subscribe(handleDownloadTimeout, connMngr);
        subscribe(handleUpdateSelf, timer);
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            if (downloading) {
                log.info("{} starting video download", config.getSelf());
                
                getNewPieces();
                for (int i = 0; i < config.startPieces; i++) {
                    downloadPiece();
                }

                scheduleSpeedUp(10);
                schedulePeriodicUpdateSelf();
            } else {
                log.info("{} seeding file", config.getSelf());
            }
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

            //TODO check hash
            fileMngr.writePiece(resp.pieceId, resp.piece);

            if (nextPieces.isEmpty()) {
                if (fileMngr.isComplete()) {
                    finishDownload();
                    return;
                }
                getNewPieces();
            }
            downloadPiece();
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

            CancelTimeout cancelSpeedUp = new CancelTimeout(speedUpTId);
            trigger(cancelSpeedUp, timer);
            scheduleSpeedUp(1000);
        }
    };

    private Handler<DownloadControl.ScheduledSpeedUp> handleDownloadSpeedUp = new Handler<DownloadControl.ScheduledSpeedUp>() {

        @Override
        public void handle(DownloadControl.ScheduledSpeedUp event) {
            log.trace("{} handling speedup");

            if (nextPieces.isEmpty()) {
                if (fileMngr.isComplete()) {
                    finishDownload();
                    return;
                }
                getNewPieces();
            }

            downloadPiece();
            scheduleSpeedUp(10);
        }

    };

    private Handler<UpdateSelf.UpdateTimeout> handleUpdateSelf = new Handler<UpdateSelf.UpdateTimeout>() {

        @Override
        public void handle(UpdateSelf.UpdateTimeout event) {
            log.trace("{} handle {}", config.getSelf(), event);
            
            VodDescriptor newSelf = new VodDescriptor(config.overlayId, fileMngr.contiguousStart());
            LocalVodDescriptor localSelf = new LocalVodDescriptor(newSelf, downloading);
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
        ScheduleTimeout st = new ScheduleTimeout(periodFactor * config.periodicUpdate);
        Timeout t = new DownloadControl.ScheduledSpeedUp(st);
        st.setTimeoutEvent(t);
        trigger(st, timer);
        speedUpTId = t.getTimeoutId();
    }

    private void schedulePeriodicUpdateSelf() {
        long updateSelfPeriod = config.periodicUpdate; //get proper value later
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(updateSelfPeriod, updateSelfPeriod);
        Timeout t = new UpdateSelf.UpdateTimeout(spt);
        updateSelfTId = t.getTimeoutId();
        spt.setTimeoutEvent(t);
        trigger(spt, timer);
    }

    private void finishDownload() {
        log.info("{} finished download", config.getSelf());
        
        downloading = false;

        //one last update
        VodDescriptor newSelf = new VodDescriptor(config.overlayId, fileMngr.contiguousStart());
        LocalVodDescriptor localSelf = new LocalVodDescriptor(newSelf, downloading);
        trigger(new UpdateSelf(UUID.randomUUID(), localSelf), connMngr);
        
        //cancel timeouts
        CancelTimeout ct = new CancelTimeout(speedUpTId);
        trigger(ct, timer);
        
        ct = new CancelTimeout(updateSelfTId);
        trigger(ct, timer);
    }

    public static class DownloadMngrInit extends Init<DownloadMngrComp> {

        public final DownloadMngrConfig config;
        public final FileMngr hashMngr;
        public final FileMngr fileMngr;
        public final boolean downloader;

        public DownloadMngrInit(DownloadMngrConfig config, FileMngr hashMngr, FileMngr fileMngr, boolean downloader) {
            this.config = config;
            this.hashMngr = hashMngr;
            this.fileMngr = fileMngr;
            this.downloader = downloader;
        }
    }
}
