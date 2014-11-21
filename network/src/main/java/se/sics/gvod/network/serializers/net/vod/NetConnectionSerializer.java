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
package se.sics.gvod.network.serializers.net.vod;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import se.sics.gvod.common.msg.vod.Connection;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.netmsg.HeaderField;
import se.sics.gvod.network.netmsg.vod.NetConnection;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.net.base.NetMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetConnectionSerializer {

    public static class Request extends NetMsgSerializer.AbsRequest<NetConnection.Request> {

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, NetConnection.Request obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsRequest(context, buf, obj);
            context.getSerializer(Connection.Request.class).encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public NetConnection.Request decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absReq = decodeAbsRequest(context, buf);
            Connection.Request content = context.getSerializer(Connection.Request.class).decode(context, buf);
            return new NetConnection.Request(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absReq.getValue0(), absReq.getValue1(), content);
        }

        @Override
        public int getSize(SerializationContext context, NetConnection.Request obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsRequest(context, obj);
            size += context.getSerializer(Connection.Request.class).getSize(context, obj.content);
            return size;
        }
    }

    public static class Response extends NetMsgSerializer.AbsResponse<NetConnection.Response> {

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, NetConnection.Response obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsResponse(context, buf, obj);
            context.getSerializer(Connection.Response.class).encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public NetConnection.Response decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absResp = decodeAbsResponse(context, buf);
            Connection.Response content = context.getSerializer(Connection.Response.class).decode(context, buf);
            return new NetConnection.Response(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absResp.getValue0(), absResp.getValue1(), content);
        }

        @Override
        public int getSize(SerializationContext context, NetConnection.Response obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsResponse(context, obj);
            size += context.getSerializer(Connection.Response.class).getSize(context, obj.content);
            return size;
        }
    }

    public static class Update extends NetMsgSerializer.AbsOneWay<NetConnection.Update> {

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, NetConnection.Update obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsOneWay(context, buf, obj);
            context.getSerializer(Connection.Update.class).encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public NetConnection.Update decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absOne = decodeAbsOneWay(context, buf);
            Connection.Update content = context.getSerializer(Connection.Update.class).decode(context, buf);
            return new NetConnection.Update(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absOne.getValue0(), absOne.getValue1(), content);
        }

        @Override
        public int getSize(SerializationContext context, NetConnection.Update obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsOneWay(context, obj);
            size += context.getSerializer(Connection.Update.class).getSize(context, obj.content);
            return size;
        }
    }

    public static class Close extends NetMsgSerializer.AbsOneWay<NetConnection.Close> {
        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, NetConnection.Close obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsOneWay(context, buf, obj);
            context.getSerializer(Connection.Close.class).encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public NetConnection.Close decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absOne = decodeAbsOneWay(context, buf);
            Connection.Close content = context.getSerializer(Connection.Close.class).decode(context, buf);
            return new NetConnection.Close(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absOne.getValue0(), absOne.getValue1(), content);
        }

        @Override
        public int getSize(SerializationContext context, NetConnection.Close obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsOneWay(context, obj);
            size += context.getSerializer(Connection.Close.class).getSize(context, obj.content);
            return size;
        }

    }
}
