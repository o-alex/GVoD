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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.client.BootstrapComp;
import se.sics.gvod.bootstrap.client.BootstrapInit;
import se.sics.gvod.bootstrap.client.BootstrapPort;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HostManagerComp extends ComponentDefinition {
    private static final Logger log = LoggerFactory.getLogger(HostManagerComp.class);

    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);

    private Component vod;
    private Component bootstrapClient;
    

    public HostManagerComp(HostManagerInit init) {
        log.debug("init");
        this.vod = create(VoDComp.class, new VoDInit());
        this.bootstrapClient = create(BootstrapComp.class, new BootstrapInit());

        connect(vod.getNegative(VodNetwork.class), network);
        connect(bootstrapClient.getNegative(VodNetwork.class), network);
        connect(vod.getNegative(BootstrapPort.class), bootstrapClient.getPositive(BootstrapPort.class));
    }
}
