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
//
//package se.sics.gvod.network;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.Unpooled;
//import java.net.Inet4Address;
//import java.net.UnknownHostException;
//import java.util.Arrays;
//import org.junit.Assert;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import se.sics.gvod.common.msg.ReqStatus;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class UtilTests {
//    
//    private static SerializationContext context;
//    @BeforeClass
//    public static void setup() {
//        GVoDNetFrameDecoder.register();
//        GVoDNetworkSettings.checkPreCond();
//        GVoDNetworkSettings.registerSerializers();
//        context = GVoDNetworkSettings.getContext();
//    }
//    
//    @Test
//    public void testReqStatus() throws SerializationContext.MissingException, Serializer.SerializerException, UnknownHostException, UnknownHostException, UnknownHostException {
//        Serializer<ReqStatus> serializer = context.getSerializer(ReqStatus.class);
//        Assert.assertNotNull(serializer);
//        ReqStatus expected, decoded;
//        ByteBuf buf, newBuf;
//        int expectedSize;
//        
//        expected = ReqStatus.SUCCESS;
//        expectedSize = serializer.getSize(context, expected);
//        buf = Unpooled.buffer();
//        serializer.encode(context, buf, expected);
//        newBuf = Unpooled.wrappedBuffer(buf.array());
//        decoded = serializer.decode(context, newBuf);
//        Assert.assertEquals(expected, decoded);
//        Assert.assertEquals(expectedSize, buf.readableBytes());
//        
//        expected = ReqStatus.FAIL;
//        expectedSize = serializer.getSize(context, expected);
//        buf = Unpooled.buffer();
//        serializer.encode(context, buf, expected);
//        newBuf = Unpooled.wrappedBuffer(buf.array());
//        decoded = serializer.decode(context, newBuf);
//        Assert.assertEquals(expected, decoded);
//        Assert.assertEquals(expectedSize, buf.readableBytes());
//        
//        expected = ReqStatus.BUSY;
//        expectedSize = serializer.getSize(context, expected);
//        buf = Unpooled.buffer();
//        serializer.encode(context, buf, expected);
//        newBuf = Unpooled.wrappedBuffer(buf.array());
//        decoded = serializer.decode(context, newBuf);
//        Assert.assertEquals(expected, decoded);
//        Assert.assertEquals(expectedSize, buf.readableBytes());
//        
//        expected = ReqStatus.MISSING;
//        expectedSize = serializer.getSize(context, expected);
//        buf = Unpooled.buffer();
//        serializer.encode(context, buf, expected);
//        newBuf = Unpooled.wrappedBuffer(buf.array());
//        decoded = serializer.decode(context, newBuf);
//        Assert.assertEquals(expected, decoded);
//        Assert.assertEquals(expectedSize, buf.readableBytes());
//        
//        expected = ReqStatus.TIMEOUT;
//        expectedSize = serializer.getSize(context, expected);
//        buf = Unpooled.buffer();
//        serializer.encode(context, buf, expected);
//        newBuf = Unpooled.wrappedBuffer(buf.array());
//        decoded = serializer.decode(context, newBuf);
//        Assert.assertEquals(expected, decoded);
//        Assert.assertEquals(expectedSize, buf.readableBytes());
//    }
//    
//     @Test
//    public void testVodAddress() throws SerializationContext.MissingException, Serializer.SerializerException, UnknownHostException {
//        Serializer<VodAddress> serializer = context.getSerializer(VodAddress.class);
//        Assert.assertNotNull(serializer);
//        VodAddress expected, decoded;
//        ByteBuf buf, newBuf;
//        int expectedSize;
//        
//        expected = new VodAddress(new Address(Inet4Address.getLocalHost(), 10000, 123), 10);
//        expectedSize = serializer.getSize(context, expected);
//        buf = Unpooled.buffer();
//        serializer.encode(context, buf, expected);
//        newBuf = Unpooled.wrappedBuffer(Arrays.copyOf(buf.array(), buf.array().length));
//        decoded = serializer.decode(context, newBuf);
//        Assert.assertEquals(expected, decoded);
//        Assert.assertEquals(expectedSize, buf.readableBytes());
//        
//        expected = new VodAddress(new Address(Inet4Address.getByName("localhost"), 10000, 123), 10);
//        expectedSize = serializer.getSize(context, expected);
//        buf = Unpooled.buffer();
//        serializer.encode(context, buf, expected);
//        newBuf = Unpooled.wrappedBuffer(Arrays.copyOf(buf.array(), buf.array().length));
//        decoded = serializer.decode(context, newBuf);
//        Assert.assertEquals(expected, decoded);
//        Assert.assertEquals(expectedSize, buf.readableBytes());
//    }
//}
