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

import com.typesafe.config.Config;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.net.VodAddress;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VideoConfig {
    private final Config config;
    public final VodAddress selfAddress;
    public final boolean downloader;
    
    private VideoConfig(Config config, VodAddress selfAddress, boolean downloader) {
        this.config = config;
        this.selfAddress = selfAddress;
        this.downloader = downloader;
    }
    
    public static class Builder {
        private final Config config;
        private final VodAddress selfAddress;
        private Boolean downloader;
        
        public Builder(Config config, VodAddress selfAddress) {
            this.config = config;
            this.selfAddress = selfAddress;
        }
        
        public Builder setDownloader(boolean downloader) {
            this.downloader = downloader;
            return this;
        }
        
        public VideoConfig finalise() throws GVoDConfigException.Missing {
            if(downloader == null) {
                throw new GVoDConfigException.Missing("downloader not set");
            }
            return new VideoConfig(config, selfAddress, downloader);
        }
    }
}
