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
package se.sics.gvod.network.serializers.vod;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.OverlaySample;
import se.sics.gvod.common.msg.vod.Download;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.base.GvodMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DownloadSerializer {

    public static final class DataRequest extends GvodMsgSerializer.AbsRequest<Download.DataRequest> {

        @Override
        public Download.DataRequest decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            int overlayId = buf.readInt();
            int pieceId = buf.readInt();
            return new Download.DataRequest((UUID) shellObj.get(ID_F), overlayId, pieceId);
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, Download.DataRequest obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            buf.writeInt(obj.overlayId);
            buf.writeInt(obj.pieceId);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, Download.DataRequest obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += Integer.SIZE / 8; //overlayId
            size += Integer.SIZE / 8; //pieceId
            return size;
        }
    }

    public static final class DataResponse extends GvodMsgSerializer.AbsResponse<Download.DataResponse> {

        @Override
        public Download.DataResponse decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            int overlayId = buf.readInt();
            int pieceId = buf.readInt();
            int size = buf.readInt();
            byte[] piece = new byte[size];
            buf.readBytes(piece);
            return new Download.DataResponse((UUID) shellObj.get(ID_F), (ReqStatus) shellObj.get(STATUS_F), overlayId, pieceId, piece);
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, Download.DataResponse obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            buf.writeInt(obj.overlayId);
            buf.writeInt(obj.pieceId);
            buf.writeInt(obj.piece.length);
            buf.writeBytes(obj.piece);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, Download.DataResponse obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += Integer.SIZE / 8; //overlayId
            size += Integer.SIZE / 8; //pieceId
            size += Integer.SIZE / 8; //size of piece
            size += obj.piece.length * Byte.SIZE / 8; //piece
            return size;
        }

    }

    public static final class HashRequest extends GvodMsgSerializer.AbsRequest<Download.HashRequest> {

        @Override
        public Download.HashRequest decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            int targetPos = buf.readInt();
            int nrHashes = buf.readInt();
            Set<Integer> hashes = new HashSet<Integer>();
            for (int i = 0; i < nrHashes; i++) {
                hashes.add(buf.readInt());
            }
            return new Download.HashRequest((UUID) shellObj.get(ID_F), targetPos, hashes);
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, Download.HashRequest obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            buf.writeInt(obj.targetPos);
            buf.writeInt(obj.hashes.size());
            for (Integer hash : obj.hashes) {
                buf.writeInt(hash);
            }
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, Download.HashRequest obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += Integer.SIZE / 8; //targetPos
            size += Integer.SIZE / 8; //size of hashes
            for (Integer hash : obj.hashes) {
                size += Integer.SIZE / 8; //hash
            }
            return size;
        }

    }

    public static final class HashResponse extends GvodMsgSerializer.AbsResponse<Download.HashResponse> {

        @Override
        public Download.HashResponse decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            int targetPos = buf.readInt();

            Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
            int nrHashes = buf.readInt();
            if (nrHashes > 0) {
                int hashSize = buf.readInt();
                byte[] hash;
                for (int i = 0; i < nrHashes; i++) {
                    int hashId = buf.readInt();
                    hash = new byte[hashSize];
                    buf.readBytes(hash);
                    hashes.put(hashId, hash);
                }
            }

            Set<Integer> missingHashes = new HashSet<Integer>();
            int nrMissingHashes = buf.readInt();
            for (int i = 0; i < nrMissingHashes; i++) {
                missingHashes.add(buf.readInt());
            }

            return new Download.HashResponse((UUID)shellObj.get(ID_F), (ReqStatus)shellObj.get(STATUS_F), targetPos, hashes, missingHashes);
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, Download.HashResponse obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            buf.writeInt(obj.targetPos);
            buf.writeInt(obj.hashes.size());
            if (obj.hashes.size() > 0) {
                int hashSize = obj.hashes.values().iterator().next().length;
                buf.writeInt(hashSize);

                for (Map.Entry<Integer, byte[]> e : obj.hashes.entrySet()) {
                    buf.writeInt(e.getKey());
                    buf.writeBytes(e.getValue());
                }
            }

            buf.writeInt(obj.missingHashes.size());
            for (Integer missingHash : obj.missingHashes) {
                buf.writeInt(missingHash);
            }
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, Download.HashResponse obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += Integer.SIZE / 8; //targetPos
            size += Integer.SIZE / 8; //hashes size
            if (obj.hashes.size() > 0) {
                size += Integer.SIZE / 8; //hash size
                int hashSize = obj.hashes.values().iterator().next().length;
                size += obj.hashes.size() * (Integer.SIZE / 8 + hashSize * Byte.SIZE / 8);
            }
            size += Integer.SIZE / 8; // missingHashes size
            size += obj.missingHashes.size() * (Integer.SIZE / 8);
            return size;
        }
    }
}
