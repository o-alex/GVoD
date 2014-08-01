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
import java.util.Map;
import java.util.Set;
import se.sics.gvod.address.Address;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesEncoderFactory;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapNetMsg {
    public static class Request extends DirectMsgNetty.Request {

        public Request(VodAddress vodSrc, VodAddress vodDest) {
            super(vodSrc, vodDest);
        }
        
        public Response getResponse(Set<Address> systemSample) {
            return new Response(vodDest, vodSrc, systemSample);
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
        public final Set<Address> systemSample;
        
        public Response(VodAddress vodSrc, VodAddress vodDest, Set<Address> systemSample) {
            super(vodSrc, vodDest);
            this.systemSample = systemSample;
        }
        @Override
        public int getSize() {
            int size = super.getHeaderSize();
            size += UserTypesEncoderFactory.getListAddressSize(systemSample);
            return  size;
        }

        @Override
        public RewriteableMsg copy() {
            return new Response(vodSrc, vodDest, systemSample);
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeListAddresses(buffer, systemSample);
            return buffer;
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
    
    public static final class Heartbeat extends DirectMsgNetty.Oneway {
        public final Set<Integer> seeding;
        public final Map<Integer, Integer> leeching;
        
        public Heartbeat(VodAddress vodSrc, VodAddress vodDest, Set<Integer> seeding, Map<Integer, Integer> leeching) {
            super(vodSrc, vodDest);
            this.seeding = seeding;
            this.leeching = leeching;
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