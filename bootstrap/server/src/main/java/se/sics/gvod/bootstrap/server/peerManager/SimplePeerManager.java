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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import se.sics.gvod.net.VodAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimplePeerManager implements PeerManager {

    private final PeerManagerConfig config;
    private final List<VodAddress> systemPeers;
    private final Map<Integer, List<VodAddress>> overlayPeers;
    private final Random rand;
    
    public SimplePeerManager(PeerManagerConfig config) {
        this.config = config;
        
        this.overlayPeers = new HashMap<>();
        this.systemPeers = new ArrayList<>();
        this.rand = new SecureRandom(config.seed);
    }
    
    @Override
    public void addVodPeer(VodAddress peerAdr) {
        systemPeers.add(peerAdr);
    }
    
    @Override
    public void addOverlay(int overlayId) throws PeerManager.PMException {
        if(overlayPeers.containsKey(overlayId)) {
            throw new PeerManager.PMException("overlay " + overlayId + " already exists");
        }
        overlayPeers.put(overlayId, new ArrayList<VodAddress>());
    }
    
    @Override
    public void addOverlayPeer(int overlayId, VodAddress peer) throws PeerManager.PMException {
        if(!overlayPeers.containsKey(overlayId)) {
            throw new PeerManager.PMException("overlay " + overlayId + " does not exist");
        }
        overlayPeers.get(overlayId).add(peer);
        
    }

    @Override
    public Set<VodAddress> getOverlaySample(int overlayId) throws PeerManager.PMException {
        Set<VodAddress> sample = new HashSet<>();
        
        List<VodAddress> overlay = overlayPeers.get(overlayId);
        if(overlay == null) {
            throw new PeerManager.PMException("overlay " + overlayId + " does not exist");
        }
        if(overlay.size() <= config.sampleSize) {
            sample.addAll(overlay);
            return sample;
        }
        while(sample.size() < config.sampleSize) {
            int idx = rand.nextInt(overlay.size());
            sample.add(overlay.get(idx));
        }
        return sample;
    }

    @Override
    public Set<VodAddress> getSystemSample() {
        Set<VodAddress> sample = new HashSet<>();
        
        if(config.sampleSize >= systemPeers.size()) {
            sample.addAll(systemPeers);
            return sample;
        }
        while(sample.size() < config.sampleSize) {
            int idx = rand.nextInt(systemPeers.size());
            sample.add(systemPeers.get(idx));
        }
        return sample;
    }
    
}
