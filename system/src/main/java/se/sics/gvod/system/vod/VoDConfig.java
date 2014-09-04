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

package se.sics.gvod.system.vod;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.system.video.VideoConfig;
import se.sics.gvod.system.video.connMngr.ConnMngrConfig;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VoDConfig {
    private final Config config;
    public final VodAddress selfAddress;
    public final int pieceSize;
    
    private VoDConfig(Config config, VodAddress selfAddress, int pieceSize) {
        this.config = config;
        this.selfAddress = selfAddress;
        this.pieceSize = pieceSize;
    }
    
    public VideoConfig.Builder getVideoConfig() {
        return new VideoConfig.Builder(config, selfAddress);
    }
    
    public ConnMngrConfig.Builder getConnMngrConfig() {
        return new ConnMngrConfig.Builder(config);
    }
    
    public static class Builder {
        private final Config config;
        private final VodAddress selfAddress;
        
        public Builder(Config config, VodAddress selfAddress) {
            this.config = config;
            this.selfAddress = selfAddress;
        }
        
        public VoDConfig finalise() throws GVoDConfigException.Missing {
            int pieceSize;
            try {
                pieceSize = config.getInt("vod.video.pieceSize");
            } catch(ConfigException.Missing ex) {
                throw new GVoDConfigException.Missing(ex);
            }
            return new VoDConfig(config, selfAddress, pieceSize);
        }
    }
}
