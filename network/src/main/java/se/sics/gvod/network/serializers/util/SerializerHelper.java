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

package se.sics.gvod.network.serializers.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.javatuples.Triplet;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SerializerHelper {
    
    public static byte[] serializeOverlayData(SerializationContext context, VodAddress node, long timestamp, int overlayUtility) throws SerializationContext.MissingException, Serializer.SerializerException {
        ByteBuf buf = Unpooled.buffer();
        buf.writeLong(timestamp);
        buf.writeInt(overlayUtility);
        context.getSerializer(VodAddress.class).encode(context, buf, node);
        return buf.array();
    }
    
    public static Triplet<VodAddress, Long, Integer> deserializeOverlayData(SerializationContext context, ByteBuf buf) throws Serializer.SerializerException, SerializationContext.MissingException {
        long timestamp = buf.readLong();
        int overlayUtility = buf.readInt();
        VodAddress node = context.getSerializer(VodAddress.class).decode(context, buf);
        return Triplet.with(node, timestamp, overlayUtility);
    }
}
