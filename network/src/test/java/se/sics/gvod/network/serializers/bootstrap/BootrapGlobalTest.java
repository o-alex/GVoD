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

package se.sics.gvod.network.serializers.bootstrap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.gvod.address.Address;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.BootstrapGlobal;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.GVoDNetFrameDecoder;
import se.sics.gvod.network.GVoDNetworkSettings;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootrapGlobalTest {
    private static SerializationContext context;
    @BeforeClass
    public static void setup() {
        GVoDNetFrameDecoder.reset();
        GVoDNetFrameDecoder.register();
        GVoDNetworkSettings.checkPreCond();
        GVoDNetworkSettings.registerSerializers();
        context = GVoDNetworkSettings.getContext();
    }
    
    @Test
    public void testRequest() throws SerializationContext.MissingException, Serializer.SerializerException {
        Serializer<BootstrapGlobal.Request> serializer = context.getSerializer(BootstrapGlobal.Request.class);
        Assert.assertNotNull(serializer);
        BootstrapGlobal.Request expected = new BootstrapGlobal.Request(UUID.randomUUID());
        int expectedSize = serializer.getSize(context, expected);
        ByteBuf buf = Unpooled.buffer();
        serializer.encode(context, buf, expected);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        BootstrapGlobal.Request decoded = serializer.decode(context, newBuf);
        Assert.assertEquals(expected, decoded);
    }
    
    @Test
    public void testSuccessResponse0() throws SerializationContext.MissingException, Serializer.SerializerException {
        Serializer<BootstrapGlobal.Response> serializer = context.getSerializer(BootstrapGlobal.Response.class);
        Assert.assertNotNull(serializer);
        BootstrapGlobal.Response expected = new BootstrapGlobal.Response(UUID.randomUUID(), ReqStatus.SUCCESS, new HashSet<VodAddress>());
        int expectedSize = serializer.getSize(context, expected);
        ByteBuf buf = Unpooled.buffer();
        serializer.encode(context, buf, expected);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        BootstrapGlobal.Response decoded = serializer.decode(context, newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
    
    @Test
    public void testSuccessResponse() throws UnknownHostException, SerializationContext.MissingException, Serializer.SerializerException {
        Serializer<BootstrapGlobal.Response> serializer = context.getSerializer(BootstrapGlobal.Response.class);
        Assert.assertNotNull(serializer);
        Set<VodAddress> systemSample = new HashSet<VodAddress>();
        InetAddress ip = InetAddress.getByName("localhost");
        Address adr = new Address(ip, 12345, 0);
        systemSample.add(new VodAddress(adr, 1));
        systemSample.add(new VodAddress(adr, 2));
        systemSample.add(new VodAddress(adr, 3));
        BootstrapGlobal.Response expected = new BootstrapGlobal.Response(UUID.randomUUID(), ReqStatus.SUCCESS, systemSample);
        int expectedSize = serializer.getSize(context, expected);
        ByteBuf buf = Unpooled.buffer();
        serializer.encode(context, buf, expected);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        BootstrapGlobal.Response decoded = serializer.decode(context, newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
    
    @Test
    public void testFail() throws SerializationContext.MissingException, Serializer.SerializerException {
        Serializer<BootstrapGlobal.Response> serializer = context.getSerializer(BootstrapGlobal.Response.class);
        Assert.assertNotNull(serializer);
        BootstrapGlobal.Response expected = new BootstrapGlobal.Response(UUID.randomUUID(), ReqStatus.FAIL, null);
        int expectedSize = serializer.getSize(context, expected);
        ByteBuf buf = Unpooled.buffer();
        serializer.encode(context, buf, expected);
        
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        BootstrapGlobal.Response decoded = serializer.decode(context, newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
}