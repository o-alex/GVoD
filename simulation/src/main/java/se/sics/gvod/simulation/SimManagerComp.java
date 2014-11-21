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

import com.typesafe.config.ConfigFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.address.Address;
import se.sics.gvod.bootstrap.server.BootstrapServerComp;
import se.sics.gvod.bootstrap.server.BootstrapServerConfig;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerPort;
import se.sics.gvod.bootstrap.server.simulation.SimPMComp;
import se.sics.gvod.bootstrap.server.simulation.SimPMConfig;
import se.sics.gvod.common.network.filters.NodeIdFilter;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.manager.VoDManager;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.simulation.cmd.operations.DownloadVideoCmd;
import se.sics.gvod.simulation.cmd.operations.UploadVideoCmd;
import se.sics.gvod.simulation.cmd.system.StartBSCmd;
import se.sics.gvod.simulation.cmd.system.StartVodPeerCmd;
import se.sics.gvod.system.HostConfiguration;
import se.sics.gvod.system.HostManagerComp;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.p2p.experiment.dsl.events.TerminateExperiment;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimManagerComp extends ComponentDefinition {
    private final static int port = 11122;
    private final static byte[] seed = new byte[]{1,2,3,4};

    private static final Logger log = LoggerFactory.getLogger(SimManagerComp.class);

    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Positive<VodExperiment> experiment = requires(VodExperiment.class);

    private HashMap<Integer, Component> systemComp;

    public SimManagerComp(SimManagerInit init) {
        log.debug("init");
        systemComp = new HashMap<Integer, Component>();

        subscribe(handleStartBS, experiment);
//        subscribe(handleStopBS, experiment);
        subscribe(handleStartVodPeer, experiment);
//        subscribe(handleStopVodPeer, experiment);
        subscribe(handleUploadVideo, experiment);
        subscribe(handleDownloadVideo, experiment);
        subscribe(handleTerminate, experiment);
    }

    public Handler<StartBSCmd> handleStartBS = new Handler<StartBSCmd>() {

        @Override
        public void handle(final StartBSCmd start) {
            log.info("bootstrap server - id {} - starting...", start.id);
            try {
                VodAddress selfAddress = new VodAddress(new Address(InetAddress.getLocalHost(), port, start.id), -1);
                Component bootstrapServer = create(BootstrapServerComp.class, new BootstrapServerComp.BootstrapServerInit(new BootstrapServerConfig(ConfigFactory.load(),selfAddress, seed)));
                Component peerManager = create(SimPMComp.class, new SimPMComp.SimPMInit(new SimPMConfig.Builder(ConfigFactory.load(), seed, selfAddress.getPeerAddress()).finalise()));

                connect(bootstrapServer.getNegative(VodNetwork.class), network);
                connect(bootstrapServer.getNegative(PeerManagerPort.class), peerManager.getPositive(PeerManagerPort.class));

                trigger(Start.event, bootstrapServer.control());
                trigger(Start.event, peerManager.control());
            } catch (GVoDConfigException.Missing ex) {
                throw new RuntimeException(ex);
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
        }
    };

//    public Handler<StopBSCmd> handleStopBS = new Handler<StopBSCmd>() {
//
//        @Override
//        public void handle(final StopBSCmd stop) {
//            if (!systemComp.containsKey(stop.id)) {
//                log.error("bootstrap server - id {} - cannot stop that which is not started", stop.id);
//                throw new RuntimeException("cannot stop that which is not started");
//            }
//            log.info("bootstrap server - id {} - stopping...", stop.id);
//
//            final Component vodPeerHost = systemComp.remove(stop.id);
//            disconnect(vodPeerHost.getNegative(VodNetwork.class), network);
//            disconnect(vodPeerHost.getNegative(Timer.class), timer);
//
//            Handler<Stopped> handleStopped = new Handler<Stopped>() {
//
//                @Override
//                public void handle(Stopped stopped) {
//                    log.debug("bootstrap server - id {} - cleaning", stop.id);
//                    destroy(vodPeerHost);
//                    unsubscribe(this, control);
//                }
//            };
//
//            subscribe(handleStopped, control);
//            trigger(Stop.event, vodPeerHost.control());
//        }
//    };

    public Handler<StartVodPeerCmd> handleStartVodPeer = new Handler<StartVodPeerCmd>() {

        @Override
        public void handle(final StartVodPeerCmd start) {
            log.info("vod peer - id {} - starting...", start.id);
            try {
                VodAddress bootstrapServer = new VodAddress(new Address(InetAddress.getLocalHost(), port, 0), -1);
                VodAddress selfAddress = new VodAddress(new Address(InetAddress.getLocalHost(), port, start.id), -1);
                HostConfiguration hostConfig = new HostConfiguration.SimulationBuilder().setSeed(seed).setId(start.id).setLibDir(start.libDir).finalise();
                Component peerManager = create(SimPMComp.class, new SimPMComp.SimPMInit(new SimPMConfig.Builder(ConfigFactory.load(), seed, selfAddress.getPeerAddress()).finalise()));
                Component vodPeerHost = create(HostManagerComp.class, new HostManagerComp.HostManagerInit(hostConfig, peerManager));
                systemComp.put(start.id, vodPeerHost);

                connect(vodPeerHost.getNegative(VodNetwork.class), network, new NodeIdFilter(start.id));
                connect(vodPeerHost.getNegative(Timer.class), timer);

                trigger(Start.event, vodPeerHost.control());
                trigger(Start.event, peerManager.control());
            } catch (GVoDConfigException.Missing ex) {
                log.error("error loading vod peer configuration");
                throw new RuntimeException(ex);
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
        }
    };

//    public Handler<StopVodPeerCmd> handleStopVodPeer = new Handler<StopVodPeerCmd>() {
//
//        @Override
//        public void handle(final StopVodPeerCmd stop) {
//            if (!systemComp.containsKey(stop.id)) {
//                log.error("vod peer - id {} - cannot stop that which is not started", stop.id);
//                throw new RuntimeException("cannot stop that which is not started");
//            }
//            log.info("vod peer - id {} - stopping...", stop.id);
//
//            final Component vodPeerHost = systemComp.remove(stop.id);
//            disconnect(vodPeerHost.getNegative(VodNetwork.class), network);
//            disconnect(vodPeerHost.getNegative(Timer.class), timer);
//
//            Handler<Stopped> handleStopped = new Handler<Stopped>() {
//
//                @Override
//                public void handle(Stopped stopped) {
//                    log.debug("vodPeer - id {} - cleaning", stop.id);
//                    destroy(vodPeerHost);
//                    unsubscribe(this, control);
//                }
//            };
//
//            subscribe(handleStopped, control);
//            trigger(Stop.event, vodPeerHost.control());
//        }
//    };

    public Handler<UploadVideoCmd> handleUploadVideo = new Handler<UploadVideoCmd>() {

        @Override
        public void handle(UploadVideoCmd cmd) {
            log.trace("{}", cmd);
            Component node = systemComp.get(cmd.nodeId);
            if (node == null) {
                return;
            }
            se.sics.gvod.system.HostManagerComp nodeHost = (se.sics.gvod.system.HostManagerComp) node.getComponent();
            VoDManager vodMngr = nodeHost.getVoDManager();
            vodMngr.reloadLibrary();
            if(!vodMngr.pendingUpload(cmd.videoName)) {
                throw new RuntimeException();
            }
            if(!vodMngr.uploadVideo(cmd.videoName, cmd.overlayId)) {
                throw new RuntimeException();
            }
        }
    };

    public Handler<DownloadVideoCmd> handleDownloadVideo = new Handler<DownloadVideoCmd>() {

        @Override
        public void handle(DownloadVideoCmd cmd) {
            log.trace("{}", cmd);
            Component node = systemComp.get(cmd.nodeId);
            if (node == null) {
                return;
            }
            se.sics.gvod.system.HostManagerComp nodeHost = (se.sics.gvod.system.HostManagerComp) node.getComponent();
            VoDManager vodMngr = nodeHost.getVoDManager();
            vodMngr.downloadVideo(cmd.videoName, cmd.overlayId);
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
