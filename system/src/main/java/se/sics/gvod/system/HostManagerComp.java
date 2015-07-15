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
import se.sics.gvod.manager.VoDManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.client.BootstrapClientComp;
import se.sics.gvod.bootstrap.client.BootstrapClientInit;
import se.sics.gvod.bootstrap.client.BootstrapClientPort;
import se.sics.gvod.bootstrap.server.BootstrapServerComp;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerPort;
import se.sics.gvod.common.utility.UtilityUpdatePort;
import se.sics.gvod.core.VoDComp;
import se.sics.gvod.core.VoDInit;
import se.sics.gvod.core.VoDPort;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.util.traits.Nated;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HostManagerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(HostManagerComp.class);

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private Component vodMngr;
    private Component vod;
    private Component bootstrapClient;
    private Component bootstrapServer;
    private Component peerManager;
    private Component globalCroupier;

    private final HostManagerConfig config;

    public HostManagerComp(HostManagerInit init) {
        log.debug("starting... - self {}, bootstrap server {}",
                new Object[]{init.config.getSelf(), init.config.getCaracalClient()});
        this.config = init.config;

        this.vodMngr = create(VoDManagerImpl.class, new VoDManagerImpl.VoDManagerInit(config.getVoDManagerConfig()));
        init.gvodSyncIFuture.set(vodMngr.getComponent());
        this.vod = create(VoDComp.class, new VoDInit(config.getVoDConfig()));
        this.bootstrapClient = create(BootstrapClientComp.class, new BootstrapClientInit(config.getBootstrapClientConfig()));

        log.info("{} node is Natted:{}", config.getSelf(), config.getSelf().hasTrait(Nated.class));
        if (!config.getSelf().hasTrait(Nated.class)) {
            bootstrapServer = create(BootstrapServerComp.class, new BootstrapServerComp.BootstrapServerInit(config.getBootstrapServerConfig()));
            peerManager = init.peerManager;

            connect(bootstrapServer.getNegative(Network.class), network);
            connect(bootstrapServer.getNegative(PeerManagerPort.class), peerManager.getPositive(PeerManagerPort.class));
        } else {
            bootstrapServer = null;
            peerManager = null;
        }

        connect(vodMngr.getNegative(VoDPort.class), vod.getPositive(VoDPort.class));
        connect(vod.getNegative(Network.class), network);
        connect(vod.getNegative(BootstrapClientPort.class), bootstrapClient.getPositive(BootstrapClientPort.class));
        connect(vod.getNegative(Timer.class), timer);
        connect(bootstrapClient.getNegative(Network.class), network);
        connect(bootstrapClient.getNegative(Timer.class), timer);

        connect(bootstrapClient.getNegative(UtilityUpdatePort.class), vod.getPositive(UtilityUpdatePort.class));
        connect(vodMngr.getNegative(UtilityUpdatePort.class), vod.getPositive(UtilityUpdatePort.class));
    }

    public static class HostManagerInit extends Init<HostManagerComp> {

        public final HostManagerConfig config;
        public final Component peerManager;
        public final SettableFuture gvodSyncIFuture;

        public HostManagerInit(HostManagerConfig config, Component peerManager, SettableFuture gvodSyncIFuture) {
            this.config = config;
            this.peerManager = peerManager;
            this.gvodSyncIFuture = gvodSyncIFuture;
        }
    }
}
