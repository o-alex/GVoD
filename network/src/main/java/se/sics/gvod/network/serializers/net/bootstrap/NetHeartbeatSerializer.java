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

package se.sics.gvod.network.serializers.net.bootstrap;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import se.sics.gvod.common.msg.peerMngr.Heartbeat;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.netmsg.HeaderField;
import se.sics.gvod.network.netmsg.bootstrap.NetHeartbeat;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.net.base.NetMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetHeartbeatSerializer {
    public static class OneWay extends NetMsgSerializer.AbsOneWay<NetHeartbeat.OneWay> {
        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, NetHeartbeat.OneWay obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsOneWay(context, buf, obj);
            context.getSerializer(Heartbeat.OneWay.class).encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public NetHeartbeat.OneWay decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absOne = decodeAbsOneWay(context, buf);
            Heartbeat.OneWay content = context.getSerializer(Heartbeat.OneWay.class).decode(context, buf);
            return new NetHeartbeat.OneWay(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absOne.getValue0(), absOne.getValue1(), content);
        }

        @Override
        public int getSize(SerializationContext context, NetHeartbeat.OneWay obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsOneWay(context, obj);
            size += context.getSerializer(Heartbeat.OneWay.class).getSize(context, obj.content);
            return size;
        }

    }
    
}
