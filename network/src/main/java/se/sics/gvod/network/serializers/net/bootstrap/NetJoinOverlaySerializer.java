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

package se.sics.gvod.network.serializers.net.bootstrap;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import se.sics.gvod.common.msg.peerMngr.JoinOverlay;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.netmsg.HeaderField;
import se.sics.gvod.network.netmsg.bootstrap.NetJoinOverlay;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.net.base.NetMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetJoinOverlaySerializer {
    public static class Request extends NetMsgSerializer.AbsRequest<NetJoinOverlay.Request> {
        
        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, NetJoinOverlay.Request obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsRequest(context, buf, obj);
            context.getSerializer(JoinOverlay.Request.class).encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public NetJoinOverlay.Request decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absReq = decodeAbsRequest(context, buf);
            JoinOverlay.Request content = context.getSerializer(JoinOverlay.Request.class).decode(context, buf);
            return new NetJoinOverlay.Request(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absReq.getValue0(), absReq.getValue1(), content);
        }

        @Override
        public int getSize(SerializationContext context, NetJoinOverlay.Request obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsRequest(context, obj);
            size += context.getSerializer(JoinOverlay.Request.class).getSize(context, obj.content);
            return size;
        }

    }
    
    public static class Response extends NetMsgSerializer.AbsResponse<NetJoinOverlay.Response> {

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, NetJoinOverlay.Response obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsResponse(context, buf, obj);
            context.getSerializer(JoinOverlay.Response.class).encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public NetJoinOverlay.Response decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absResp = decodeAbsResponse(context, buf);
            JoinOverlay.Response content = context.getSerializer(JoinOverlay.Response.class).decode(context, buf);
            return new NetJoinOverlay.Response(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absResp.getValue0(), absResp.getValue1(), content);
        }

        @Override
        public int getSize(SerializationContext context, NetJoinOverlay.Response obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsResponse(context, obj);
            size += context.getSerializer(JoinOverlay.Response.class).getSize(context, obj.content);
            return size;
        }

    }
}
