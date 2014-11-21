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
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ReqStatusSerializer implements Serializer<ReqStatus> {

    @Override
    public ByteBuf encode(SerializationContext context, ByteBuf buf, ReqStatus obj) throws SerializerException {
        switch (obj) {
            case FAIL:
                buf.writeByte(0x00);
                break;
            case SUCCESS:
                buf.writeByte(0x01);
                break;
            case MISSING:
                buf.writeByte(0x02);
                break;
            case BUSY:
                buf.writeByte(0x03);
                break;
            case TIMEOUT:
                buf.writeByte(0x04);
                break;
            default:
                throw new SerializerException("no code for encoding status " + obj);
        }
        return buf;
    }

    @Override
    public ReqStatus decode(SerializationContext context, ByteBuf buf) throws SerializerException {
        byte status = buf.readByte();
        switch (status) {
            case 0x00:
                return ReqStatus.FAIL;
            case 0x01:
                return ReqStatus.SUCCESS;
            case 0x02:
                return ReqStatus.MISSING;
            case 0x03:
                return ReqStatus.BUSY;
            case 0x04:
                return ReqStatus.TIMEOUT;
            default:
                throw new SerializerException("no code for decoding status byte " + status);
        }
    }

    @Override
    public int getSize(SerializationContext context, ReqStatus obj) throws SerializerException, SerializationContext.MissingException {
        return Byte.SIZE / 8;
    }

}
