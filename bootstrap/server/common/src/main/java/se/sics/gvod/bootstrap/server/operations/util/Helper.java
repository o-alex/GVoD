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

package se.sics.gvod.bootstrap.server.operations.util;

import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.javatuples.Triplet;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;
import se.sics.gvod.network.serializers.util.SerializerHelper;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Helper {
    public static Map<VodAddress, Integer> processOverlaySample(SerializationContext context, Set<byte[]> boverlaySample) throws Serializer.SerializerException, SerializationContext.MissingException {
        Map<VodAddress, Integer> overlaySample = new HashMap<VodAddress, Integer>();
        //TODO Alex fix hardcoded timestamp old
        long oldTimestamp = System.nanoTime() - 25*1000*1000;
        for (byte[] peer : boverlaySample) {
            Triplet<VodAddress, Long, Integer> node = SerializerHelper.deserializeOverlayData(context, Unpooled.wrappedBuffer(peer));
            if(oldTimestamp < node.getValue1()) {
                overlaySample.put(node.getValue0(), node.getValue2());
            }
        }
        return overlaySample;
    }
}
