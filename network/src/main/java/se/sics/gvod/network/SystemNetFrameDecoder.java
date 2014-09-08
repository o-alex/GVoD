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
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.net.BaseMsgFrameDecoder;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.network.nettyadapter.GvodNettyAdapter;
import se.sics.gvod.common.network.NetworkNettyAdapter;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SystemNetFrameDecoder extends BaseMsgFrameDecoder {

    private static final byte MAX = (byte) 0x255;
    
    //netty
    public static final byte GVOD_NET_REQUEST = 0x60;
    public static final byte GVOD_NET_RESPONSE = 0x61;
    public static final byte GVOD_NET_ONEWAY = 0x62;
    public static final byte CROUPIER_NET_REQUEST = 0x63;
    public static final byte CROUPIER_NET_RESPONSE = 0x64; 

    private static final Map<Byte, NetworkNettyAdapter> nettyAdapters = new HashMap<Byte, NetworkNettyAdapter>();
    static {
        GvodNettyAdapter.Request.setMsgFrameDecoder(SystemNetFrameDecoder.class);
        GvodNettyAdapter.Response.setMsgFrameDecoder(SystemNetFrameDecoder.class);
        
        nettyAdapters.put(GVOD_NET_REQUEST, new GvodNettyAdapter.Request());
        nettyAdapters.put(GVOD_NET_RESPONSE, new GvodNettyAdapter.Response());
        
    }
    
    public SystemNetFrameDecoder() {
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
            
            NetworkNettyAdapter currentAdapter = nettyAdapters.get(opKod);
            if(currentAdapter == null) {
                throw new RuntimeException("no adapter registered for opcode:" + opKod);
            }
            
            return currentAdapter.decodeMsg(buffer);
        } catch (NetworkNettyAdapter.DecodingException ex) {
            throw new MessageDecodingException(ex);
        }
    }
}
