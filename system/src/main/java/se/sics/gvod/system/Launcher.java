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

import com.google.common.util.concurrent.SettableFuture;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.cclient.CaracalPSManagerComp;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerPort;
import se.sics.gvod.bootstrap.server.peermanager.msg.CaracalReady;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.manager.VoDManager;
import se.sics.gvod.manager.util.FileStatus;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Launcher extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    public static int seed = 1234;
    public static byte[] bseed = new byte[]{1, 2, 3, 4};

    private Component timer;
    private Component network;
    private Component manager;
    private Component caracalPSManager;
    private static VoDManager vodManager = null;

    public static CMD firstCmd = null;

    private DecoratedAddress selfAddress;
    private final HostConfiguration.ExecBuilder configBuilder;
    private HostConfiguration config;
    private final String logPrefix;

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
        
        GVoDSystemSerializerSetup.oneTimeSetup();
        
        subscribe(handleStart, control);

        configBuilder = new HostConfiguration.ExecBuilder();
        try {
            InetAddress configuredLocalIp = InetAddress.getByName(configBuilder.getIp());
            Integer configuredLocalPort = configBuilder.getPort();
            Integer configureLocalId = configBuilder.getId();
            selfAddress = new DecoratedAddress(new BasicAddress(configuredLocalIp, configuredLocalPort, configureLocalId));
        } catch (UnknownHostException ex) {
            throw new RuntimeException("bad local ip", ex);
        } catch (GVoDConfigException.Missing ex) {
            throw new RuntimeException("missing addressparts", ex);
        }
        this.logPrefix = selfAddress.toString();

        timer = create(JavaTimer.class, Init.NONE);
        network = create(NettyNetwork.class, new NettyInit(selfAddress));

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

        subscribe(handleCaracalReady, caracalPSManager.getPositive(PeerManagerPort.class));
    }

    private Handler<CaracalReady> handleCaracalReady = new Handler<CaracalReady>() {

        @Override
        public void handle(CaracalReady event) {
//            unsubscribe(handleCaracalReady, caracalPSManager.getPositive(PeerManagerPort.class));
            phase4();
        }
    };

    private void phase4() {
        manager = create(HostManagerComp.class, new HostManagerComp.HostManagerInit(config, caracalPSManager));
        vodManager = ((HostManagerComp) manager.getComponent()).getVoDManager();

        connect(manager.getNegative(Network.class), network.getPositive(Network.class));
        connect(manager.getNegative(Timer.class), timer.getPositive(Timer.class));

        trigger(Start.event, manager.control());

        if (firstCmd != null) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                log.error("threading problem in launcher");
                System.exit(1);
            }
            try {
                if (firstCmd.download) {
                    SettableFuture<Map<String, FileStatus>> libFuture = SettableFuture.create();
                    vodManager.getFiles(libFuture);
                    log.info("{} getting library", logPrefix);
                    libFuture.get();
                    SettableFuture<Boolean> downloadFuture = SettableFuture.create();
                    vodManager.downloadVideo(firstCmd.fileName, firstCmd.overlayId, downloadFuture);
                    log.info("{} first command", logPrefix);
                    if (!downloadFuture.get()) {
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
                        SettableFuture<Integer> playFuture = SettableFuture.create();
                        vodManager.playVideo(firstCmd.fileName, firstCmd.overlayId, playFuture);
                        videoPort = playFuture.get();
                    } while (videoPort == null);
                    log.info("can play video:{} on port:{}", firstCmd.fileName, videoPort);

                } else {
                    SettableFuture<Map<String, FileStatus>> libFuture = SettableFuture.create();
                    vodManager.getFiles(libFuture);
                    log.info("{} getting library", logPrefix);
                    libFuture.get();
                    
                    SettableFuture<Boolean> pendingUpFuture = SettableFuture.create();
                    vodManager.pendingUpload(firstCmd.fileName, pendingUpFuture);
                    log.info("{} first command", logPrefix);
                    if (!pendingUpFuture.get()) {
                        log.error("bad first command - cannot upload - check if library contains file");
                        System.exit(1);
                    }
                    SettableFuture<Boolean> uploadFuture = SettableFuture.create();
                    vodManager.uploadVideo(firstCmd.fileName, firstCmd.overlayId, uploadFuture);
                    if (!uploadFuture.get()) {
                        log.error("bad first command - cannot upload - check if library contains file and you called pendingUpload before");
                    }
                }
            } catch (InterruptedException ex) {
                log.error("future internal logic error");
                System.exit(1);
            } catch (ExecutionException ex) {
                log.error("future internal logic error");
                System.exit(1);
            }
        }
    }

    public Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("starting...");
        }
    };

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