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

import se.sics.gvod.common.network.LocalNettyAdapter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.AddOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.network.GVoDAdapterFactory;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AddOverlayTest {
    
    @Test
    public void testRequest() {
        LocalNettyAdapter<AddOverlay.Request> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.ADD_OVERLAY_REQUEST);
        AddOverlay.Request expected = new AddOverlay.Request(UUID.randomUUID(), 1, new FileMetadata(10000, 1024, "SHA", 100));
        int expectedSize = adapter.getEncodedSize(expected);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.ADD_OVERLAY_REQUEST, type);
        AddOverlay.Request decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
    
    @Test
    public void tesResponsetSuccess() {
        LocalNettyAdapter<AddOverlay.Response> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.ADD_OVERLAY_RESPONSE);
        AddOverlay.Response expected = new AddOverlay.Response(UUID.randomUUID(), ReqStatus.SUCCESS, 1);
        int expectedSize = adapter.getEncodedSize(expected);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.ADD_OVERLAY_RESPONSE, type);
        AddOverlay.Response decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
    
    @Test
    public void tesResponsetFail() {
        LocalNettyAdapter<AddOverlay.Response> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.ADD_OVERLAY_RESPONSE);
        AddOverlay.Response expected = new AddOverlay.Response(UUID.randomUUID(), ReqStatus.FAIL, 1);
        int expectedSize = adapter.getEncodedSize(expected);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.ADD_OVERLAY_RESPONSE, type);
        AddOverlay.Response decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
}
