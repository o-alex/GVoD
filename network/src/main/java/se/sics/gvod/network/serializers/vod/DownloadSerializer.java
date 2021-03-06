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

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.vod.Download;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DownloadSerializer {

    public static final class DataRequest implements Serializer {

        private final int id;

        public DataRequest(int id) {
            this.id = id;
        }

        @Override
        public int identifier() {
            return id;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            Download.DataRequest obj = (Download.DataRequest) o;
            Serializers.lookupSerializer(UUID.class).toBinary(obj.id, buf);
            buf.writeInt(obj.overlayId);
            buf.writeInt(obj.pieceId);
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            UUID mId = (UUID) Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
            int overlayId = buf.readInt();
            int pieceId = buf.readInt();
            return new Download.DataRequest(mId, overlayId, pieceId);
        }
    }

    public static final class DataResponse implements Serializer {

        private final int id;

        public DataResponse(int id) {
            this.id = id;
        }

        @Override
        public int identifier() {
            return id;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            Download.DataResponse obj = (Download.DataResponse) o;
            Serializers.lookupSerializer(UUID.class).toBinary(obj.id, buf);
            Serializers.lookupSerializer(ReqStatus.class).toBinary(obj.status, buf);
            buf.writeInt(obj.overlayId);
            buf.writeInt(obj.pieceId);
            if (obj.status.equals(ReqStatus.SUCCESS)) {
                buf.writeInt(obj.piece.length);
                buf.writeBytes(obj.piece);
            } else {
                //nothing
            }
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            UUID mId = (UUID) Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
            ReqStatus status = (ReqStatus) Serializers.lookupSerializer(ReqStatus.class).fromBinary(buf, hint);
            int overlayId = buf.readInt();
            int pieceId = buf.readInt();
            if (status.equals(ReqStatus.SUCCESS)) {
                int size = buf.readInt();
                byte[] piece = new byte[size];
                buf.readBytes(piece);
                return new Download.DataResponse(mId, status, overlayId, pieceId, piece);
            } else {
                //nothing
                return new Download.DataResponse(mId, status, overlayId, pieceId, null);
            }
        }
    }

    public static final class HashRequest implements Serializer {

        private final int id;

        public HashRequest(int id) {
            this.id = id;
        }

        @Override
        public int identifier() {
            return id;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            Download.HashRequest obj = (Download.HashRequest) o;
            Serializers.lookupSerializer(UUID.class).toBinary(obj.id, buf);

            buf.writeInt(obj.targetPos);
            buf.writeInt(obj.hashes.size());
            for (Integer hash : obj.hashes) {
                buf.writeInt(hash);
            }
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            UUID mId = (UUID) Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);

            int targetPos = buf.readInt();
            int nrHashes = buf.readInt();
            Set<Integer> hashes = new HashSet<Integer>();
            for (int i = 0; i < nrHashes; i++) {
                hashes.add(buf.readInt());
            }
            return new Download.HashRequest(mId, targetPos, hashes);
        }

    }

    public static final class HashResponse implements Serializer {

        private final int id;

        public HashResponse(int id) {
            this.id = id;
        }

        @Override
        public int identifier() {
            return id;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            Download.HashResponse obj = (Download.HashResponse) o;
            Serializers.lookupSerializer(UUID.class).toBinary(obj.id, buf);
            Serializers.lookupSerializer(ReqStatus.class).toBinary(obj.status, buf);

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
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            UUID mId = (UUID) Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
            ReqStatus status = (ReqStatus) Serializers.lookupSerializer(ReqStatus.class).fromBinary(buf, hint);

            int targetPos = buf.readInt();
            int nrHashes = buf.readInt();
            Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
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

            return new Download.HashResponse(mId, status, targetPos, hashes, missingHashes);
        }
    }
}
