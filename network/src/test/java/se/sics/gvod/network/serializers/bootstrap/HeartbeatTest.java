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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.gvod.common.msg.peerMngr.Heartbeat;
import se.sics.gvod.network.GVoDNetFrameDecoder;
import se.sics.gvod.network.GVoDNetworkSettings;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HeartbeatTest {
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
    public void test() throws Serializer.SerializerException, SerializationContext.MissingException {
        Serializer<Heartbeat.OneWay> serializer = context.getSerializer(Heartbeat.OneWay.class);
        Assert.assertNotNull(serializer);
        
        Heartbeat.OneWay expected, decoded;
        ByteBuf buf, newBuf;
        int expectedSize;
        Map<Integer, Integer> overlaysUtility;
        
        overlaysUtility = new HashMap<Integer, Integer>();
        expected = new Heartbeat.OneWay(UUID.randomUUID(), overlaysUtility);
        expectedSize = serializer.getSize(context, expected);
        buf = Unpooled.buffer();
        serializer.encode(context, buf, expected);
        newBuf = Unpooled.wrappedBuffer(buf.array());
        decoded = serializer.decode(context, newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
        
        overlaysUtility = new HashMap<Integer, Integer>();
        overlaysUtility.put(1, 1);
        overlaysUtility.put(3, 2);
        expected = new Heartbeat.OneWay(UUID.randomUUID(), overlaysUtility);
        expectedSize = serializer.getSize(context, expected);
        buf = Unpooled.buffer();
        serializer.encode(context, buf, expected);
        newBuf = Unpooled.wrappedBuffer(buf.array());
        decoded = serializer.decode(context, newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
}
