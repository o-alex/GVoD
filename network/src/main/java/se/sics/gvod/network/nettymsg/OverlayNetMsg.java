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
package se.sics.gvod.network.nettymsg;

import io.netty.buffer.ByteBuf;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.GVoDAdapterFactory;
import se.sics.gvod.network.GVoDNetFrameDecoder;
import se.sics.gvod.network.pmadapter.GVoDAdapter;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class OverlayNetMsg {

    private static SerializationContext context;

    public void setContext(SerializationContext context) {
        this.context = context;
    }

    public static class Request<E extends GvodMsg.Request> extends DirectMsgNetty.Request {

        public final int overlayId;
        public final E payload;

        public Request(VodAddress vodSrc, VodAddress vodDest, int overlayId, E payload) {
            super(vodSrc, vodDest);

            //TODO ALEX fix later
            setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
            //fix

            this.overlayId = overlayId;
            this.payload = payload;
        }

        public <E extends GvodMsg.Response> Response getResponse(E payload) {
            return new Response(vodDest, vodSrc, overlayId, payload);
        }

        @Override
        public String toString() {
            return payload.toString() + "src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString() + "overlay " + overlayId;
        }

        @Override
        public int getSize() {
            try {
                int size = getHeaderSize();
                size += Byte.SIZE / 8; //payload code
                size += Integer.SIZE / 8; //overlayId
                Serializer<E> serializer = (Serializer<E>) context.getSerializer(payload.getClass());
                size += serializer.getSize(context, payload);
                return size;
            } catch (SerializationContext.MissingException ex) {
                throw new RuntimeException(ex);
            } catch (Serializer.SerializerException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public Request<E> copy() {
            return new Request<E>(vodSrc, vodDest, overlayId, (E) payload.copy());
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            try {
                ByteBuf buffer = createChannelBufferWithHeader();
                buffer.writeByte(context.getOpcode(payload.getClass()));
                buffer.writeInt(overlayId);
                Serializer<E> serializer = (Serializer<E>) context.getSerializer(payload.getClass());
                serializer.encode(context, buffer, payload);
                return buffer;
            } catch (SerializationContext.MissingException ex) {
                throw new MessageEncodingException("missing serializer");
            } catch (Serializer.SerializerException ex) {
                throw new MessageEncodingException("serialization exception");
            }
        }

        @Override
        public byte getOpcode() {
            return GVoDNetFrameDecoder.OVERLAY_NET_REQUEST;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 83 * hash + this.overlayId;
            hash = 83 * hash + (this.payload != null ? this.payload.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Request<?> other = (Request<?>) obj;
            if (this.overlayId != other.overlayId) {
                return false;
            }
            if (this.payload != other.payload && (this.payload == null || !this.payload.equals(other.payload))) {
                return false;
            }
            return true;
        }
    }

    public static class Response<E extends GvodMsg.Response> extends DirectMsgNetty.Response {

        public final int overlayId;
        public final E payload;

        public Response(VodAddress vodSrc, VodAddress vodDest, int overlayId, E payload) {
            super(vodSrc, vodDest);
            //TODO ALEX fix later
            setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
            //fix

            this.overlayId = overlayId;
            this.payload = payload;
        }

        @Override
        public String toString() {
            return payload.toString() + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString() + "overlay " + overlayId;
        }

        @Override
        public int getSize() {
            try {
                int size = getHeaderSize();
                size += Byte.SIZE / 8; //payload code
                size += Integer.SIZE / 8; //overlayId
                Serializer<E> serializer = (Serializer<E>) context.getSerializer(payload.getClass());
                size += serializer.getSize(context, payload);
                size += GVoDAdapterFactory.getAdapter(payload).getEncodedSize(this);
                return size;
            } catch (SerializationContext.MissingException ex) {
                throw new RuntimeException(ex);
            } catch (Serializer.SerializerException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public Response<E> copy() {
            return new Response<E>(vodSrc, vodDest, overlayId, (E) payload.copy());
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            try {
                ByteBuf buffer = createChannelBufferWithHeader();
                buffer.writeByte(context.getOpcode(payload.getClass()));
                buffer.writeInt(overlayId);
                Serializer<E> serializer = (Serializer<E>) context.getSerializer(payload.getClass());
                serializer.encode(context, buffer, payload);
                return buffer;
            } catch (SerializationContext.MissingException ex) {
                throw new MessageEncodingException("missing serializer");
            } catch (Serializer.SerializerException ex) {
                throw new MessageEncodingException("serialization exception");
            }
        }

        @Override
        public byte getOpcode() {
            return GVoDNetFrameDecoder.OVERLAY_NET_RESPONSE;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + this.overlayId;
            hash = 79 * hash + (this.payload != null ? this.payload.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Response<?> other = (Response<?>) obj;
            if (this.overlayId != other.overlayId) {
                return false;
            }
            if (this.payload != other.payload && (this.payload == null || !this.payload.equals(other.payload))) {
                return false;
            }
            return true;
        }
    }

    public static class OneWay<E extends GvodMsg.OneWay> extends DirectMsgNetty.Oneway {

        public final int overlayId;
        public final E payload;

        public OneWay(VodAddress vodSrc, VodAddress vodDest, int overlayId, E payload) {
            super(vodSrc, vodDest);

            this.overlayId = overlayId;
            this.payload = payload;
        }

        @Override
        public String toString() {
            return payload.toString() + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString() + " overlay " + overlayId;
        }

        @Override
        public int getSize() {
            try {
                int size = getHeaderSize();
                size += Byte.SIZE / 8; //payload code
                size += Integer.SIZE/8; //overlayId
                Serializer<E> serializer = (Serializer<E>) context.getSerializer(payload.getClass());
                size += serializer.getSize(context, payload);
                return size;
            } catch (SerializationContext.MissingException ex) {
                throw new RuntimeException(ex);
            } catch (Serializer.SerializerException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public OneWay<E> copy() {
            return new OneWay<E>(vodSrc, vodDest, overlayId, (E) payload.copy());
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            try {
                ByteBuf buffer = createChannelBufferWithHeader();
                buffer.writeByte(context.getOpcode(payload.getClass()));
                buffer.writeInt(overlayId);
                Serializer<E> serializer = (Serializer<E>) context.getSerializer(payload.getClass());
                serializer.encode(context, buffer, payload);
                return buffer;
            } catch (SerializationContext.MissingException ex) {
                throw new MessageEncodingException("serializer not found");
            } catch (Serializer.SerializerException ex) {
                throw new MessageEncodingException("serialization exception");
            }
        }

        @Override
        public byte getOpcode() {
            return GVoDNetFrameDecoder.OVERLAY_NET_ONEWAY;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + this.overlayId;
            hash = 89 * hash + (this.payload != null ? this.payload.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final OneWay<?> other = (OneWay<?>) obj;
            if (this.overlayId != other.overlayId) {
                return false;
            }
            if (this.payload != other.payload && (this.payload == null || !this.payload.equals(other.payload))) {
                return false;
            }
            return true;
        }
    }
}
