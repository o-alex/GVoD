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

import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.address.Address;
import se.sics.gvod.common.util.ConfigException;
import se.sics.gvod.net.NatNetworkControl;
import se.sics.gvod.net.NettyInit;
import se.sics.gvod.net.NettyNetwork;
import se.sics.gvod.net.Transport;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.net.events.PortBindRequest;
import se.sics.gvod.net.events.PortBindResponse;
import se.sics.gvod.network.GVoDNetFrameDecoder;
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
    public static byte[] bseed = new byte[]{1,2,3,4};
    public static int port = 23456;
    public static int id = 123;

    private Component timer;
    private Component resolveIp;
    private Component network;
    private Component manager;

    private Address selfAddress;

    public Launcher() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        subscribe(handleStart, control);

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
        log.info("phase 2 - ip:{} - binding port:{}", selfIp, port);
        selfAddress = new Address(selfIp, port, id);

        network = create(NettyNetwork.class, new NettyInit(seed, true, GVoDNetFrameDecoder.class));
        connect(network.getNegative(Timer.class), timer.getPositive(Timer.class));

        subscribe(handlePsPortBindResponse, network.getPositive(NatNetworkControl.class));
        trigger(Start.event, network.getControl());
    }
    
    private void phase3() {
        log.info("phase 3 - starting with Address: {}", selfAddress);
        HostConfiguration hostConfig = null;
        try {
            hostConfig = new HostConfiguration.ExecBuilder("server.conf").setSelfAddress(selfAddress).setSeed(bseed).finalise();
        } catch (ConfigException.Missing ex) {
            throw new RuntimeException(ex);
        }
        manager = create(HostManagerComp.class, new HostManagerInit(hostConfig));

        connect(manager.getNegative(VodNetwork.class), network.getPositive(VodNetwork.class));
        connect(manager.getNegative(Timer.class), timer.getPositive(Timer.class));
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
            PortBindRequest pb1 = new PortBindRequest(selfAddress, Transport.UDP);
            pb1.setResponse(new PsPortBindResponse(pb1));
            trigger(pb1, network.getPositive(NatNetworkControl.class));
        }
    };

    public Handler<PsPortBindResponse> handlePsPortBindResponse = new Handler<PsPortBindResponse>() {

        @Override
        public void handle(PsPortBindResponse event) {
            if (event.getStatus() != PortBindResponse.Status.SUCCESS) {
                log.warn("Couldn't bind to port {}. Either another instance of the program is"
                        + "already running, or that port is being used by a different program. Go"
                        + "to settings to change the port in use. Status: ", event.getPort(),
                        event.getStatus());
                Kompics.shutdown();
                System.exit(-1);
            } else {
                phase3();
            }
        }
    };

    public static class PsPortBindResponse extends PortBindResponse {

        public PsPortBindResponse(PortBindRequest request) {
            super(request);
        }
    }
}