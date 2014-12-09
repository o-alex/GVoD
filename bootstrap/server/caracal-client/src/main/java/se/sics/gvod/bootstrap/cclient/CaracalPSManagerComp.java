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
package se.sics.gvod.bootstrap.cclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.CoreSerializer;
import se.sics.caracaldb.global.ForwardMessage;
import se.sics.caracaldb.operations.CaracalMsg;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.caracaldb.operations.PutRequest;
import se.sics.caracaldb.operations.PutResponse;
import se.sics.caracaldb.operations.RangeQuery;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerPort;
import se.sics.gvod.common.util.GVoDConfigException;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CaracalPSManagerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(CaracalPSManagerComp.class);

    private Negative<PeerManagerPort> peerManager = provides(PeerManagerPort.class);
    
    private Component network;
    private Component caracal;

    private final CaracalPSManagerConfig config;

    public CaracalPSManagerComp(CaracalPSManagerInit init) {
        registerSerializers();
        try {
            this.config = init.config;
            log.info("{} connecting components", config.selfAddress);
            
            network = create(NettyNetwork.class, new NettyInit(config.selfAddress));
            caracal = create(CaracalPeerStoreComp.class, new CaracalPeerStoreComp.CaracalPeerStoreInit(config.getCaracalPeerStoreConfig()));
            
            connect(caracal.getNegative(Network.class), network.getPositive(Network.class));
            connect(peerManager, caracal.getPositive(PeerManagerPort.class));
        } catch (GVoDConfigException.Missing ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void registerSerializers() {
        Serializers.register(CoreSerializer.LOOKUP.instance, "lookupS");
        Serializers.register(ForwardMessage.class, "lookupS");
        Serializers.register(se.sics.caracaldb.global.Message.class, "lookupS");
        Serializers.register(CoreSerializer.OP.instance, "opS");
        Serializers.register(CaracalMsg.class, "opS");
        Serializers.register(CaracalOp.class, "opS");
        Serializers.register(CoreSerializer.OP.instance, "caracal-op");
        Serializers.register(RangeQuery.Request.class, "caracal-op");
        Serializers.register(RangeQuery.Response.class, "caracal-op");
        Serializers.register(PutRequest.class, "caracal-op");
        Serializers.register(PutResponse.class, "caracal-op");
    }

    public static class CaracalPSManagerInit extends Init<CaracalPSManagerComp> {
        public final CaracalPSManagerConfig config;
        
        public CaracalPSManagerInit(CaracalPSManagerConfig config) {
            this.config = config;
        }
    }
}