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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.address.Address;
import se.sics.gvod.bootstrap.cclient.CaracalPSManagerComp;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerPort;
import se.sics.gvod.bootstrap.server.peermanager.msg.CaracalReady;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.manager.VoDManager;
import se.sics.gvod.net.NatNetworkControl;
import se.sics.gvod.net.NettyInit;
import se.sics.gvod.net.NettyNetwork;
import se.sics.gvod.net.Transport;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.net.events.PortBindRequest;
import se.sics.gvod.net.events.PortBindResponse;
import se.sics.gvod.network.GVoDNetFrameDecoder;
import se.sics.gvod.network.GVoDNetworkSettings;
import se.sics.gvod.timer.Timer;
import se.sics.gvod.timer.java.JavaTimer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.nat.utils.getip.IpAddrStatus;
import se.sics.kompics.nat.utils.getip.ResolveIp;
import se.sics.kompics.nat.utils.getip.ResolveIpPort;
import se.sics.kompics.nat.utils.getip.events.GetIpRequest;
import se.sics.kompics.nat.utils.getip.events.GetIpResponse;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Launcher extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    public static int seed = 1234;
    public static byte[] bseed = new byte[]{1, 2, 3, 4};

    private Component timer;
    private Component resolveIp;
    private Component network;
    private Component manager;
    private Component caracalPSManager;
    private static VoDManager vodManager = null;

    public static CMD firstCmd = null;

    private Address selfAddress;
    private final HostConfiguration.ExecBuilder configBuilder;
    private HostConfiguration config;

    public static VoDManager getInstance() {
        return vodManager;
    }

    public Launcher() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            log.error("threading problem in launcher");
            System.exit(1);
        }
        log.info("init");
        subscribe(handleStart, control);

        GVoDNetFrameDecoder.register();
        GVoDNetworkSettings.checkPreCond();
        GVoDNetworkSettings.registerSerializers();

        configBuilder = new HostConfiguration.ExecBuilder();

        phase1();
    }

    private void phase1() {
        log.info("phase 1 - getting ip");
        timer = create(JavaTimer.class, Init.NONE);
        resolveIp = create(ResolveIp.class, Init.NONE);

        connect(resolveIp.getNegative(Timer.class), timer.getPositive(Timer.class));
        subscribe(handleGetIpResponse, resolveIp.getPositive(ResolveIpPort.class));
    }

    public Handler<GetIpResponse> handleGetIpResponse = new Handler<GetIpResponse>() {
        @Override
        public void handle(GetIpResponse resp) {
            resolveLocalAddress(resp.getBoundIp(), resp.getAddrs());
            phase2();
        }
    };
    
    private void phase2() {
            log.info("phase 2 - ip:{} - binding port:{}", selfAddress.getIp(), selfAddress.getPort());

            network = create(NettyNetwork.class, new NettyInit(seed, true, GVoDNetFrameDecoder.class));
            connect(network.getNegative(Timer.class), timer.getPositive(Timer.class));

            subscribe(handlePsPortBindResponse, network.getPositive(NatNetworkControl.class));
            trigger(Start.event, network.getControl());
            
            BootstrapPortBind.Request pb1 = new BootstrapPortBind.Request(selfAddress, Transport.UDP);
            pb1.setResponse(new BootstrapPortBind.Response(pb1));
            trigger(pb1, network.getPositive(NatNetworkControl.class));
    }

    private void resolveLocalAddress(InetAddress boundIp, List<IpAddrStatus> localAddresses) {
        String configuredLocalIp;
        Integer configuredLocalPort;
        Integer configureLocalId;
        try {
            configuredLocalIp = configBuilder.getIp();
            configuredLocalPort = configBuilder.getPort();
            configureLocalId = configBuilder.getId();
        } catch (GVoDConfigException.Missing ex) {
            log.error("ip config error");
            System.exit(1);
            throw new RuntimeException(ex);
        }
        for (IpAddrStatus addrStatus : localAddresses) {
            if (addrStatus.getAddr().getHostAddress().equals(configuredLocalIp)) {
                selfAddress = new Address(addrStatus.getAddr(), configuredLocalPort, configureLocalId);
                return;
            }
        }
        log.error("configured ip is not within the retrieved ips of the local interfaces");
        System.exit(1);
        throw new RuntimeException();
    }
    
    public Handler<BootstrapPortBind.Response> handlePsPortBindResponse = new Handler<BootstrapPortBind.Response>() {

        @Override
        public void handle(BootstrapPortBind.Response resp) {
            if (resp.getStatus() != PortBindResponse.Status.SUCCESS) {
                log.warn("Couldn't bind to port {}. Either another instance of the program is"
                        + "already running, or that port is being used by a different program. Go"
                        + "to settings to change the port in use. Status: ", resp.getPort(),
                        resp.getStatus());
                System.exit(1);
            } else {
                phase3(resp.boundAddress);
            }
        }
    };

    private void phase3(Address selfAddress) {
        log.info("phase 3 - starting with Address: {}", selfAddress);
        try {
            config = configBuilder.setSelfAddress(selfAddress).setSeed(bseed).finalise();
        } catch (GVoDConfigException.Missing ex) {
            log.error(" bad configuration" + ex.getMessage());
            System.exit(1);
        }
        //TODO
        //should create and start only on open nodes
        caracalPSManager = create(CaracalPSManagerComp.class, new CaracalPSManagerComp.CaracalPSManagerInit(config.getCaracalPSManagerConfig()));
        connect(caracalPSManager.getNegative(Timer.class), timer.getPositive(Timer.class));

        trigger(Start.event, caracalPSManager.control());
        subscribe(handleCaracalReady, caracalPSManager.getPositive(PeerManagerPort.class));
    }

    private Handler<CaracalReady> handleCaracalReady = new Handler<CaracalReady>() {

        @Override
        public void handle(CaracalReady event) {
            unsubscribe(handleCaracalReady, caracalPSManager.getPositive(PeerManagerPort.class));
            phase4();
        }
    };

    private void phase4() {
        manager = create(HostManagerComp.class, new HostManagerComp.HostManagerInit(config, caracalPSManager));
        vodManager = ((HostManagerComp) manager.getComponent()).getVoDManager();

        connect(manager.getNegative(VodNetwork.class), network.getPositive(VodNetwork.class));
        connect(manager.getNegative(Timer.class), timer.getPositive(Timer.class));

        trigger(Start.event, manager.control());

        if (firstCmd != null) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                log.error("threading problem in launcher");
                System.exit(1);
            }
            if (firstCmd.download) {
                if (!vodManager.downloadVideo(firstCmd.fileName, firstCmd.overlayId)) {
                    log.error("bad first command - cannot download - check if library contains file already");
                    System.exit(1);
                }
                Integer videoPort = null;
                do {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        log.error("threading problem in launcher");
                        System.exit(1);
                    }
                    videoPort = vodManager.playVideo(firstCmd.fileName);
                } while (videoPort == null);
                log.info("can play video:{} on port:{}", firstCmd.fileName, videoPort);
            } else {
                if (!vodManager.pendingUpload(firstCmd.fileName)) {
                    log.error("bad first command - cannot upload - check if library contains file");
                }
                if (!vodManager.uploadVideo(firstCmd.fileName, firstCmd.overlayId)) {
                    log.error("bad first command - cannot upload - check if library contains file and you called pendingUpload before");
                }
            }
        }
    }

    public Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            trigger(new GetIpRequest(false), resolveIp.getPositive(ResolveIpPort.class));
        }
    };


    private static class BootstrapPortBind {

        private static class Request extends PortBindRequest {

            public final Address boundAddress;

            public Request(Address address, Transport transport) {
                super(address, transport);
                this.boundAddress = address;
            }
        }

        private static class Response extends PortBindResponse {

            public final Address boundAddress;

            public Response(Request req) {
                super(req);
                this.boundAddress = req.boundAddress;
            }
        }
    }

    public static class CMD {

        public final boolean download;
        public final String fileName;
        public final int overlayId;

        public CMD(boolean download, String fileName, int overlayId) {
            this.download = download;
            this.fileName = fileName;
            this.overlayId = overlayId;
        }
    }

}
