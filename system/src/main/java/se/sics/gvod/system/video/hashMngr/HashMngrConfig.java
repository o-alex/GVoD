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

package se.sics.gvod.system.video.hashMngr;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.common.util.HashUtil;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HashMngrConfig {
    public final String libDir;
    public final String videoName;
    public final int pieceSize;
    public final String hashAlg;
    public final int hashSize;
    
    private HashMngrConfig(String libDir, String videoName, int pieceSize, String hashAlg, int hashSize) {
        this.libDir = libDir;
        this.videoName = videoName;
        this.pieceSize = pieceSize;
        this.hashAlg = hashAlg;
        this.hashSize = hashSize;
    }
    
    public static class Builder {
        private final Config config;
        private final String libDir;
        private final String videoName;
        private final int pieceSize;
        
        public Builder(Config config, String libDir, String videoName, int pieceSize) {
            this.config = config;
            this.libDir = libDir;
            this.videoName = videoName;
            this.pieceSize = pieceSize;
        }

        public HashMngrConfig finalise() throws GVoDConfigException.Missing {
            try {
                String hashAlg = config.getString("vod.video.hashAlg");
                int hashSize = HashUtil.getHashSize(hashAlg);
                
                return new HashMngrConfig(libDir, videoName, pieceSize, hashAlg, hashSize);
            }catch (ConfigException.Missing ex) {
                throw new GVoDConfigException.Missing(ex);
            }
        }
    }
}
