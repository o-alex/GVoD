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
package se.sics.gvod.common.msg;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;

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
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public RewriteableMsg copy() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public byte getOpcode() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public RewriteableMsg copy() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public byte getOpcode() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public RewriteableMsg copy() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public byte getOpcode() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
