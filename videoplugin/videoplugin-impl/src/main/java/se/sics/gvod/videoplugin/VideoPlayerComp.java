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
package se.sics.gvod.videoplugin;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.core.downloadMngr.Data;
import se.sics.gvod.core.downloadMngr.DownloadMngrPort;
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
public class VideoPlayerComp extends ComponentDefinition implements VideoPlayer {

    private static final Logger log = LoggerFactory.getLogger(VideoPlayerComp.class);

    private final Positive<DownloadMngrPort> videoStore = requires(DownloadMngrPort.class);
    private final Positive<Timer> timer = requires(Timer.class);

    private VideoPlayerConfig config;
    private TimeoutId nextReadTId;
    private OutputStream responseBody;

    private boolean writeHeader;
    private long playPos;

    public VideoPlayerComp(VideoPlayerInit init) {
        this.config = init.config;
        this.playPos = -1;

        subscribe(handleStart, control);
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start e) {
            log.info("{} starting...", config.overlayId);
            subscribe(handleTryRead, timer);
            subscribe(handleReadResponse, videoStore);
        }
    };

    private Handler<VideoPlayerTimeout.TryRead> handleTryRead = new Handler<VideoPlayerTimeout.TryRead>() {

        @Override
        public void handle(VideoPlayerTimeout.TryRead timeout) {
            synchronized (config) {
                if (playPos == -1) {
                    return;
                }
                log.debug("{} timeout {}", config.overlayId, timeout.getTimeoutId());
                tryNextPiece();
            }
        }
    };

    private Handler<Data.DResponse> handleReadResponse = new Handler<Data.DResponse>() {

        @Override
        public void handle(Data.DResponse resp) {
            synchronized (config) {
                if (playPos == -1) {
                    return;
                }
                if (resp.status.equals(ReqStatus.SUCCESS)) {
                    try {
                        log.debug("{} sending data from:{} size:{}", new Object[]{config.overlayId, playPos, resp.block.length});
                        responseBody.write(resp.block);
                        responseBody.flush();
                    } catch (IOException ex) {
                        log.warn("{} player seems to have died... reseting connection", config.overlayId);
                        playPos = -1;
                        responseBody = null;
                        return;
                    }

                    playPos = playPos + resp.block.length;
                    if (playPos == config.videoLength) {
                        log.info("{} video ended");
                        playPos = -1;
                    } else {
                        tryNextPiece();
                    }
                } else {
                    scheduleTryRead(1);
                }
            }
        }
    };

    private void tryNextPiece() {
        log.debug("{} requesting data from:{}", config.overlayId, playPos);
        trigger(new Data.DRequest(UUID.randomUUID(), config.overlayId, playPos, config.readBlockSize), videoStore);
    }

    private void scheduleTryRead(float fractionPeriod) {
        ScheduleTimeout st = new ScheduleTimeout((long) (fractionPeriod * config.readDelay));
        Timeout t = new VideoPlayerTimeout.TryRead(st);
        st.setTimeoutEvent(t);
        nextReadTId = t.getTimeoutId();
        log.debug("video player:{} scheduling next read {}", config.overlayId, nextReadTId);
        trigger(st, timer);
    }

    //TODO Alex -make thread safe later
    @Override
    public long getLength() {
        return config.videoLength;
    }

    @Override
    public String getVideoName() {
        return config.videoName;
    }

    @Override
    public void play(long readPos, OutputStream responseBody) {
        synchronized (config) {
            if (playPos != -1) {
                log.info("{} already playing video", config.overlayId);
                return;
            }
            log.info("{} play at {}", config.overlayId, readPos);
            this.playPos = readPos;
            this.responseBody = responseBody;

//            subscribe(handleTryRead, timer);
//            subscribe(handleReadResponse, videoStore);

            scheduleTryRead(1);
        }
    }

    @Override
    public void stop() {
        synchronized (config) {
            if (playPos == -1) {
                log.info("{} already stopped", config.overlayId);
                return;
            }
            log.info("{} stop", config.overlayId);
//            unsubscribe(handleTryRead, timer);
//            unsubscribe(handleReadResponse, videoStore);
            playPos = -1;
        }
    }

    public static class VideoPlayerInit extends Init<VideoPlayerComp> {

        public final VideoPlayerConfig config;

        public VideoPlayerInit(VideoPlayerConfig config) {
            this.config = config;
        }
    }

    public static class VideoPlayerConfig {

        public final String videoName;
        public final int overlayId;
        public final long videoLength;
        public final long readDelay;
        public final int readBlockSize;

        public VideoPlayerConfig(String videoName, int overlayId, int videoLength, long readDelay, int readBlockSize) {
            this.videoName = videoName;
            this.videoLength = videoLength;
            this.overlayId = overlayId;
            this.readDelay = readDelay;
            this.readBlockSize = readBlockSize;
        }
    }
}
