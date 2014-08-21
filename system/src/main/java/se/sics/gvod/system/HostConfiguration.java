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
package se.sics.gvod.system;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import se.sics.gvod.address.Address;
import se.sics.gvod.bootstrap.client.BootstrapClientConfig;
import se.sics.gvod.common.util.ConfigException;
import se.sics.gvod.net.VodAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HostConfiguration {

    private final Config config;
    public final VodAddress self;
    public final VodAddress server;
    public final byte[] seed;

    private HostConfiguration(Config config, VodAddress self, VodAddress server, byte[] seed) {
        this.config = config;
        this.self = self;
        this.server = server;
        this.seed = seed;
    }

    public BootstrapClientConfig.Builder getBootstrapClientConfig() {
        return new BootstrapClientConfig.Builder(config, self, server, seed);
    }

    public VoDConfiguration.Builder getVoDConfiguration() {
        return new VoDConfiguration.Builder(config, self);
    }

    public static class Builder {

        private final Config config;
        private Integer id;
        private byte[] seed;

        public Builder() {
            this.config = ConfigFactory.load();
        }

        public Builder(String configFile) {
            this.config = ConfigFactory.load(configFile);
        }

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setSeed(byte[] seed) {
            this.seed = seed;
            return this;
        }

        public HostConfiguration finalise() throws ConfigException.Missing {
            try {
                Address self = new Address(
                        InetAddress.getByName(config.getString("vod.address.ip")),
                        config.getInt("vod.address.port"),
                        id == null ? config.getInt("vod.address.id") : id
                );

                Address server = new Address(
                        InetAddress.getByName(config.getString("bootstrap.address.ip")),
                        config.getInt("bootstrap.address.port"),
                        config.getInt("bootstrap.address.id")
                );

                if (seed == null) {
                    throw new ConfigException.Missing("missing seed");
                }
                return new HostConfiguration(config, new VodAddress(self, -1), new VodAddress(server, -1), seed);
            } catch (UnknownHostException e) {
                throw new ConfigException.Missing("bad host - " + e.getMessage());
            } catch (com.typesafe.config.ConfigException e) {
                throw new ConfigException.Missing(e.getMessage());
            }
        }
    }
}
