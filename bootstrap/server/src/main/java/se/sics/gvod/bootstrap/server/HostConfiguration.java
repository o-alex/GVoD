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

package se.sics.gvod.bootstrap.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HostConfiguration {
    public final Config config;
    
    private HostConfiguration(Config config) {
        this.config = config;
    }
    
    public InetAddress getIp() throws UnknownHostException {
        return InetAddress.getLocalHost();
    }
    
    public int getPort() {
        return Integer.valueOf(config.getString("bootstrap.address.port"));
    }
    
    public int getId() {
        return Integer.valueOf(config.getString("bootstrap.address.id")); 
    }
    
    public static class Builder {
        private Config config;
        public Builder() {
            loadDefault();
        }
        
        private void loadDefault() {
            this.config = ConfigFactory.load();
        }
        
        public Builder loadConfig(String configFile) {
            this.config = ConfigFactory.load(configFile);
            return this;
        }
        
        public HostConfiguration finalise() {
            return new HostConfiguration(config);
        }
    }
}
