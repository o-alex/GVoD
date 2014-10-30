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
import java.util.Map;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.tags.Tag;
import se.sics.gvod.network.tags.TagType;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MyNetMsg {

    public static class Request<E extends GvodMsg.Request> extends DirectMsgNetty.Request {

        public final Map<TagType, Tag> tags;
        public final E payload;

        public Request(VodAddress vodSrc, VodAddress vodDest, Map<TagType, Tag> tags, E payload) {
            super(vodSrc, vodDest);

            //TODO ALEX fix later
            setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
            //fix

            this.tags = tags;
            this.payload = payload;
        }

        public <E extends GvodMsg.Response> Response getResponse(E payload) {
            return new Response(vodDest, vodSrc, tags, payload);
        }

        @Override
        public String toString() {
            return payload.toString() + "src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString() + tags;
        }

        @Override
        public int getSize() {
            throw new UnsupportedOperationException("serialization");
        }

        @Override
        public Request<E> copy() {
            return new Request<E>(vodSrc, vodDest, tags, (E) payload.copy());
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            throw new UnsupportedOperationException("serialization");
        }

        @Override
        public byte getOpcode() {
            throw new UnsupportedOperationException("serialization");
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + (this.tags != null ? this.tags.hashCode() : 0);
            hash = 71 * hash + (this.payload != null ? this.payload.hashCode() : 0);
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
            if (this.tags != other.tags && (this.tags == null || !this.tags.equals(other.tags))) {
                return false;
            }
            if (this.payload != other.payload && (this.payload == null || !this.payload.equals(other.payload))) {
                return false;
            }
            return true;
        }
    }
    
    public static class Response<E extends GvodMsg.Response> extends DirectMsgNetty.Response {

        public final Map<TagType, Tag> tags;
        public final E payload;

        public Response(VodAddress vodSrc, VodAddress vodDest, Map<TagType, Tag> tags, E payload) {
            super(vodSrc, vodDest);
            //TODO ALEX fix later
            setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
            //fix

            this.tags = tags;
            this.payload = payload;
        }

        @Override
        public String toString() {
            return payload.toString() + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString() + tags;
        }

        @Override
        public int getSize() {
            throw new UnsupportedOperationException("serialization");
        }

        @Override
        public Response<E> copy() {
            return new Response<E>(vodSrc, vodDest, tags, (E) payload.copy());
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            throw new UnsupportedOperationException("serialization");
        }

        @Override
        public byte getOpcode() {
            throw new UnsupportedOperationException("serialization");
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + (this.tags != null ? this.tags.hashCode() : 0);
            hash = 97 * hash + (this.payload != null ? this.payload.hashCode() : 0);
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
            if (this.tags != other.tags && (this.tags == null || !this.tags.equals(other.tags))) {
                return false;
            }
            if (this.payload != other.payload && (this.payload == null || !this.payload.equals(other.payload))) {
                return false;
            }
            return true;
        }
    }

    public static class OneWay<E extends GvodMsg.OneWay> extends DirectMsgNetty.Oneway {

        public final Map<TagType, Tag> tags;
        public final E payload;

        public OneWay(VodAddress vodSrc, VodAddress vodDest, Map<TagType, Tag> tags, E payload) {
            super(vodSrc, vodDest);
            
            this.tags = tags;
            this.payload = payload;
        }

        @Override
        public String toString() {
            return payload.toString() + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString() + tags;
        }

        @Override
        public int getSize() {
            throw new UnsupportedOperationException("serialization");
        }

        @Override
        public OneWay<E> copy() {
            return new OneWay<E>(vodSrc, vodDest, tags, (E) payload.copy());
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            throw new UnsupportedOperationException("serialization");
        }

        @Override
        public byte getOpcode() {
            throw new UnsupportedOperationException("serialization");
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + (this.tags != null ? this.tags.hashCode() : 0);
            hash = 23 * hash + (this.payload != null ? this.payload.hashCode() : 0);
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
            if (this.tags != other.tags && (this.tags == null || !this.tags.equals(other.tags))) {
                return false;
            }
            if (this.payload != other.payload && (this.payload == null || !this.payload.equals(other.payload))) {
                return false;
            }
            return true;
        }
    }
}
