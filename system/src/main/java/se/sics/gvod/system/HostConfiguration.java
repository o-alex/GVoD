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

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HostConfiguration {

    private final Config config;
    public final Address self;
    public final Address bootstrapServer;

    private HostConfiguration(Config config, Address self, Address bootstrapServer) {
        this.config = config;
        this.self = self;
        this.bootstrapServer = bootstrapServer;
    }
    
    public BootstrapClientConfig getBootstrapClientConfig() {
        return new BootstrapClientConfig(self, bootstrapServer);
    }
    
    public VoDConfiguration getVoDConfiguration() {
        return new VoDConfiguration(self.getId());
    }

    public static class Builder {

        private Config config;
        private Integer id;
        

        public Builder() {
            loadDefault();
        }

        private void loadDefault() {
            this.config = ConfigFactory.load();
        }

        private loadConfig(String configFile) {
            this.config = ConfigFactory.load(configFile);
            return this;
        }

        public Builder setId(int id) {
            this.id = id;
            return this;
        }
        
        public HostConfiguration finalise() throws ConfigException {
            try {
                Address self = new Address(
                        InetAddress.getByName(config.getString("vod.address.ip")),
                        config.getInt("vod.address.port"),
                        id == null ? config.getInt("vod.address.id") : id
                );
                
                Address bootstrapServer = new Address(
                        InetAddress.getByName(config.getString("bootstrap.address.ip")),
                        config.getInt("bootstrap.address.port"),
                        config.getInt("bootstrap.address.id")
                );
                
                return new HostConfiguration(config, self, bootstrapServer);
            } catch (UnknownHostException | com.typesafe.config.ConfigException ex) {
                throw new ConfigException(ex);
            } 
        }

    }

    public static class ConfigException extends Exception {

        public ConfigException(String msg) {
            super(msg);
        }
        
        public ConfigException(Throwable cause) {
            super(cause);
        }
    }
}
