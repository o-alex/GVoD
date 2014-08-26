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
import java.util.Objects;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.GVoDAdapterFactory;
import se.sics.gvod.network.GVoDNetFrameDecoder;
import se.sics.gvod.network.gvodadapter.GVoDAdapter;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GvodNetMsg {

    public static class Request<E extends GvodMsg.Request> extends DirectMsgNetty.Request {

        public E payload;

        public Request(VodAddress vodSrc, VodAddress vodDest, E payload) {
            super(vodSrc, vodDest);
            this.payload = payload;
        }
        
        public <E extends GvodMsg.Response> Response getResponse(E payload) {
            return new Response(vodDest, vodSrc, payload);
        }
        
        @Override 
        public String toString() {
            return payload.toString() + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString();
        }

        @Override
        public int getSize() {
            GVoDAdapter adapter = GVoDAdapterFactory.getAdapter(payload);
            return getHeaderSize() + adapter.getEncodedSize(payload);
        }

        @Override
        public Request<E> copy() {
            return new Request<>(vodSrc, vodDest, (E) payload.copy());
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            GVoDAdapter<GvodMsg.Request> adapter = GVoDAdapterFactory.getAdapter(payload);
            adapter.encode(payload, buffer);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return GVoDNetFrameDecoder.GVOD_NET_REQUEST;
        }
        
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.payload);
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
            final Request<E> other = (Request<E>) obj;
            if (!Objects.equals(this.payload, other.payload)) {
                return false;
            }
            return true;
        }
    }

    public static class Response<E extends GvodMsg.Response> extends DirectMsgNetty.Response {

        public final E payload;

        public Response(VodAddress vodSrc, VodAddress vodDest, E payload) {
            super(vodSrc, vodDest);
            this.payload = payload;
        }
        
        @Override
        public String toString() {
            return payload.toString() + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString();
        }

        @Override
        public int getSize() {
            GVoDAdapter adapter = GVoDAdapterFactory.getAdapter(payload);
            return getHeaderSize() + adapter.getEncodedSize(payload);
        }

        @Override
        public Response<E> copy() {
            return new Response<>(vodSrc, vodDest, (E) payload.copy());
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            GVoDAdapter<GvodMsg.Response> adapter = GVoDAdapterFactory.getAdapter(payload);
            adapter.encode(payload, buffer);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return GVoDNetFrameDecoder.GVOD_NET_RESPONSE;
        }
        
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.payload);
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
            final Response<E> other = (Response<E>) obj;
            if (!Objects.equals(this.payload, other.payload)) {
                return false;
            }
            return true;
        }
    }

    public static class OneWay<E extends GvodMsg.OneWay> extends DirectMsgNetty.Oneway {

        public final E payload;

        public OneWay(VodAddress vodSrc, VodAddress vodDest, E payload) {
            super(vodSrc, vodDest);
            this.payload = payload;
        }
        
        @Override
        public String toString() {
            return payload.toString() + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString();
        }

        @Override
        public int getSize() {
            GVoDAdapter adapter = GVoDAdapterFactory.getAdapter(payload);
            return getHeaderSize() + adapter.getEncodedSize(payload);
        }

        @Override
        public OneWay<E> copy() {
            return new OneWay<E>(vodSrc, vodDest, (E) payload.copy());
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            GVoDAdapter<GvodMsg.OneWay> adapter = GVoDAdapterFactory.getAdapter(payload);
            adapter.encode(payload, buffer);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return GVoDNetFrameDecoder.GVOD_NET_ONEWAY;
        }
        
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.payload);
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
            final OneWay<E> other = (OneWay<E>) obj;
            if (!Objects.equals(this.payload, other.payload)) {
                return false;
            }
            return true;
        }
    }
}