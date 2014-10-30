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
package se.sics.gvod.bootstrap.cclient;

import com.typesafe.config.Config;
import java.net.InetAddress;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.kompics.address.Address;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CaracalPSManagerConfig {

    private final Config config;
    public final Address selfAddress;
    public final byte[] seed;

    private CaracalPSManagerConfig(Config config, Address selfAddress, byte[] seed) {
        this.config = config;
        this.selfAddress = selfAddress;
        this.seed = seed;
    }

    public CaracalPeerStoreConfig getCaracalPeerStoreConfig() throws GVoDConfigException.Missing {
        return new CaracalPeerStoreConfig.Builder(config, seed, selfAddress).finalise();
    }
    
    public static class Builder {
        private final Config config;
        private final byte[] seed;
        private Address selfAddress;
        
        public Builder(Config config, byte[] seed) {
            this.config = config;
            this.seed = seed;
        } 
        
        public Builder setSelfAddress(InetAddress ip, int port, Integer id) {
            this.selfAddress = new Address(ip, port, id.byteValue());
            return this;
        }
        
        public CaracalPSManagerConfig finalise() {
            return new CaracalPSManagerConfig(config, selfAddress, seed);
        }
    }
}
