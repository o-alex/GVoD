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
package se.sics.gvod.network.serializers.bootstrap;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.OverlaySample;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class OverlaySampleSerializer {

    public static class Request implements Serializer {
        private final int id;
        
        public Request(int id) {
            this.id = id;
        }
        
        @Override
        public int identifier() {
            return id;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            OverlaySample.Request obj = (OverlaySample.Request)o;
            Serializers.lookupSerializer(UUID.class).toBinary(obj.id, buf);
            buf.writeInt(obj.overlayId);
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
             UUID mId = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
             int overlayId = buf.readInt();
             return new OverlaySample.Request(mId, overlayId);
        }
    }

    public static class Response implements Serializer {
        private final int id;
        
        public Response(int id) {
            this.id = id;
        }

        @Override
        public int identifier() {
            return id;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            OverlaySample.Response obj = (OverlaySample.Response)o;
            Serializers.lookupSerializer(UUID.class).toBinary(obj.id, buf);
            Serializers.lookupSerializer(ReqStatus.class).toBinary(obj.status, buf);
            
            buf.writeInt(obj.overlayId);
            if(!obj.status.equals(ReqStatus.SUCCESS)) {
                return;
            }
            buf.writeInt(obj.overlaySample.size());
            Serializer decoratedAddressSerializer = Serializers.lookupSerializer(DecoratedAddress.class);
            for (Map.Entry<DecoratedAddress, Integer> e : obj.overlaySample.entrySet()) {
                decoratedAddressSerializer.toBinary(e.getKey(), buf);
                buf.writeInt(e.getValue());
            }
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            UUID mId = (UUID) Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
            ReqStatus status = (ReqStatus) Serializers.lookupSerializer(ReqStatus.class).fromBinary(buf, hint);
            
            int overlayId = buf.readInt();
            if(!status.equals(ReqStatus.SUCCESS)) {
                return new OverlaySample.Response(mId, status, overlayId, null);
            }
            int overlaySampleSize = buf.readInt();
            Map<DecoratedAddress, Integer> overlaySample = new HashMap<DecoratedAddress, Integer>();
            Serializer decoratedAddressSerializer = Serializers.lookupSerializer(DecoratedAddress.class);
            for (int i = 0; i < overlaySampleSize; i++) {
                DecoratedAddress node = (DecoratedAddress)decoratedAddressSerializer.fromBinary(buf, hint);
                int utility = buf.readInt();
                overlaySample.put(node, utility);
            }
            return new OverlaySample.Response(mId, status, overlayId, overlaySample);
        }
    }
}
