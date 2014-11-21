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
import java.util.Map;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.OverlaySample;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;
import se.sics.gvod.network.serializers.base.GvodMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class OverlaySampleSerializer {

    public static class Request extends GvodMsgSerializer.AbsRequest<OverlaySample.Request> {

        @Override
        public OverlaySample.Request decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            int overlayId = buf.readInt();
            return new OverlaySample.Request((UUID)shellObj.get(ID_F), overlayId);
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, OverlaySample.Request obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            buf.writeInt(obj.overlayId);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, OverlaySample.Request obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += Integer.SIZE / 8; //overlayId
            return size;
        }

    }

    public static class Response extends GvodMsgSerializer.AbsResponse<OverlaySample.Response> {

        @Override
        public OverlaySample.Response decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            int overlayId = buf.readInt();
            if(!shellObj.get(STATUS_F).equals(ReqStatus.SUCCESS)) {
                return new OverlaySample.Response((UUID) shellObj.get(ID_F), (ReqStatus) shellObj.get(STATUS_F), overlayId, null);
            }
            Map<VodAddress, Integer> overlaySample = new HashMap<VodAddress, Integer>();
            int overlaySampleSize = buf.readInt();
            Serializer<VodAddress> vodAddressSerializer = context.getSerializer(VodAddress.class);
            for (int i = 0; i < overlaySampleSize; i++) {
                overlaySample.put(vodAddressSerializer.decode(context, buf), buf.readInt());
            }
            return new OverlaySample.Response((UUID) shellObj.get(ID_F), (ReqStatus) shellObj.get(STATUS_F), overlayId, overlaySample);
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, OverlaySample.Response obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            buf.writeInt(obj.overlayId);
            if(!obj.status.equals(ReqStatus.SUCCESS)) {
                return buf;
            }
            buf.writeInt(obj.overlaySample.size());
            Serializer<VodAddress> vodAddressSerializer = context.getSerializer(VodAddress.class);
            for (Map.Entry<VodAddress, Integer> e : obj.overlaySample.entrySet()) {
                vodAddressSerializer.encode(context, buf, e.getKey());
                buf.writeInt(e.getValue());
            }
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, OverlaySample.Response obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += Integer.SIZE / 8; //overlayId
            if(!obj.status.equals(ReqStatus.SUCCESS)) {
                return size;
            }
            size += Integer.SIZE / 8; //sampleSize
            Serializer<VodAddress> vodAddressSerializer = context.getSerializer(VodAddress.class);
            for (Map.Entry<VodAddress, Integer> e : obj.overlaySample.entrySet()) {
                size += vodAddressSerializer.getSize(context, e.getKey());
                size += Integer.SIZE / 8;
            }
            return size;
        }
    }
}
