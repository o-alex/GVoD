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
import se.sics.gvod.common.util.VodDescriptor;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VodDescriptorSerializer implements Serializer<VodDescriptor> {

    @Override
    public ByteBuf encode(SerializationContext context, ByteBuf buf, VodDescriptor obj) throws SerializerException {
        buf.writeInt(obj.downloadPos);
        return buf;
    }

    @Override
    public VodDescriptor decode(SerializationContext context, ByteBuf buf) throws SerializerException {
        int downloadPos = buf.readInt();
        return new VodDescriptor(downloadPos);
    }

    @Override
    public int getSize(SerializationContext context, VodDescriptor obj) throws SerializerException {
        return Integer.SIZE/8;
    }
    
}
