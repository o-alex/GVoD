///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.gvod.system;
//
//import java.net.UnknownHostException;
//import se.sics.kompics.Component;
//import se.sics.kompics.ComponentDefinition;
//import se.sics.kompics.Init;
//import se.sics.kompics.address.Address;
//import se.sics.kompics.network.Network;
//import se.sics.kompics.network.netty.NettyNetwork;
//import se.sics.kompics.network.netty.NettyNetworkInit;
//import se.sics.kompics.timer.Timer;
//import se.sics.kompics.timer.java.JavaTimer;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class Launcher extends ComponentDefinition {
//
//    private Component network;
//    private Component timer;
//    private Component manager;
//
//    {
//        try {
//            HostConfiguration configuration = new HostConfiguration.Builder().finalise();
//            Address netSelf = new Address(configuration.getIp(), configuration.getPort(), 0);
//            network = create(NettyNetwork.class, new NettyNetworkInit(netSelf));
//            timer = create(JavaTimer.class, Init.NONE);
//            manager = create(HostManagerComp.class, new HostManagerInit());
//
//            connect(manager.getNegative(Network.class), network.getPositive(Network.class));
//            connect(manager.getNegative(Timer.class), timer.getPositive(Timer.class));
//        } catch (UnknownHostException ex) {
//            throw new RuntimeException(ex);
//        }
//
//    }
//}
