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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.address.Address;
import se.sics.gvod.bootstrap.cclient.CaracalPSManagerComp;
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
import se.sics.gvod.system.vodmngr.VoDManagerImpl;
import se.sics.gvod.timer.Timer;
import se.sics.gvod.timer.java.JavaTimer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
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
    private VoDManager vodManager;

    private Address selfAddress;
    private final HostConfiguration.ExecBuilder config;

    public Launcher() {
        log.info("init");
        subscribe(handleStart, control);

        config = new HostConfiguration.ExecBuilder();

        phase1();
    }

    private void phase1() {
        log.info("phase 1 - getting ip");
        timer = create(JavaTimer.class, Init.NONE);
        resolveIp = create(ResolveIp.class, Init.NONE);

        connect(resolveIp.getNegative(Timer.class), timer.getPositive(Timer.class));
        subscribe(handleGetIpResponse, resolveIp.getPositive(ResolveIpPort.class));
    }

    private void phase2(InetAddress selfIp) {
        try {
            log.info("phase 2 - ip:{} - binding port:{}", selfIp, config.getPort());
            selfAddress = new Address(selfIp, config.getPort(), config.getId());

            network = create(NettyNetwork.class, new NettyInit(seed, true, GVoDNetFrameDecoder.class));
            connect(network.getNegative(Timer.class), timer.getPositive(Timer.class));

            subscribe(handlePsPortBindResponse, network.getPositive(NatNetworkControl.class));
            trigger(Start.event, network.getControl());
        } catch (GVoDConfigException.Missing ex) {
            throw new RuntimeException(ex);
        }
    }

    private void phase3(Address selfAddress) {
        log.info("phase 3 - starting with Address: {}", selfAddress);
        try {
            HostConfiguration hostConfig = config.setSelfAddress(selfAddress).setSeed(bseed).finalise();
            //TODO
            //should create and start only on open nodes
            Component peerManager = create(CaracalPSManagerComp.class, new CaracalPSManagerComp.CaracalPSManagerInit(hostConfig.getCaracalPSManagerConfig()));
            manager = create(HostManagerComp.class, new HostManagerComp.HostManagerInit(hostConfig, peerManager));
            vodManager = ((HostManagerComp) manager.getComponent()).getVoDManager();
            connect(manager.getNegative(VodNetwork.class), network.getPositive(VodNetwork.class));
            connect(manager.getNegative(Timer.class), timer.getPositive(Timer.class));

            trigger(Start.event, peerManager.control());
            trigger(Start.event, manager.control());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            phase4();
        } catch (GVoDConfigException.Missing ex) {
            throw new RuntimeException(ex);
        }
    }

    private void phase4() {
        int overlayId = 10;
        String videoName = "video2.mp4";
        String libDir = "/Users/Alex/Documents/Work/Code/GVoD/video-catalog/node1";
        log.info("{} libDir:{}", selfAddress, libDir);
        try {
            File f = new File(libDir);
            f.delete();
            f.mkdir();
            File videoFile = new File(libDir + File.separator + videoName);
            videoFile.createNewFile();
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(videoFile)));
            for (int i = 0; i < 10000; i++) {
                writer.write("abc" + i + "\n");
            }
            writer.flush();
            writer.close();
            ((VoDManagerImpl)vodManager).loadLibrary();
            if(!vodManager.pendingUpload(videoName)) {
                throw new RuntimeException();
            }
            if(!vodManager.uploadVideo(videoName, overlayId)) {
                throw new RuntimeException();
            }

//            vodManager.downloadVideo(new DownloadFileInfo(10, libDir, videoName));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            trigger(new GetIpRequest(false), resolveIp.getPositive(ResolveIpPort.class));
        }
    };

    public Handler<GetIpResponse> handleGetIpResponse = new Handler<GetIpResponse>() {
        @Override
        public void handle(GetIpResponse resp) {
            phase2(resp.getIpAddress());
            BootstrapPortBind.Request pb1 = new BootstrapPortBind.Request(selfAddress, Transport.UDP);
            pb1.setResponse(new BootstrapPortBind.Response(pb1));
            trigger(pb1, network.getPositive(NatNetworkControl.class));
        }
    };

    public Handler<BootstrapPortBind.Response> handlePsPortBindResponse = new Handler<BootstrapPortBind.Response>() {

        @Override
        public void handle(BootstrapPortBind.Response resp) {
            if (resp.getStatus() != PortBindResponse.Status.SUCCESS) {
                log.warn("Couldn't bind to port {}. Either another instance of the program is"
                        + "already running, or that port is being used by a different program. Go"
                        + "to settings to change the port in use. Status: ", resp.getPort(),
                        resp.getStatus());
                Kompics.shutdown();
                System.exit(-1);
            } else {
                phase3(resp.boundAddress);
            }
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
}
