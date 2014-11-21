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

package se.sics.gvod.network.serializers.netvod;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.gvod.address.Address;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.AddOverlay;
import se.sics.gvod.common.msg.vod.Download;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.GVoDNetFrameDecoder;
import se.sics.gvod.network.GVoDNetworkSettings;
import se.sics.gvod.network.netmsg.bootstrap.NetAddOverlay;
import se.sics.gvod.network.netmsg.vod.NetDownload;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetDownloadTest {
    private static SerializationContext context;
    @BeforeClass
    public static void setup() {
        GVoDNetFrameDecoder.reset();
        GVoDNetFrameDecoder.register();
        GVoDNetworkSettings.checkPreCond();
        GVoDNetworkSettings.registerSerializers();
        context = GVoDNetworkSettings.getContext();
    }
    
//    @Test
//    public void testRequest() throws SerializationContext.MissingException, Serializer.SerializerException, UnknownHostException {
//        Address adr = new Address(Inet4Address.getLocalHost(), 10000, 123);
//        Serializer<NetAddOverlay.Request> serializer = context.getSerializer(NetAddOverlay.Request.class);
//        Assert.assertNotNull(serializer);
//        AddOverlay.Request req = new AddOverlay.Request(UUID.randomUUID(), 1, new FileMetadata(10000, 1024, "SHA", 100));
//        NetAddOverlay.Request expected = new NetAddOverlay.Request(new VodAddress(adr, -1), new VodAddress(adr, -1), UUID.randomUUID(), req);
//        int expectedSize = serializer.getSize(context, expected);
//        ByteBuf buf = Unpooled.buffer();
//        serializer.encode(context, buf, expected);
//        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
//        NetAddOverlay.Request decoded = serializer.decode(context, newBuf);
//        decoded.rewriteDestination(adr);
//        decoded.rewritePublicSource(adr);
//        Assert.assertEquals(expected, decoded);
//        Assert.assertEquals(expectedSize, buf.readableBytes());
//    }
    
    @Test
    public void testHashResponse() throws Serializer.SerializerException, SerializationContext.MissingException, UnknownHostException {
        Address adr = new Address(Inet4Address.getLocalHost(), 10000, 123);
        Serializer<NetDownload.HashResponse> serializer = context.getSerializer(NetDownload.HashResponse.class);
        Assert.assertNotNull(serializer);
        Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
        hashes.put(1, new byte[]{0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02});
        hashes.put(2, new byte[]{0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03});
        hashes.put(3, new byte[]{0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02});
        hashes.put(4, new byte[]{0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02,0x01,0x02});
        hashes.put(5, new byte[]{0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03,0x01,0x03});
        hashes.put(6, new byte[]{0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02});
        hashes.put(7, new byte[]{0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02});
        hashes.put(8, new byte[]{0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02});
        hashes.put(9, new byte[]{0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02,0x03,0x02});
        Download.HashResponse resp = new Download.HashResponse(UUID.randomUUID(), ReqStatus.SUCCESS, 10, hashes, new HashSet<Integer>());
        NetDownload.HashResponse expected = new NetDownload.HashResponse(new VodAddress(adr, -1), new VodAddress(adr, -1), UUID.randomUUID(), 10,resp);
        int expectedSize = serializer.getSize(context, expected);
        ByteBuf buf = Unpooled.buffer();
        serializer.encode(context, buf, expected);
        ByteBuf newBuf = Unpooled.wrappedBuffer(buf.array());
        NetDownload.HashResponse decoded = serializer.decode(context, newBuf);
        decoded.rewriteDestination(adr);
        decoded.rewritePublicSource(adr);
        Assert.assertEquals(expected, decoded);
        Assert.assertEquals(expectedSize, buf.readableBytes());
        System.out.println(expectedSize);
    }
    
}
