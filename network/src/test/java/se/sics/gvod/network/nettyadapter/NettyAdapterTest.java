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

package se.sics.gvod.network.nettyadapter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.gvod.address.Address;
import se.sics.gvod.common.msg.peerMngr.AddOverlay;
import se.sics.gvod.common.msg.peerMngr.BootstrapGlobal;
import se.sics.gvod.common.msgs.DirectMsgNettyFactory;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.GVoDNetFrameDecoder;
import se.sics.gvod.network.GVoDNetworkSettings;
import se.sics.gvod.network.netmsg.NetMsg;
import se.sics.gvod.network.netmsg.bootstrap.NetAddOverlay;
import se.sics.gvod.network.serializers.SerializationContext;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NettyAdapterTest {
    private static SerializationContext context;
    @BeforeClass
    public static void setup() {
        GVoDNetFrameDecoder.reset();
        GVoDNetFrameDecoder.register();
        GVoDNetworkSettings.checkPreCond();
        GVoDNetworkSettings.registerSerializers();
        context = GVoDNetworkSettings.getContext();
    }
    
//    @Test
//    public void testRequest() throws UnknownHostException, MessageEncodingException, Exception {
//        AddOverlay.Request req = new AddOverlay.Request(UUID.randomUUID(), 1, new FileMetadata(10000, 1024, "SHA", 100));
//        VodAddress src = new VodAddress(new Address(InetAddress.getLocalHost(), 1234, 1), -1);
//        VodAddress dest = new VodAddress(new Address(InetAddress.getLocalHost(), 1234, 2), -1);
//        NetAddOverlay.Request expected = new NetAddOverlay.Request(src, dest, UUID.randomUUID(), req);
//        int expectedSize = expected.getSize();
//        ByteBuf buf = expected.toByteArray();
//        ByteBuf newBuf = Unpooled.wrappedBuffer(Arrays.copyOf(buf.array(), buf.array().length));
//        
//        GVoDNetFrameDecoder g = new GVoDNetFrameDecoder();
//        NetAddOverlay.Request decoded = (NetAddOverlay.Request) g.parse(newBuf);
//        Assert.assertEquals(expected, decoded);
//        Assert.assertEquals(0, newBuf.readableBytes());
//    }
    
//    @Test
//    public void testRequest2() throws UnknownHostException, MessageEncodingException, Exception {
//        BootstrapGlobal.Request req = new BootstrapGlobal.Request(UUID.randomUUID());
//        VodAddress src = new VodAddress(new Address(InetAddress.getLocalHost(), 1234, 1), -1);
//        VodAddress dest = new VodAddress(new Address(InetAddress.getLocalHost(), 1234, 2), -1);
//        PayloadNetMsg.Request<BootstrapGlobal.Request> expected = new PayloadNetMsg.Request<BootstrapGlobal.Request>(src, dest, req);
//        expected.setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
//        int expectedSize = expected.getSize();
//        ByteBuf buf = expected.toByteArray();
//        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
//        GVoDNetFrameDecoder decoder = new GVoDNetFrameDecoder();
//        PayloadNetMsg.Request<BootstrapGlobal.Request> decoded = (PayloadNetMsg.Request<BootstrapGlobal.Request>)decoder.parse(newBuf);
//        Assert.assertEquals(expected, decoded);
//        GVoDAdapter<BootstrapGlobal.Request> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_REQUEST);
//        Assert.assertEquals(adapter.getEncodedSize(expected.payload), adapter.getEncodedSize(decoded.payload));
////        Assert.assertEquals(expectedSize, buf.readableBytes());
//    }
}
