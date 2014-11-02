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
import se.sics.gvod.common.msg.builder.GVoDMsgBuilder;
import se.sics.gvod.common.msg.builder.vod.ConnectionBuilder;
import se.sics.gvod.common.msg.vod.Connection;
import se.sics.gvod.common.util.VodDescriptor;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;
import se.sics.gvod.network.serializers.base.GvodMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ConnectionSerializer {

    public static abstract class AbsRequest<E extends Connection.Request, F extends ConnectionBuilder.Request> extends GvodMsgSerializer.AbsRequest<E, F> {

        @Override
        public F decode(SerializationContext context, ByteBuf buf, F shellObj) throws SerializerException, SerializationContext.MissingException {
            super.decode(context, buf, shellObj);
            VodDescriptor desc = context.getSerializer(VodDescriptor.class).decode(context, buf);
            shellObj.setVodDescriptor(desc);
            return shellObj;
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, E obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            context.getSerializer(VodDescriptor.class).encode(context, buf, obj.desc);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, E obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += context.getSerializer(VodDescriptor.class).getSize(context, obj.desc);
            return size;
        }
    }

    public static final class Request extends AbsRequest<Connection.Request, ConnectionBuilder.Request> {

        @Override
        public Connection.Request decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            try {
                return decode(context, buf, new ConnectionBuilder.Request()).finalise();
            } catch (GVoDMsgBuilder.IncompleteException ex) {
                throw new SerializerException(ex);
            }
        }

    }

    public static abstract class AbsResponse<E extends Connection.Response, F extends ConnectionBuilder.Response> extends GvodMsgSerializer.AbsResponse<E, F> {
    }

    public static class Response extends AbsResponse<Connection.Response, ConnectionBuilder.Response> {

        @Override
        public Connection.Response decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            try {
                return decode(context, buf, new ConnectionBuilder.Response()).finalise();
            } catch (GVoDMsgBuilder.IncompleteException ex) {
                throw new SerializerException(ex);
            }
        }

    }

    public static abstract class AbsUpdate<E extends Connection.Update, F extends ConnectionBuilder.Update> extends GvodMsgSerializer.AbsOneWay<E, F> {

        @Override
        public F decode(SerializationContext context, ByteBuf buf, F shellObj) throws SerializerException, SerializationContext.MissingException {
            super.decode(context, buf, shellObj);
            VodDescriptor desc = context.getSerializer(VodDescriptor.class).decode(context, buf);
            shellObj.setVodDescriptor(desc);
            return shellObj;
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, E obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            context.getSerializer(VodDescriptor.class).encode(context, buf, obj.desc);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, E obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += context.getSerializer(VodDescriptor.class).getSize(context, obj.desc);
            return size;
        }
    }

    public static final class Update extends AbsUpdate<Connection.Update, ConnectionBuilder.Update> {

        @Override
        public Connection.Update decode(SerializationContext context, ByteBuf buf) throws Serializer.SerializerException, SerializationContext.MissingException {
            try {
                return decode(context, buf, new ConnectionBuilder.Update()).finalise();
            } catch (GVoDMsgBuilder.IncompleteException ex) {
                throw new Serializer.SerializerException(ex);
            }
        }
    }

    public static abstract class AbsClose<E extends Connection.Close, F extends ConnectionBuilder.Close> extends GvodMsgSerializer.AbsOneWay<E, F> {
    }

    public static final class Close extends AbsClose<Connection.Close, ConnectionBuilder.Close> {

        @Override
        public Connection.Close decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            try {
                return decode(context, buf, new ConnectionBuilder.Close()).finalise();
            } catch (GVoDMsgBuilder.IncompleteException ex) {
                throw new SerializerException(ex);
            }
        }

    }
}
