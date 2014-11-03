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

import se.sics.gvod.system.vodmngr.VoDManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.client.BootstrapClientComp;
import se.sics.gvod.bootstrap.client.BootstrapClientInit;
import se.sics.gvod.bootstrap.client.BootstrapClientPort;
import se.sics.gvod.bootstrap.server.BootstrapServerComp;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerPort;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.gvod.manager.VoDManager;
import se.sics.gvod.net.VodAddress.NatType;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.system.vod.VoDComp;
import se.sics.gvod.system.vod.VoDInit;
import se.sics.gvod.system.vod.VoDPort;
import se.sics.gvod.system.vodmngr.VoDManagerConfig;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HostManagerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(HostManagerComp.class);

    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);

    private Component vodMngr;
    private Component vod;
    private Component bootstrapClient;
    private Component bootstrapServer;
    private Component peerManager;
    private Component globalCroupier;

    private final HostConfiguration config;

    public HostManagerComp(HostManagerInit init) {
        log.debug("starting... - self {}, bootstrap server {}",
                new Object[]{init.config.self.toString(), init.config.server.toString()});
        this.config = init.config;

        try {
            this.vodMngr = create(VoDManagerImpl.class,new VoDManagerImpl.VoDManagerInit(new VoDManagerConfig(config.libDir)));
            this.vod = create(VoDComp.class, new VoDInit(config.getVoDConfiguration().finalise()));
            this.bootstrapClient = create(BootstrapClientComp.class, new BootstrapClientInit(config.getBootstrapClientConfig().finalise()));

            log.info("{} node NAT is {}", config.self, config.self.getNatType());
            if (config.self.getNatType().equals(NatType.OPEN)) {
                bootstrapServer = create(BootstrapServerComp.class, new BootstrapServerComp.BootstrapServerInit(config.getBootstrapServerConfig()));
                peerManager = init.peerManager;

                connect(bootstrapServer.getNegative(VodNetwork.class), network);
                connect(bootstrapServer.getNegative(PeerManagerPort.class), peerManager.getPositive(PeerManagerPort.class));
            } else {
                bootstrapServer = null;
                peerManager = null;
            }

            connect(vodMngr.getNegative(VoDPort.class), vod.getPositive(VoDPort.class));
            connect(vod.getNegative(VodNetwork.class), network);
            connect(vod.getNegative(BootstrapClientPort.class), bootstrapClient.getPositive(BootstrapClientPort.class));
            connect(vod.getNegative(Timer.class), timer);
            connect(bootstrapClient.getNegative(VodNetwork.class), network);
            connect(bootstrapClient.getNegative(Timer.class), timer);

        } catch (GVoDConfigException.Missing ex) {
            throw new RuntimeException(ex);
        }
    }

    public VoDManager getVoDManager() {
        VoDManagerImpl vodM = (VoDManagerImpl) vodMngr.getComponent();
        return vodM;
    }

    public static class HostManagerInit extends Init<HostManagerComp> {

        public final HostConfiguration config;
        public final Component peerManager;

        public HostManagerInit(HostConfiguration config, Component peerManager) {
            this.config = config;
            this.peerManager = peerManager;
        }
    }
}
