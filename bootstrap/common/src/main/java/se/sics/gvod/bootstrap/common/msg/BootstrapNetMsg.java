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

package se.sics.gvod.bootstrap.common.msg;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.msgs.RewriteableMsg;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapNetMsg {
    public static class Request extends DirectMsgNetty.Request {

        public Request(VodAddress vodSrc, VodAddress vodDest) {
            super(vodSrc, vodDest);
        }
        
        public Response getResponse() {
            return new Response(vodDest, vodSrc);
        }
        
        @Override
        public int getSize() {
            return super.getHeaderSize();
        }

        @Override
        public RewriteableMsg copy() {
            return new Request(vodSrc, vodDest);
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buf = createChannelBufferWithHeader();
            return buf;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.BOOTSTRAP_REQUEST;
        }
        
        @Override
        public String toString() {
            return "BootstrapNetMsg.Request from " + vodSrc.toString() + " to " + vodDest.toString();
        }
    }
    
    public static class Response extends DirectMsgNetty.Response {

        public Response(VodAddress vodSrc, VodAddress vodDest) {
            super(vodSrc, vodDest);
        }
        @Override
        public int getSize() {
            return super.getHeaderSize();
        }

        @Override
        public RewriteableMsg copy() {
            return new Response(vodSrc, vodDest);
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buf = createChannelBufferWithHeader();
            return buf;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.BOOTSTRAP_RESPONSE;
        }
        
        @Override
        public String toString() {
            return "BootstrapNetMsg.Response from " + vodSrc.toString() + " to " + vodDest.toString();
        }
    }
}