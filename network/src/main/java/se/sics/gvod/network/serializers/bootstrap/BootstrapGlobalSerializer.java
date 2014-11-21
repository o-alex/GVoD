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

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.BootstrapGlobal;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;
import se.sics.gvod.network.serializers.base.GvodMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapGlobalSerializer {

    public static class Request extends GvodMsgSerializer.AbsRequest<BootstrapGlobal.Request> {

        @Override
        public BootstrapGlobal.Request decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            return new BootstrapGlobal.Request((UUID) shellObj.get(ID_F));
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, BootstrapGlobal.Request obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, BootstrapGlobal.Request obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            return size;
        }
    }

    public static class Response extends GvodMsgSerializer.AbsResponse<BootstrapGlobal.Response> {

        @Override
        public BootstrapGlobal.Response decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            int overlaySampleSize = buf.readInt();
            if (overlaySampleSize == -1) {
                return new BootstrapGlobal.Response((UUID) shellObj.get(ID_F), (ReqStatus) shellObj.get(STATUS_F), null);
            }

            Set<VodAddress> overlaySample = new HashSet<VodAddress>();
            Serializer<VodAddress> vodAddressSerializer = context.getSerializer(VodAddress.class);
            for (int i = 0; i < overlaySampleSize; i++) {
                overlaySample.add(vodAddressSerializer.decode(context, buf));
            }
            return new BootstrapGlobal.Response((UUID) shellObj.get(ID_F), (ReqStatus) shellObj.get(STATUS_F), overlaySample);
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, BootstrapGlobal.Response obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            if (obj.systemSample == null) {
                buf.writeInt(-1);
                return buf;
            }
            buf.writeInt(obj.systemSample.size());
            Serializer<VodAddress> vodAddressSerializer = context.getSerializer(VodAddress.class);
            for (VodAddress sample : obj.systemSample) {
                vodAddressSerializer.encode(context, buf, sample);
            }
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, BootstrapGlobal.Response obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += Integer.SIZE / 8; //sampleSize

            if (obj.systemSample != null) {
                Serializer<VodAddress> vodAddressSerializer = context.getSerializer(VodAddress.class);
                for (VodAddress sample : obj.systemSample) {
                    size += vodAddressSerializer.getSize(context, sample);
                }
            }
            return size;
        }
    }
}
