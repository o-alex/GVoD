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
package se.sics.gvod.system.video;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.system.video.connMngr.ConnMngr;
import se.sics.gvod.system.video.storage.Storage;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VideoComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(VideoComp.class);

    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);

    private final VideoConfig config;
    private final ConnMngr connMngr;
    private final Storage videoFile;
    
    public VideoComp(VideoInit init) {
        this.config = init.config;
        this.connMngr = init.connMngr;
        this.videoFile = init.videoFile;
        
        log.info("{} video component init", config.selfAddress);
        log.debug("{} starting with sample {}", new Object[]{config.selfAddress, init.overlaySample});
    }

    public static class VideoInit extends Init<VideoComp> {
        public final VideoConfig config;
        public final ConnMngr connMngr;
        public final Storage videoFile;
        public final Map<VodAddress, Integer> overlaySample;
        
        public VideoInit(VideoConfig config, ConnMngr connMngr, Storage videoFile, Map<VodAddress, Integer> overlaySample) {
            this.config = config;
            this.connMngr = connMngr;
            this.videoFile = videoFile;
            this.overlaySample = overlaySample;
        }
    }
}
