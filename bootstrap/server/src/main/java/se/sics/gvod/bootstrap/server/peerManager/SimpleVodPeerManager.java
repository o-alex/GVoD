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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import se.sics.gvod.address.Address;
import se.sics.gvod.bootstrap.server.VodPeerManager;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimpleVodPeerManager implements VodPeerManager {

    private final VodPeerManagerConfig config;
    private final Set<Address> systemPeers;
    private final Map<Integer, Set<Address>> overlayPeers;
    
    public SimpleVodPeerManager(VodPeerManagerConfig config) {
        this.config = config;
        this.overlayPeers = new HashMap<>();
        this.systemPeers = new HashSet<>();
    }
    
    @Override
    public void addVodPeer(Address peerAdr) {
        systemPeers.add(peerAdr);
    }

    @Override
    public void getOverlaySample(int overlayId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void getSystemSample() {
        
    }
    
}
