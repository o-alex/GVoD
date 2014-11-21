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
package se.sics.gvod.network.serializers.vod;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.vod.Connection;
import se.sics.gvod.common.util.VodDescriptor;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.base.GvodMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ConnectionSerializer {

    public static final class Request extends GvodMsgSerializer.AbsRequest<Connection.Request> {

        @Override
        public Connection.Request decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            VodDescriptor desc = context.getSerializer(VodDescriptor.class).decode(context, buf);
            return new Connection.Request((UUID) shellObj.get(ID_F), desc);
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, Connection.Request obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            context.getSerializer(VodDescriptor.class).encode(context, buf, obj.desc);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, Connection.Request obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += context.getSerializer(VodDescriptor.class).getSize(context, obj.desc);
            return size;
        }
    }

    public static final class Response extends GvodMsgSerializer.AbsResponse<Connection.Response> {

        @Override
        public Connection.Response decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            return new Connection.Response((UUID) shellObj.get(ID_F), (ReqStatus) shellObj.get(STATUS_F));
        }

    }

    public static final class Update extends GvodMsgSerializer.AbsOneWay<Connection.Update> {

        @Override
        public Connection.Update decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            VodDescriptor desc = context.getSerializer(VodDescriptor.class).decode(context, buf);
            return new Connection.Update((UUID)shellObj.get(ID_F), desc);
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, Connection.Update obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            context.getSerializer(VodDescriptor.class).encode(context, buf, obj.desc);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, Connection.Update obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += context.getSerializer(VodDescriptor.class).getSize(context, obj.desc);
            return size;
        }

    }

    public static final class Close extends GvodMsgSerializer.AbsOneWay<Connection.Close> {

        @Override
        public Connection.Close decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            return new Connection.Close((UUID)shellObj.get(ID_F));
        }
    }
}
