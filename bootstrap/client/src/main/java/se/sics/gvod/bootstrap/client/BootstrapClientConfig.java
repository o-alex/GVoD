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
package se.sics.gvod.bootstrap.client;

import com.typesafe.config.Config;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.net.VodAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapClientConfig {

    public final VodAddress self;
    public final VodAddress server;
    public final byte[] seed;
    public final int openViewSize;
    public final int storageViewSize;
    public final int heartbeatPeriod;

    private BootstrapClientConfig(VodAddress self, VodAddress server, byte[] seed, 
            int openViewSize, int storageViewSize, int heartbeatPeriod) {
        this.self = self;
        this.server = server;
        this.seed = seed;
        this.openViewSize = openViewSize;
        this.storageViewSize = storageViewSize;
        this.heartbeatPeriod = heartbeatPeriod;
    }

    public static class Builder {

        private final Config config;
        private final VodAddress self;
        private final VodAddress server;
        private final byte[] seed;

        public Builder(Config config, VodAddress self, VodAddress server, byte[] seed) {
            this.config = config;
            this.self = self;
            this.server = server;
            this.seed = seed;
        }

        public BootstrapClientConfig finalise() throws GVoDConfigException.Missing {
            try {
                int openViewSize = config.getInt("bootstrap.client.globalViewSize");
                int storageViewSize = config.getInt("bootstrap.client.storageViewSize");
                int heartbeatPeriod = config.getInt("bootstrap.client.heartbeatPeriod");
                
                return new BootstrapClientConfig(self, server, seed, openViewSize, storageViewSize, heartbeatPeriod);
            } catch (com.typesafe.config.ConfigException.Missing e) {
                throw new GVoDConfigException.Missing(e.getMessage());
            }
        }
    }
}
