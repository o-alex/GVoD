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
package se.sics.gvod.bootstrap.server.simulation;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import se.sics.gvod.address.Address;
import se.sics.gvod.common.util.GVoDConfigException;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimPMConfig {

    private final Config config;
    public final Address selfAddress;
    public final byte[] seed;
    public final int intSeed;
    public final int sampleSize;

    private SimPMConfig(Config config, byte[] seed, int intSeed, Address selfAddress, int sampleSize) {
        this.config = config;
        this.seed = seed;
        this.intSeed = intSeed;
        this.selfAddress = selfAddress;
        this.sampleSize = sampleSize;
    }

    public static class Builder {

        private final Config config;
        private final byte[] seed;
        private final Address selfAddress;

        public Builder(Config config, byte[] seed, Address selfAddress) {
            this.config = config;
            this.seed = seed;
            this.selfAddress = selfAddress;
        }
        
        public SimPMConfig finalise() throws GVoDConfigException.Missing {
            int sampleSize;
            int intSeed;
            try {
                intSeed = config.getInt("intSeed");
                sampleSize = config.getInt("bootstrap.server.sampleSize");
            } catch (ConfigException.Missing ex) {
                throw new GVoDConfigException.Missing(ex);
            }
            return new SimPMConfig(config, seed, intSeed, selfAddress, sampleSize);
        }
    }
}
