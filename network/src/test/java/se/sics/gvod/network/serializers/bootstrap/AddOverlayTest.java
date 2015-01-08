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
import java.util.UUID;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.AddOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.network.GVoDNetFrameDecoder;
import se.sics.gvod.network.GVoDNetworkSettings;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AddOverlayTest {
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
        Serializer<AddOverlay.Request> serializer = context.getSerializer(AddOverlay.Request.class);
        Assert.assertNotNull(serializer);
        AddOverlay.Request expected = new AddOverlay.Request(UUID.randomUUID(), 1, new FileMetadata("file.mp4", 10000, 1024, "SHA", 100));
        int expectedSize = serializer.getSize(context, expected);
        ByteBuf buf = Unpooled.buffer();
        serializer.encode(context, buf, expected);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        AddOverlay.Request decoded = serializer.decode(context, newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
    
    @Test
    public void testResponseSuccess() throws Serializer.SerializerException, SerializationContext.MissingException {
        Serializer<AddOverlay.Response> serializer = context.getSerializer(AddOverlay.Response.class);
        Assert.assertNotNull(serializer);
        AddOverlay.Response expected = new AddOverlay.Response(UUID.randomUUID(), ReqStatus.SUCCESS, 1);
        int expectedSize = serializer.getSize(context, expected);
        ByteBuf buf = Unpooled.buffer();
        serializer.encode(context, buf, expected);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        AddOverlay.Response decoded = serializer.decode(context, newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
    
    @Test
    public void testResponseFail() throws Serializer.SerializerException, SerializationContext.MissingException {
        Serializer<AddOverlay.Response> serializer = context.getSerializer(AddOverlay.Response.class);
        Assert.assertNotNull(serializer);
        
        AddOverlay.Response expected = new AddOverlay.Response(UUID.randomUUID(), ReqStatus.FAIL, 1);
        int expectedSize = serializer.getSize(context, expected);
        ByteBuf buf = Unpooled.buffer();
        serializer.encode(context, buf, expected);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        AddOverlay.Response decoded = serializer.decode(context, newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
}
