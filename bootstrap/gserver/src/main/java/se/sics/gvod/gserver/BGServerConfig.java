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

package se.sics.gvod.gserver;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import se.sics.gvod.address.Address;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.net.VodAddress;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BGServerConfig {
    public final VodAddress self;
    public final Config config;
    public final byte[] seed;

    private BGServerConfig(Config config, VodAddress self, byte[] seed) {
        this.config = config;
        this.self = self;
        this.seed = seed;
    }

    public static class ExecBuilder {

        private Config config;
        private Address selfAddress;
        private byte[] seed;

        public ExecBuilder() {
            this.config = ConfigFactory.load();
        }

        public ExecBuilder setSelfAddress(Address selfAddress) {
            this.selfAddress = selfAddress;
            return this;
        }

        public ExecBuilder setSeed(byte[] seed) {
            this.seed = seed;
            return this;
        }

        public BGServerConfig finalise() throws GVoDConfigException.Missing {
            if (selfAddress == null) {
                throw new GVoDConfigException.Missing("self Address");
            }
            if (seed == null) {
                throw new GVoDConfigException.Missing("missing seed");
            }

            return new BGServerConfig(config, new VodAddress(selfAddress, -1), seed);
        }
    }
}