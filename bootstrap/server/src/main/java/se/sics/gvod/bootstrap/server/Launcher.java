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
//package se.sics.gvod.bootstrap.server;
//
//import java.net.UnknownHostException;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import se.sics.gvod.net.NettyInit;
//import se.sics.gvod.net.VodMsgFrameDecoder;
//import se.sics.kompics.Component;
//import se.sics.kompics.ComponentDefinition;
//import se.sics.kompics.Init;
//import se.sics.kompics.address.Address;
//import se.sics.kompics.network.Network;
//import se.sics.kompics.network.netty.NettyNetwork;
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
//            Address netSelf = new Address(configuration.getIp(), configuration.getPort(), configuration.getId());
//            network = create(NettyNetwork.class);
//            timer = create(JavaTimer.class);
//            manager = create(HostManagerComp.class);
//
//            connect(manager.getNegative(Network.class), network.getPositive(Network.class));
//            connect(manager.getNegative(Timer.class), timer.getPositive(Timer.class));
//            
//            trigger(new NettyInit(configuration.getSeed(), true, VodMsgFrameDecoder.class), network.control());
//            trigger(new HostManagerInit(), manager.control());
//        } catch (UnknownHostException ex) {
//            throw new RuntimeException(ex);
//        }
//
//    }
//}
