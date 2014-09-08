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

package se.sics.gvod.croupier.msg.intern;

import se.sics.gvod.croupier.CroupierMsg;
import com.google.common.base.Objects;
import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.common.network.LocalNettyAdapter;
import se.sics.gvod.croupier.network.CroupierRegistryImpl;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.SystemNetFrameDecoder;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierNettyMsg {
    public static class Request<E extends CroupierMsg.Request> extends DirectMsgNetty.Request {

        public E payload;

        public Request(VodAddress vodSrc, VodAddress vodDest, E payload) {
            super(vodSrc, vodDest);
            //TODO ALEX fix later
            setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
            //fix
            this.payload = payload;
        }
        
        public <E extends CroupierMsg.Response> Response getResponse(E payload) {
            return new Response(vodDest, vodSrc, payload);
        }
        
        @Override 
        public String toString() {
            return payload.toString() + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString();
        }

        @Override
        public int getSize() {
            LocalNettyAdapter adapter = CroupierRegistryImpl.getAdapter(payload);
            return getHeaderSize() + adapter.getEncodedSize(payload);
        }

        @Override
        public Request<E> copy() {
            return new Request<E>(vodSrc, vodDest, (E) payload.copy());
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            LocalNettyAdapter<CroupierMsg.Request> adapter = CroupierRegistryImpl.getAdapter(payload);
            adapter.encode(payload, buffer);
            return buffer;
        }

        //TODO Alex should fix - we don't want this dependency - my parent should know about frame decoder
        @Override
        public byte getOpcode() {
            return SystemNetFrameDecoder.CROUPIER_NET_REQUEST;
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
            if (!Objects.equal(this.payload, other.payload)) {
                return false;
            }
            return true;
        }
    }

    public static class Response<E extends CroupierMsg.Response> extends DirectMsgNetty.Response {

        public final E payload;

        public Response(VodAddress vodSrc, VodAddress vodDest, E payload) {
            super(vodSrc, vodDest);
            //TODO ALEX fix later
            setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
            //fix
            this.payload = payload;
        }
        
        @Override
        public String toString() {
            return payload.toString() + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString();
        }

        @Override
        public int getSize() {
            LocalNettyAdapter adapter = CroupierRegistryImpl.getAdapter(payload);
            return getHeaderSize() + adapter.getEncodedSize(payload);
        }

        @Override
        public Response<E> copy() {
            return new Response<E>(vodSrc, vodDest, (E) payload.copy());
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            LocalNettyAdapter<CroupierMsg.Response> adapter = CroupierRegistryImpl.getAdapter(payload);
            adapter.encode(payload, buffer);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return SystemNetFrameDecoder.GVOD_NET_RESPONSE;
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
            if (!Objects.equal(this.payload, other.payload)) {
                return false;
            }
            return true;
        }
    }
}
