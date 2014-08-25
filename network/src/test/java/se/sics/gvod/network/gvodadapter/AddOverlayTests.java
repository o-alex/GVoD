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
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.AddOverlayMsg;
import se.sics.gvod.network.GVoDAdapterFactory;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AddOverlayTests {
    
    @Test
    public void testRequest() {
        GVoDAdapter<AddOverlayMsg.Request> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.ADD_OVERLAY_REQUEST);
        AddOverlayMsg.Request expected = new AddOverlayMsg.Request(UUID.randomUUID(), 1);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.ADD_OVERLAY_REQUEST, type);
        AddOverlayMsg.Request decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
    }
    
    @Test
    public void tesResponsetSuccess() {
        GVoDAdapter<AddOverlayMsg.Response> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.ADD_OVERLAY_RESPONSE);
        AddOverlayMsg.Response expected = new AddOverlayMsg.Response(UUID.randomUUID(), ReqStatus.SUCCESS);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.ADD_OVERLAY_RESPONSE, type);
        AddOverlayMsg.Response decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
    }
    
    @Test
    public void tesResponsetFail() {
        GVoDAdapter<AddOverlayMsg.Response> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.ADD_OVERLAY_RESPONSE);
        AddOverlayMsg.Response expected = new AddOverlayMsg.Response(UUID.randomUUID(), ReqStatus.FAIL);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.ADD_OVERLAY_RESPONSE, type);
        AddOverlayMsg.Response decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
    }
}
