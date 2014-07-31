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

package se.sics.gvod.bootstrap.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.client.msg.BootstrapMsg;
import se.sics.gvod.bootstrap.common.msg.BootstrapNetMsg;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapClientComp extends ComponentDefinition {
    private static final Logger log = LoggerFactory.getLogger(BootstrapClientComp.class);
    
    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Negative<BootstrapClientPort> bootstrap = provides(BootstrapClientPort.class);

    private BootstrapClientConfig config;
    
    public BootstrapClientComp(BootstrapClientInit init) {
        log.debug("init");
        this.config = init.config;
        
        subscribe(handleBootstrapRequest, bootstrap);
        subscribe(handleBootstrapNetResponse, network);
    }
    
    public Handler<BootstrapMsg.Request> handleBootstrapRequest = new Handler<BootstrapMsg.Request>() {

        @Override
        public void handle(BootstrapMsg.Request req) {
            log.trace("received {}", req.toString());
            BootstrapNetMsg.Request netReq = new BootstrapNetMsg.Request(
                    new VodAddress(config.self, -1),
                    new VodAddress(config.server, -1));
            log.debug("sending {}", netReq.toString());
            trigger(netReq, network);
        }
    };
    
    public Handler<BootstrapNetMsg.Response> handleBootstrapNetResponse = new Handler<BootstrapNetMsg.Response>() {

        @Override
        public void handle(BootstrapNetMsg.Response netResp) {
            log.debug("received {}", netResp.toString());
        }
    };
}
