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
package se.sics.gvod.network;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import org.javatuples.Pair;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.util.UserTypesDecoderFactory;
import se.sics.gvod.net.util.UserTypesEncoderFactory;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Util {

    public static ByteBuf encodeUUID(ByteBuf buffer, UUID id) {
        buffer.writeLong(id.getMostSignificantBits());
        buffer.writeLong(id.getLeastSignificantBits());
        return buffer;
    }

    public static UUID decodeUUID(ByteBuf buffer) {
        Long uuidMSB = buffer.readLong();
        Long uuidLSB = buffer.readLong();
        return new UUID(uuidMSB, uuidLSB);
    }

    public static int getUUIDEncodedSize() {
        return 8 + 8; //2 longs
    }

    public static ByteBuf encodeVodAddress(ByteBuf buffer, VodAddress address) {
        try {
            UserTypesEncoderFactory.writeVodAddress(buffer, address);
            return buffer;
        } catch (MessageEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static VodAddress decodeVodAddress(ByteBuf buffer) {
        try {
            return UserTypesDecoderFactory.readVodAddress(buffer);
        } catch (MessageDecodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int getVodAddressEncodedSize(VodAddress address) {
        int size = 0;
        size += UserTypesEncoderFactory.ADDRESS_LEN; // address
        size += 4; // overlayId
        size += 1; //natPolicy
        size += (address.getParents().size() == 0 ? 2 : 2 + address.getParents().size() * UserTypesEncoderFactory.ADDRESS_LEN);
        return size;
    }

    public static ByteBuf encodeReqStatus(ByteBuf buffer, ReqStatus status) {
        switch (status) {
            case FAIL:
                buffer.writeByte(0);
                break;
            case SUCCESS:
                buffer.writeByte(1);
                break;
            default:
                throw new RuntimeException("no code for encoding status " + status);
        }
        return buffer;
    }

    public static ReqStatus decodeReqStatus(ByteBuf buffer) {
        byte statusB = buffer.readByte();
        switch (statusB) {
            case 0x00:
                return ReqStatus.FAIL;
            case 0x01:
                return ReqStatus.SUCCESS;
            default:
                throw new RuntimeException("no code for decoding status byte " + statusB);
        }
    }

    public static int getReqStatusEncodedSize() {
        return 1;
    }

    public static ByteBuf encodeFileMeta(ByteBuf buffer, FileMetadata fileMeta) {
        buffer.writeInt(fileMeta.size);
        buffer.writeInt(fileMeta.pieceSize);
        return buffer;
    }

    public static FileMetadata decodeFileMeta(ByteBuf buffer) {
        int fileSize = buffer.readInt();
        int pieceSize = buffer.readInt();
        return new FileMetadata(fileSize, pieceSize);
    }

    public static int getFileMetaEncodedSize() {
        int size = 0;
        size += 4; //fileSize
        size += 4; //pieceSize
        return size;
    }
    
    public static ByteBuf encodeHeartbeatEntry(ByteBuf buffer, VodAddress peer, int utility) {
        encodeVodAddress(buffer, peer);
        buffer.writeInt(utility);
        return buffer;
    }
    
    public static Pair<VodAddress, Integer> decodeHeartbeatEntry(ByteBuf buffer) {
        VodAddress peer = decodeVodAddress(buffer);
        int utility = buffer.readInt();
        return Pair.with(peer, utility);
    }
    
    public static int getHeartbeatEntryEncodedSize(VodAddress peer) {
        int size = 0;
        size += getVodAddressEncodedSize(peer);
        size += 4; //utility
        return size;
    }
}
