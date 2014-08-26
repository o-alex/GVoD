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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import se.sics.gvod.address.Address;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.BootstrapGlobalMsg;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.GVoDAdapterFactory;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootrapGlobalTest {
    
    @Test
    public void testRequest() {
        GVoDAdapter<BootstrapGlobalMsg.Request> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_REQUEST);
        BootstrapGlobalMsg.Request expected = new BootstrapGlobalMsg.Request(UUID.randomUUID());
        int expectedSize = adapter.getEncodedSize(expected);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_REQUEST, type);
        BootstrapGlobalMsg.Request decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
    }
    
    @Test
    public void testSuccessResponse0() {
        GVoDAdapter<BootstrapGlobalMsg.Response> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_RESPONSE);
        BootstrapGlobalMsg.Response expected = new BootstrapGlobalMsg.Response(UUID.randomUUID(), ReqStatus.SUCCESS, new HashSet<VodAddress>());
        int expectedSize = adapter.getEncodedSize(expected);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_RESPONSE, type);
        BootstrapGlobalMsg.Response decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
    
    @Test
    public void testSuccessResponse() throws UnknownHostException {
        GVoDAdapter<BootstrapGlobalMsg.Response> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_RESPONSE);
        Set<VodAddress> systemSample = new HashSet<VodAddress>();
        InetAddress ip = InetAddress.getByName("localhost");
        Address adr = new Address(ip, 12345, 0);
        systemSample.add(new VodAddress(adr, 1));
        systemSample.add(new VodAddress(adr, 2));
        systemSample.add(new VodAddress(adr, 3));
        BootstrapGlobalMsg.Response expected = new BootstrapGlobalMsg.Response(UUID.randomUUID(), ReqStatus.SUCCESS, systemSample);
        int expectedSize = adapter.getEncodedSize(expected);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_RESPONSE, type);
        BootstrapGlobalMsg.Response decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
    
    @Test
    public void testFail() {
        GVoDAdapter<BootstrapGlobalMsg.Response> adapter = GVoDAdapterFactory.getAdapter(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_RESPONSE);
        BootstrapGlobalMsg.Response expected = new BootstrapGlobalMsg.Response(UUID.randomUUID(), ReqStatus.FAIL, null);
        int expectedSize = adapter.getEncodedSize(expected);
        ByteBuf buf = Unpooled.buffer();
        adapter.encode(expected, buf);
        
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        byte type = newBuf.readByte();
        Assert.assertEquals(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_RESPONSE, type);
        BootstrapGlobalMsg.Response decoded = adapter.decode(newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
}