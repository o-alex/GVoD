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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import se.sics.gvod.address.Address;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.JoinOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.GVoDAdapterFactory;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class JoinOverlayTest {
    @Test
    public void testRequest() {
        GVoDAdapter<JoinOverlay.Request> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.JOIN_OVERLAY_REQUEST);
        JoinOverlay.Request expected = new JoinOverlay.Request(UUID.randomUUID(), 1, 0);
        int expectedSize = adapter.getEncodedSize(expected);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.JOIN_OVERLAY_REQUEST, type);
        JoinOverlay.Request decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
    
     @Test
    public void testResponseSuccess() throws UnknownHostException {
        GVoDAdapter<JoinOverlay.Response> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.JOIN_OVERLAY_RESPONSE);
        Map<VodAddress, Integer> overlaySample = new HashMap<VodAddress, Integer>();
        overlaySample.put(new VodAddress(new Address(InetAddress.getLocalHost(), 12345, 1), -1), 1);
        JoinOverlay.Response expected = new JoinOverlay.Response(UUID.randomUUID(), ReqStatus.SUCCESS, 1, overlaySample, new FileMetadata(10000, 1024));
        int expectedSize = adapter.getEncodedSize(expected);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.JOIN_OVERLAY_RESPONSE, type);
        JoinOverlay.Response decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
    
    @Test
    public void tesResponsetFail() {
        GVoDAdapter<JoinOverlay.Response> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.JOIN_OVERLAY_RESPONSE);
        JoinOverlay.Response expected = new JoinOverlay.Response(UUID.randomUUID(), ReqStatus.FAIL, 1, null, null);
        int expectedSize = adapter.getEncodedSize(expected);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.JOIN_OVERLAY_RESPONSE, type);
        JoinOverlay.Response decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
}
