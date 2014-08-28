
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
package se.sics.gvod.cserver;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.operations.CaracalMsg;
import se.sics.gvod.bootstrap.common.PeerManagerPort;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.cserver.operations.Operation;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BCServerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(BCServerComp.class);

    private Positive<Network> network = requires(Network.class);
    private Positive<PeerManagerPort> peerManager = requires(PeerManagerPort.class);

    private final BCServerConfig config;

    private final Map<UUID, Operation<? extends GvodMsg.Request>> pendingOps;
    private final Map<UUID, UUID> pendingCaracalOps;

    public BCServerComp(BCServerInit init) {
        log.debug("init");
        this.config = init.config;
        this.pendingOps = new HashMap<UUID, Operation<? extends GvodMsg.Request>>();
        this.pendingCaracalOps = new HashMap<UUID, UUID>();
        
        subscribe(handleCaracalResponse, network);
    }
    
    public Handler<CaracalMsg> handleCaracalResponse = new Handler<CaracalMsg>() {

        @Override
        public void handle(CaracalMsg resp) {
            log.debug("{} received {}", new Object[]{config.self, resp.op});
            UUID opId = pendingCaracalOps.get(resp.op.id);
            if(opId == null) {
                log.debug("{} dropping {}", new Object[]{config.self, resp.op});
            }
            pendingOps.get(opId).handle(resp.op);
        }
    };
}
