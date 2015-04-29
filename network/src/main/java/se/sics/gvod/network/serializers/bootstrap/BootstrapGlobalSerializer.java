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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.BootstrapGlobal;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapGlobalSerializer {

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
            BootstrapGlobal.Request obj = (BootstrapGlobal.Request) o;
            Serializers.lookupSerializer(UUID.class).toBinary(obj.id, buf);
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            UUID mId = (UUID) Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
            return new BootstrapGlobal.Request(mId);
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
            BootstrapGlobal.Response obj = (BootstrapGlobal.Response) o;
            Serializers.lookupSerializer(UUID.class).toBinary(obj.id, buf);
            Serializers.lookupSerializer(ReqStatus.class).toBinary(obj.status, buf);
            if (obj.systemSample == null) {
                buf.writeInt(-1);
                return;
            }
            buf.writeInt(obj.systemSample.size());
            Serializer decoratedAddressSerializer = Serializers.lookupSerializer(DecoratedAddress.class);
            for(DecoratedAddress node : obj.systemSample) {
                decoratedAddressSerializer.toBinary(node, buf);
            }
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            UUID mId = (UUID) Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
            ReqStatus status = (ReqStatus) Serializers.lookupSerializer(ReqStatus.class).fromBinary(buf, hint);
            int overlaySampleSize = buf.readInt();
            if (overlaySampleSize == -1) {
                return new BootstrapGlobal.Response(mId, status, null);
            }

            Set<DecoratedAddress> overlaySample = new HashSet<DecoratedAddress>();
            Serializer decoratedAddressSerializer = Serializers.lookupSerializer(DecoratedAddress.class);
            for (int i = 0; i < overlaySampleSize; i++) {
                DecoratedAddress node = (DecoratedAddress) decoratedAddressSerializer.fromBinary(buf, hint);
                overlaySample.add(node);
            }
            return new BootstrapGlobal.Response(mId, status, overlaySample);
        }
    }
}
