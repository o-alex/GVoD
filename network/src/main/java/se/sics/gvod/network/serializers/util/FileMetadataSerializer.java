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
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class FileMetadataSerializer implements Serializer<FileMetadata> {

    @Override
    public ByteBuf encode(SerializationContext context, ByteBuf buf, FileMetadata obj) throws SerializerException, SerializationContext.MissingException {
        buf.writeInt(obj.fileSize);
        buf.writeInt(obj.pieceSize);
        buf.writeInt(obj.hashFileSize);
        byte[] hashAlg = obj.hashAlg.getBytes();
        buf.writeInt(hashAlg.length);
        buf.writeBytes(hashAlg);
        return buf;
    }

    @Override
    public FileMetadata decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
        int fileSize = buf.readInt();
        int pieceSize = buf.readInt();
        int hashFileSize = buf.readInt();
        int hashAlgSize = buf.readInt();
        byte[] hashAlg = new byte[hashAlgSize];
        buf.readBytes(hashAlg);
        return new FileMetadata(fileSize, pieceSize, new String(hashAlg), hashFileSize);
    }

    @Override
    public int getSize(SerializationContext context, FileMetadata obj) throws SerializerException, SerializationContext.MissingException {
        int size = 0;
        size += Integer.SIZE / 8; // fileSize
        size += Integer.SIZE / 8; // pieceSize
        size += Integer.SIZE / 8; // hashFileSize
        size += Integer.SIZE / 8; // hashAlg byte array size;
        size += obj.hashAlg.getBytes().length * (Byte.SIZE / 8); //hashAlg byte array
        return size;
    }
    
}
