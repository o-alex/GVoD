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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerPort;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMAddFileMetadata;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMGetFileMetadata;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMGetOverlaySample;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMJoinOverlay;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimPMComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(SimPMComp.class);

    private Negative<PeerManagerPort> peerManager = provides(PeerManagerPort.class);

    private final SimPMConfig config;
    
    private final Random rand;
    private final Map<Integer, Map<Integer, byte[]>> overlays;
    private final Map<Integer, byte[]> files;

    public SimPMComp(SimPMInit init) {
            log.debug("init");
            this.config = init.config;
            this.overlays = new HashMap<Integer, Map<Integer, byte[]>>();
            this.overlays.put(0, new HashMap<Integer, byte[]>());
            this.files = new HashMap<Integer, byte[]>();
            this.rand = new Random(config.intSeed);
            
            subscribe(handleJoinOverlay, peerManager);
            subscribe(handleGetOverlaySample, peerManager);
            subscribe(handleAddFileMetadata, peerManager);
            subscribe(handleGetFileMetadata, peerManager);
    }

     public Handler<PMJoinOverlay.Request> handleJoinOverlay = new Handler<PMJoinOverlay.Request>() {

        @Override
        public void handle(PMJoinOverlay.Request req) {
            log.trace("{} received {}", new Object[]{config.selfAddress, req});
            log.debug("{} overlay:{} joined peer:{}", new Object[]{config.selfAddress, req.overlayId, req.nodeId});
            Map<Integer, byte[]> overlayPeers = overlays.get(req.overlayId);
            if(overlayPeers == null) {
                overlayPeers = new HashMap<Integer, byte[]>();
                overlays.put(req.overlayId, overlayPeers);
            }
            overlayPeers.put(req.nodeId, req.nodeInfo);
            trigger(req.success(), peerManager);
        }
    };

    public Handler<PMGetOverlaySample.Request> handleGetOverlaySample = new Handler<PMGetOverlaySample.Request>() {

        @Override
        public void handle(PMGetOverlaySample.Request req) {
            log.debug("{} received {}", new Object[]{config.selfAddress, req});
            Map<Integer, byte[]> overlayIds = overlays.get(req.overlayId);
            if(overlayIds == null) {
                trigger(req.success(new HashSet<byte[]>()), peerManager);
            } else {
                trigger(req.success(getOverlaySample(req.overlayId)), peerManager);
            }
        }
    };

    public Handler<PMAddFileMetadata.Request> handleAddFileMetadata = new Handler<PMAddFileMetadata.Request>() {

        @Override
        public void handle(PMAddFileMetadata.Request req) {
            log.debug("{} received {}", new Object[]{config.selfAddress, req});
            if(files.containsKey(req.overlayId)) {
                trigger(req.fail(), peerManager);
            } else {
                files.put(req.overlayId, req.fileMetadata);
                trigger(req.success(), peerManager);
            }
        }
    };
    
    public Handler<PMGetFileMetadata.Request> handleGetFileMetadata = new Handler<PMGetFileMetadata.Request>() {

        @Override
        public void handle(PMGetFileMetadata.Request req) {
            log.debug("{} received {}", new Object[]{config.selfAddress, req});
            if(files.containsKey(req.overlayId)) {
                trigger(req.success(files.get(req.overlayId)), peerManager);
            } else {
                trigger(req.fail(), peerManager);
            }
        }
    };
    
    private Set<byte[]> getOverlaySample(int overlayId) {
        Set<byte[]> result = new HashSet<byte[]>();
        Map<Integer, byte[]> overlayPeers = overlays.get(overlayId);
        if(overlayPeers == null) {
            System.exit(1);
            throw new RuntimeException("no overlay");
        }
        if(overlayPeers.size() <= config.sampleSize) {
            result.addAll(overlayPeers.values());
        } else {
            Iterator<byte[]> it = overlayPeers.values().iterator();
            while(result.size() < config.sampleSize && it.hasNext()) {
                result.add(it.next());
            }
        }
        return result;
    }

    public static class SimPMInit extends Init<SimPMComp> {

        public final SimPMConfig config;

        public SimPMInit(SimPMConfig config) {
            this.config = config;
        }
    }
}
