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
package se.sics.gvod.simulation;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.common.network.filters.NodeIdFilter;
import se.sics.gvod.common.util.ConfigException;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.simulation.cmd.system.StartBSCmd;
import se.sics.gvod.simulation.cmd.system.StartVodPeerCmd;
import se.sics.gvod.simulation.cmd.system.StopBSCmd;
import se.sics.gvod.simulation.cmd.system.StopVodPeerCmd;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.Stopped;
import se.sics.kompics.p2p.experiment.dsl.events.TerminateExperiment;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimManagerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(SimManagerComp.class);

    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Positive<VodExperiment> experiment = requires(VodExperiment.class);

    private HashMap<Integer, Component> systemComp;

    public SimManagerComp(SimManagerInit init) {
        log.debug("init");
        systemComp = new HashMap<Integer, Component>();

        subscribe(handleStartBS, experiment);
        subscribe(handleStopBS, experiment);
        subscribe(handleStartVodPeer, experiment);
        subscribe(handleStopVodPeer, experiment);
        subscribe(handleTerminate, experiment);
    }

    public Handler<StartBSCmd> handleStartBS = new Handler<StartBSCmd>() {

        @Override
        public void handle(final StartBSCmd start) {
            log.info("bootstrap server - id {} - starting...", start.id);
            try {
                se.sics.gvod.bootstrap.server.HostConfiguration bsConfig = 
                        new se.sics.gvod.bootstrap.server.HostConfiguration.Builder("bootstrap.conf").setId(start.id).setSeed(new byte[]{1,2,3,4}).finalise();
                
                final Component vodPeerHost = create(se.sics.gvod.bootstrap.server.HostManagerComp.class,
                        new se.sics.gvod.bootstrap.server.HostManagerInit(bsConfig));
                systemComp.put(start.id, vodPeerHost);

                connect(vodPeerHost.getNegative(VodNetwork.class), network, new NodeIdFilter(start.id));
                connect(vodPeerHost.getNegative(Timer.class), timer);

                trigger(Start.event, vodPeerHost.control());
            } catch (ConfigException.Missing ex) {
                log.error("error loading bootstrap server configuration");
                throw new RuntimeException(ex);
            }
        }
    };

    public Handler<StopBSCmd> handleStopBS = new Handler<StopBSCmd>() {

        @Override
        public void handle(final StopBSCmd stop) {
            if (!systemComp.containsKey(stop.id)) {
                log.error("bootstrap server - id {} - cannot stop that which is not started", stop.id);
                throw new RuntimeException("cannot stop that which is not started");
            }
            log.info("bootstrap server - id {} - stopping...", stop.id);

            final Component vodPeerHost = systemComp.remove(stop.id);
            disconnect(vodPeerHost.getNegative(VodNetwork.class), network);
            disconnect(vodPeerHost.getNegative(Timer.class), timer);
            
            Handler<Stopped> handleStopped = new Handler<Stopped>() {

                @Override
                public void handle(Stopped stopped) {
                    log.debug("bootstrap server - id {} - cleaning", stop.id);
                    destroy(vodPeerHost);
                    unsubscribe(this, control);
                }
            };
            
            subscribe(handleStopped, control);
            trigger(Stop.event, vodPeerHost.control());
        }
    }; 
    
    public Handler<StartVodPeerCmd> handleStartVodPeer = new Handler<StartVodPeerCmd>() {

        @Override
        public void handle(final StartVodPeerCmd start) {
            log.info("vod peer - id {} - starting...", start.id);
            try {
                se.sics.gvod.system.HostConfiguration bsConfig = new se.sics.gvod.system.HostConfiguration.Builder("vod.conf").setId(start.id).setSeed(new byte[]{1,2,3,4}).finalise();

                final Component vodPeerHost = create(se.sics.gvod.system.HostManagerComp.class,
                        new se.sics.gvod.system.HostManagerInit(bsConfig));
                systemComp.put(start.id, vodPeerHost);

                connect(vodPeerHost.getNegative(VodNetwork.class), network, new NodeIdFilter(start.id));
                connect(vodPeerHost.getNegative(Timer.class), timer);

                trigger(Start.event, vodPeerHost.control());
            } catch (ConfigException.Missing ex) {
                log.error("error loading vod peer configuration");
                throw new RuntimeException(ex);
            }
        }
    };

    public Handler<StopVodPeerCmd> handleStopVodPeer = new Handler<StopVodPeerCmd>() {

        @Override
        public void handle(final StopVodPeerCmd stop) {
            if (!systemComp.containsKey(stop.id)) {
                log.error("vod peer - id {} - cannot stop that which is not started", stop.id);
                throw new RuntimeException("cannot stop that which is not started");
            }
            log.info("vod peer - id {} - stopping...", stop.id);

            final Component vodPeerHost = systemComp.remove(stop.id);
            disconnect(vodPeerHost.getNegative(VodNetwork.class), network);
            disconnect(vodPeerHost.getNegative(Timer.class), timer);
            
            Handler<Stopped> handleStopped = new Handler<Stopped>() {

                @Override
                public void handle(Stopped stopped) {
                    log.debug("vodPeer - id {} - cleaning", stop.id);
                    destroy(vodPeerHost);
                    unsubscribe(this, control);
                }
            };
            
            subscribe(handleStopped, control);
            trigger(Stop.event, vodPeerHost.control());
        }
    }; 
    
    Handler<TerminateExperiment> handleTerminate = new Handler<TerminateExperiment>() {
        @Override
        public void handle(TerminateExperiment event) {
            log.info("terminate experiment.");
            Kompics.forceShutdown();
        }
    };
}
