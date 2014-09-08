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
package se.sics.gvod.croupier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.croupier.pub.msg.CroupierJoin;
import se.sics.gvod.croupier.pub.msg.CroupierJoinCompleted;
import se.sics.gvod.croupier.msg.intern.ShuffleCycle;
import se.sics.gvod.croupier.pub.util.PeerPublicView;
import se.sics.gvod.croupier.util.View;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Croupier extends ComponentDefinition {

    private static Logger logger = LoggerFactory.getLogger(Croupier.class);

    Negative<CroupierPort> croupierPort = provides(CroupierPort.class);
    Negative<PeerSamplePort> peerSamplePort = provides(PeerSamplePort.class);
    Positive<VodNetwork> network = requires(VodNetwork.class);
    Positive<Timer> timer = requires(Timer.class);

    private PeerPublicView self;
    View publicView;
    View privateView;
    private boolean firstSuccessfulShuffle = false;
    CroupierConfig config;
    String compName;
    private Map<Integer, Long> shuffleTimes = new HashMap<Integer, Long>();

    private final List<VodAddress> bootstrapNodes;

    public Croupier(CroupierInit init) {

        this.self = init.self;
        this.compName = "(" + self.getAddress().getId() + ", " + self.getOverlayId() + ") ";
        this.config = init.config;
        this.publicView = new View(self, config.rand, config.viewSize);
        this.privateView = new View(self, config.rand, config.viewSize);
        this.bootstrapNodes = new ArrayList<VodAddress>();

//        CroupierStats.addNode(self.getAddress());
        subscribe(handleJoin, croupierPort);
    }

    Handler<CroupierJoin> handleJoin = new Handler<CroupierJoin>() {
        @Override
        public void handle(CroupierJoin join) {
            logger.debug("{} joining using nodes:{}", compName, join.peers.size());

            logger.trace(compName + "initiateShuffle join");
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.shufflePeriod, config.shufflePeriod);
            spt.setTimeoutEvent(new ShuffleCycle(spt, self.getOverlayId()));
            trigger(spt, timer);

            if (!initializeCaches(join.peers)) {
                logger.warn(compName + "No insiders, not shuffling.");
                // I am/(think I am) the first peer
                trigger(new CroupierJoinCompleted(), croupierPort);
                // schedule shuffling
                return;
            }
        }
    };

    private boolean initializeCaches(Set<VodAddress> bootstrapNodes) {
        if (bootstrapNodes == null || bootstrapNodes.isEmpty()) {
            return false;
        }
        for (VodAddress peer : bootstrapNodes) {
            if (self.getAddress().equals(peer)) {
                logger.warn("Trying to bootstrap with myself: {}", self.getAddress());
            } else {
                bootstrapNodes.add(peer);
            }
        }
        return true;
    }

    private VodAddress selectPeerToShuffleWith() {
        VodAddress node = null;
        if (!bootstrapNodes.isEmpty()) {
            node = bootstrapNodes.remove(0);
        } else if (!publicView.isEmpty()) {
            node = publicView.selectPeerToShuffleWith(config.policy, true, 0.75d);
        } else if (!privateView.isEmpty()) {
            node = privateView.selectPeerToShuffleWith(config.policy, true, 0.85d);
        }
        return node;
    }
    
    private void shuffle(int shuffleSize, VodAddress node) {
        if (node == null) {
            return;
        }
        if (self.getAddress().equals(node)) {
            throw new IllegalStateException(" Sending shuffle to myself");
        }

        List<PeerPublicView> publicDescriptors = publicView.selectToSendAtInitiator(shuffleSize, node);
        List<PeerPublicView> privateDescriptors = privateView.selectToSendAtInitiator(shuffleSize, node);

        if (self.getAddress().isOpen()) {
            publicDescriptors.add(self);
        } else {
            privateDescriptors.add(self);
        }

        DescriptorBuffer buffer = new DescriptorBuffer(self.getAddress(),
                publicDescriptors, privateDescriptors);

        ScheduleRetryTimeout st =
                new ScheduleRetryTimeout(config.getRto(),
                config.getRtoRetries(), config.getRtoScale());
        ShuffleMsg.Request msg = new ShuffleMsg.Request(self.getAddress(), node,
                buffer, self.getDescriptor());
        ShuffleMsg.RequestTimeout retryRequest = 
                new ShuffleMsg.RequestTimeout(st, msg, self.getOverlayId());
        TimeoutId id = delegator.doRetry(retryRequest);

        shuffleTimes.put(id.getId(), System.currentTimeMillis());
        logger.debug(compName + "shuffle sent from {} to {} . Id=" + id, self.getId(), node);
    }

    public static class CroupierInit extends Init<Croupier> {

        public final PeerPublicView self;
        public final CroupierConfig config;

        public CroupierInit(CroupierConfig config, PeerPublicView self) {
            this.self = self;
            this.config = config;
        }
    }
}
