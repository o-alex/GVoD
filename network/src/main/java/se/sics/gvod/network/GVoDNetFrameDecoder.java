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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.vod.Connection;
import se.sics.gvod.common.msg.vod.Download;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.util.VodDescriptor;
import se.sics.gvod.net.BaseMsgFrameDecoder;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.network.nettyadapter.GvodNettyAdapter;
import se.sics.gvod.network.nettyadapter.NettyAdapter;
import se.sics.gvod.network.nettyadapter.OverlayNetAdapter;
import se.sics.gvod.network.nettymsg.OverlayNetMsg;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.SerializationContextImpl;
import se.sics.gvod.network.serializers.util.ReqStatusSerializer;
import se.sics.gvod.network.serializers.util.UUIDSerializer;
import se.sics.gvod.network.serializers.util.VodAddressSerializer;
import se.sics.gvod.network.serializers.util.VodDescriptorSerializer;
import se.sics.gvod.network.serializers.vod.ConnectionSerializer;
import se.sics.gvod.network.serializers.vod.DownloadSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GVoDNetFrameDecoder extends BaseMsgFrameDecoder {

    private static final byte MAX = (byte) 0x255;
    
    //netty
    public static final byte GVOD_NET_REQUEST = 0x60;
    public static final byte GVOD_NET_RESPONSE = 0x61;
    public static final byte GVOD_NET_ONEWAY = 0x62;
    public static final byte OVERLAY_NET_REQUEST = 0x63;
    public static final byte OVERLAY_NET_RESPONSE = 0x64;
    public static final byte OVERLAY_NET_ONEWAY = 0x65;

    private static final Map<Byte, NettyAdapter> nettyAdapters = new HashMap<Byte, NettyAdapter>();
    static {
        GvodNettyAdapter.Request.setMsgFrameDecoder(GVoDNetFrameDecoder.class);
        GvodNettyAdapter.Response.setMsgFrameDecoder(GVoDNetFrameDecoder.class);
        GvodNettyAdapter.OneWay.setMsgFrameDecoder(GVoDNetFrameDecoder.class);
        
        OverlayNetAdapter.Request.setMsgFrameDecoder(GVoDNetFrameDecoder.class);
        OverlayNetAdapter.Response.setMsgFrameDecoder(GVoDNetFrameDecoder.class);
        OverlayNetAdapter.OneWay.setMsgFrameDecoder(GVoDNetFrameDecoder.class);
        
        nettyAdapters.put(GVOD_NET_REQUEST, new GvodNettyAdapter.Request());
        nettyAdapters.put(GVOD_NET_RESPONSE, new GvodNettyAdapter.Response());
        nettyAdapters.put(GVOD_NET_ONEWAY, new GvodNettyAdapter.OneWay());
        nettyAdapters.put(OVERLAY_NET_REQUEST, new OverlayNetAdapter.Request());
        nettyAdapters.put(OVERLAY_NET_RESPONSE, new OverlayNetAdapter.Response());
        nettyAdapters.put(OVERLAY_NET_ONEWAY, new OverlayNetAdapter.OneWay());
    }
    
    private static final SerializationContext context = new SerializationContextImpl();
    static {
        try {
            context.registerSerializer("request-status", new ReqStatusSerializer());
            context.registerClass("request-status", ReqStatus.class);
            context.registerSerializer("uuid", new UUIDSerializer());
            context.registerClass("uuid", UUID.class);
            context.registerSerializer("vod-address", new VodAddressSerializer());
            context.registerClass("vod-address", VodAddress.class);
            context.registerSerializer("vod-descriptor", new VodDescriptorSerializer());
            context.registerClass("vod-descriptor", VodDescriptor.class);
            
            //payloads - with opcodes
            context.registerSerializer("conn-req", new ConnectionSerializer.Request());
            context.registerClass("conn-req", Connection.Request.class);
            context.registerMessageCode(Connection.Request.class, (byte)0x01);
            context.registerSerializer("conn-resp", new ConnectionSerializer.Response());
            context.registerClass("conn-resp", Connection.Response.class);
            context.registerMessageCode(Connection.Response.class, (byte)0x02);
            context.registerSerializer("conn-update", new ConnectionSerializer.Update());
            context.registerClass("conn-update", Connection.Update.class);
            context.registerMessageCode(Connection.Update.class, (byte)0x03);
            context.registerSerializer("conn-close", new ConnectionSerializer.Close());
            context.registerClass("conn-close", Connection.Close.class);
            context.registerMessageCode(Connection.Close.class, (byte)0x04);
            
            context.registerSerializer("down-req", new DownloadSerializer.Request());
            context.registerClass("down-req", Download.Request.class);
            context.registerMessageCode(Download.Request.class, (byte)0x05);
            context.registerSerializer("down-resp", new DownloadSerializer.Response());
            context.registerClass("down-resp", Download.Response.class);
            context.registerMessageCode(Download.Response.class, (byte)0x06);
        } catch (SerializationContext.DuplicateException ex) {
            throw new RuntimeException(ex);
        } catch (SerializationContext.MissingException ex) {
           throw new RuntimeException(ex);
        }
    }
    
    public GVoDNetFrameDecoder() {
        super();
    }

    @Override
    protected RewriteableMsg decodeMsg(ChannelHandlerContext ctx,
            ByteBuf buffer) throws MessageDecodingException {

        try {
            // See if msg is part of parent project, if yes then return it.
            // Otherwise decode the msg here.
            RewriteableMsg msg = super.decodeMsg(ctx, buffer);
            if (msg != null) {
                return msg;
            }
            
            NettyAdapter currentAdapter = nettyAdapters.get(opKod);
            if(currentAdapter == null) {
                throw new RuntimeException("no adapter registered for opcode:" + opKod);
            }
            
            return currentAdapter.decodeMsg(buffer);
        } catch (NettyAdapter.DecodingException ex) {
            throw new MessageDecodingException(ex);
        }
    }
}
