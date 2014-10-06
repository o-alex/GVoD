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

package se.sics.gvod.network.gvodadapter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import se.sics.gvod.common.msg.impl.Heartbeat;
import se.sics.gvod.network.GVoDAdapterFactory;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HeartbeatTest {
    
    @Test
    public void test() {
        GVoDAdapter<Heartbeat.OneWay> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.OVERLAY_HEARTBEAT);
        Map<Integer, Integer> overlaysUtility = new HashMap<Integer, Integer>();
        overlaysUtility.put(1, 1);
        overlaysUtility.put(3, 2);
        Heartbeat.OneWay expected = new Heartbeat.OneWay(UUID.randomUUID(), overlaysUtility);
        int expectedSize = adapter.getEncodedSize(expected);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.OVERLAY_HEARTBEAT, type);
        Heartbeat.OneWay decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
}
