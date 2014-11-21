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
import se.sics.gvod.common.msg.peerMngr.Heartbeat;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer.SerializerException;
import se.sics.gvod.network.serializers.base.GvodMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HeartbeatSerializer {

    public static class OneWay extends GvodMsgSerializer.AbsOneWay<Heartbeat.OneWay> {

        @Override
        public Heartbeat.OneWay decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);

            Map<Integer, Integer> overlayUtilities = new HashMap<Integer, Integer>();
            int nrOverlays = buf.readInt();
            for (int i = 0; i < nrOverlays; i++) {
                overlayUtilities.put(buf.readInt(), buf.readInt());
            }

            return new Heartbeat.OneWay((UUID)shellObj.get(ID_F), overlayUtilities);
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, Heartbeat.OneWay obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            buf.writeInt(obj.overlayUtilities.size());
            for (Map.Entry<Integer, Integer> e : obj.overlayUtilities.entrySet()) {
                buf.writeInt(e.getKey());
                buf.writeInt(e.getValue());
            }
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, Heartbeat.OneWay obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += Integer.SIZE / 8; //nr of overlays
            for (Map.Entry<Integer, Integer> e : obj.overlayUtilities.entrySet()) {
                size += Integer.SIZE / 8; //overlayId
                size += Integer.SIZE / 8; //utility
            }
            return size;
        }

    }
}
