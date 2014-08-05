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

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.client.BootstrapClientPort;
import se.sics.gvod.bootstrap.common.msg.BootstrapMsg;
import se.sics.gvod.net.VodNetwork;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VoDComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(VoDComp.class);

    Positive<VodNetwork> network = requires(VodNetwork.class);
    Positive<BootstrapClientPort> bootstrap = requires(BootstrapClientPort.class);
    
    public VoDComp(VoDInit init) {
        log.debug("init");
        
        subscribe(handleStart, control);
    }
    
    public Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            BootstrapMsg.Request req = new BootstrapMsg.Request(UUID.randomUUID());
            log.debug("sending {}", req.toString());
            trigger(req, bootstrap);
        }
    };
}
