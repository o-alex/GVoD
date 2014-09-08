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

package se.sics.gvod.network;

import se.sics.gvod.common.network.NetUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import junit.framework.Assert;
import org.junit.Test;
import se.sics.gvod.address.Address;
import se.sics.gvod.net.VodAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class UtilTests {

    @Test
    public void testUUID() {
        UUID expected = UUID.randomUUID();
        int expectedSize = NetUtil.getUUIDEncodedSize();
        ByteBuf buf = Unpooled.buffer();
        NetUtil.encodeUUID(buf, expected);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        UUID decoded = NetUtil.decodeUUID(newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
    
    @Test 
    public void testVodAddress() throws UnknownHostException {
        VodAddress expected = new VodAddress(new Address(InetAddress.getLocalHost(), 1234, 1), -1);
        int expectedSize = NetUtil.getVodAddressEncodedSize(expected);
        ByteBuf buf = Unpooled.buffer();
        NetUtil.encodeVodAddress(buf, expected);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        VodAddress decoded = NetUtil.decodeVodAddress(newBuf);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
    }
}
