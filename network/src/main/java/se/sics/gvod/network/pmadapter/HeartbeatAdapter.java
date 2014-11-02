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
package se.sics.gvod.network.pmadapter;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import se.sics.gvod.common.msg.peerMngr.Heartbeat;
import se.sics.gvod.network.GVoDAdapterFactory;
import se.sics.gvod.network.Util;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HeartbeatAdapter {

    public static class OneWay implements GVoDAdapter<Heartbeat.OneWay> {

        @Override
        public Heartbeat.OneWay decode(ByteBuf buffer) {
            UUID id = Util.decodeUUID(buffer);
            int overlays = buffer.readInt();
            Map<Integer, Integer> overlayUtility = new HashMap<Integer, Integer>();
            for (int i = 0; i < overlays; i++) {
                int overlayId = buffer.readInt();
                int utility = buffer.readInt();
                overlayUtility.put(overlayId, utility);
            }

            return new Heartbeat.OneWay(id, overlayUtility);
        }

        @Override
        public ByteBuf encode(Heartbeat.OneWay oneWay, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.OVERLAY_HEARTBEAT);
            
            Util.encodeUUID(buffer, oneWay.id);
            buffer.writeInt(oneWay.overlaysUtility.size());
            for (Map.Entry<Integer, Integer> e : oneWay.overlaysUtility.entrySet()) {
                buffer.writeInt(e.getKey());
                buffer.writeInt(e.getValue());
            }

            return buffer;
        }

        @Override
        public int getEncodedSize(Heartbeat.OneWay req) {
            int size = 0;
            size += 1; //type
            size += Util.getUUIDEncodedSize();
            size += 4; //overlays size;
            size += req.overlaysUtility.size()*(4 + 4); //overlayId + utility
            return size;
        }

    }
}
