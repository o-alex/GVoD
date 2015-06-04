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
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Random;
import se.sics.gvod.bootstrap.cclient.CaracalPSManagerConfig;
import se.sics.gvod.bootstrap.client.BootstrapClientConfig;
import se.sics.gvod.bootstrap.server.BootstrapServerConfig;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.core.VoDConfig;
import se.sics.kompics.network.Address;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HostConfiguration {

    private final Config config;
    public final DecoratedAddress self;
    public final DecoratedAddress server;
    public final byte[] seed;
    public final String libDir;

    private HostConfiguration(Config config, DecoratedAddress self, DecoratedAddress server, byte[] seed, String libDir) {
        this.config = config;
        this.self = self;
        this.server = server;
        this.seed = seed;
        this.libDir = libDir;
    }

    public BootstrapClientConfig.Builder getBootstrapClientConfig() {
        return new BootstrapClientConfig.Builder(config, self, server, seed);
    }

    public VoDConfig.Builder getVoDConfiguration() {
        return new VoDConfig.Builder(config, self, libDir);
    }

    public BootstrapServerConfig getBootstrapServerConfig() {
        return new BootstrapServerConfig(config, self, seed);
    }

    public CaracalPSManagerConfig getCaracalPSManagerConfig() {
        return new CaracalPSManagerConfig.Builder(config, seed).setSelfAddress(self.getIp(), self.getPort() + 1, self.getId()).finalise();
    }

    public static class SimulationBuilder {

        private final Config config;
        private Integer id = null;
        private byte[] seed = null;
        private String libDir = null;

        public SimulationBuilder() {
            this.config = ConfigFactory.load();
        }

        public SimulationBuilder(String configFile) {
            this.config = ConfigFactory.load(configFile);
        }

        public SimulationBuilder setId(int id) {
            this.id = id;
            return this;
        }

        public SimulationBuilder setSeed(byte[] seed) {
            this.seed = seed;
            return this;
        }

        public SimulationBuilder setLibDir(String libDir) {
            this.libDir = libDir;
            return this;
        }

        public HostConfiguration finalise() throws GVoDConfigException.Missing {
            try {
                if (id == null) {
                    throw new GVoDConfigException.Missing("id");
                }
                DecoratedAddress self = new DecoratedAddress(new BasicAddress(
                        Inet4Address.getLocalHost(),
                        config.getInt("vod.address.port"),
                        id));

                DecoratedAddress server = new DecoratedAddress(new BasicAddress(
                        Inet4Address.getLocalHost(),
                        config.getInt("bootstrap.server.address.port"),
                        config.getInt("bootstrap.server.address.id")
                ));

                if (seed == null) {
                    throw new GVoDConfigException.Missing("seed");
                }
                libDir = (libDir == null ? config.getString("vod.libDir") : libDir);
                return new HostConfiguration(config, self, server, seed, libDir);
            } catch (UnknownHostException e) {
                throw new GVoDConfigException.Missing("bad host - " + e.getMessage());
            } catch (com.typesafe.config.ConfigException e) {
                throw new GVoDConfigException.Missing(e.getMessage());
            }
        }
    }

    public static class ExecBuilder {

        private final Config config;
        private DecoratedAddress selfAddress;
        private byte[] seed;

        public ExecBuilder() {
            this.config = ConfigFactory.load();
        }

        public int getPort() throws GVoDConfigException.Missing {
            try {
                return config.getInt("vod.address.port");
            } catch (ConfigException.Missing ex) {
                throw new GVoDConfigException.Missing(ex);
            }
        }

        public int getId() throws GVoDConfigException.Missing {
            try {
                return config.getInt("vod.address.id");
            } catch (ConfigException.Missing ex) {
                Random rand  = new SecureRandom();
                return rand.nextInt();
            }
        }

        public String getIp() throws GVoDConfigException.Missing {
            try {
                return config.getString("vod.address.ip");
            } catch (ConfigException.Missing ex) {
                throw new GVoDConfigException.Missing(ex);
            }
        }

        public ExecBuilder setSelfAddress(DecoratedAddress selfAddress) {
            this.selfAddress = selfAddress;
            return this;
        }

        public ExecBuilder setSeed(byte[] seed) {
            this.seed = seed;
            return this;
        }

        public HostConfiguration finalise() throws GVoDConfigException.Missing {
            try {

                if (seed == null) {
                    throw new GVoDConfigException.Missing("missing seed");
                }

                String libDir = config.getString("vod.libDir");
                return new HostConfiguration(config, selfAddress, selfAddress, seed, libDir);
//            } catch (UnknownHostException e) {
//                throw new GVoDConfigException.Missing("bad host - " + e.getMessage());
            } catch (com.typesafe.config.ConfigException e) {
                throw new GVoDConfigException.Missing(e.getMessage());
            }
        }
    }
}
