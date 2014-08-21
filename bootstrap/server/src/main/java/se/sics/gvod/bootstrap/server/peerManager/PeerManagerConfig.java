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
package se.sics.gvod.bootstrap.server.peerManager;

import com.typesafe.config.Config;
import se.sics.gvod.common.util.ConfigException;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class PeerManagerConfig {

    public final byte[] seed;
    public final int sampleSize;

    public PeerManagerConfig(byte[] seed, int sampleSize) {
        this.seed = seed;
        this.sampleSize = sampleSize;
    }

    public static class Builder {

        private final Config config;
        private final byte[] seed;
        private Integer sampleSize;

        public Builder(Config config, byte[] seed) {
            this.config = config;
            this.seed = seed;
        }

        public PeerManagerConfig finalise() throws ConfigException.Missing {
            try {
                sampleSize = (sampleSize == null ? config.getInt("bootstrap.sampleSize") : sampleSize);
                
                return new PeerManagerConfig(seed, sampleSize);
            } catch (com.typesafe.config.ConfigException.Missing ex) {
                throw new ConfigException.Missing(ex.getMessage());
            }
        }
    }
}
