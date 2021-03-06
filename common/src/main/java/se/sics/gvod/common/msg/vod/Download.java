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

package se.sics.gvod.common.msg.vod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.ReqStatus;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Download {
    public static class DataRequest extends GvodMsg.Request {
        public final int overlayId;
        public final int pieceId;
        
        public DataRequest(UUID id, int overlayId, int pieceId) {
            super(id);
            this.overlayId = overlayId;
            this.pieceId = pieceId;
        }
        
        @Override
        public DataRequest copy() {
            return new DataRequest(id, overlayId, pieceId);
        }
        
        @Override
        public String toString() {
            return "Download.DataRequest<" + id + ">";
        }
        
        public DataResponse success(byte[] piece) {
            return new DataResponse(id, ReqStatus.SUCCESS, overlayId, pieceId, piece);
        }
        
        public DataResponse missingPiece() {
            return new DataResponse(id, ReqStatus.MISSING, overlayId, pieceId, null);
        }
        
        public DataResponse timeout() {
            return new DataResponse(id, ReqStatus.TIMEOUT, overlayId, pieceId, null);
        }
        
        public DataResponse busy() {
             return new DataResponse(id, ReqStatus.BUSY, overlayId, pieceId, null);
        } 
    }        
    
    public static class DataResponse extends GvodMsg.Response {
        
        public final int overlayId;
        public final int pieceId;
        public final byte[] piece;
        
        public DataResponse(UUID id, ReqStatus status, int overlayId, int pieceId, byte[] piece) {
            super(id, status);
            this.overlayId = overlayId;
            this.pieceId = pieceId;
            this.piece = piece;
        }
        
        @Override
        public DataResponse copy() {
            return new DataResponse(id, status, overlayId, pieceId, piece);
        }
        
        @Override
        public String toString() {
            return "Download.DataResponse<" + id + ">";
        }
        
    }
    
    public static class HashRequest extends GvodMsg.Request {
        public final int targetPos;
        public final Set<Integer> hashes;
        
        public HashRequest(UUID id, int targetPos, Set<Integer> hashes) {
            super(id);
            this.targetPos = targetPos;
            this.hashes = hashes;
        }
        
        @Override
        public HashRequest copy() {
            return new HashRequest(id, targetPos, hashes);
        }
        
        @Override
        public String toString() {
            return "Download.HashRequest<" + id + ">";
        }
        
        public HashResponse success(Map<Integer, byte[]> pieces, Set<Integer> missingPieces) {
            return new HashResponse(id, ReqStatus.SUCCESS, targetPos, pieces, missingPieces);
        }
        
        public HashResponse timeout() {
            return new HashResponse(id, ReqStatus.TIMEOUT, targetPos, new HashMap<Integer, byte[]>(), hashes);
        }
        
        public HashResponse busy() {
            return new HashResponse(id, ReqStatus.BUSY, targetPos, new HashMap<Integer, byte[]>(), hashes);
        }
    }        
    
    public static class HashResponse extends GvodMsg.Response {
        
        public final int targetPos;
        public final Map<Integer, byte[]> hashes;
        public final Set<Integer> missingHashes;
        
        public HashResponse(UUID id, ReqStatus status, int targetPos, Map<Integer, byte[]> hashes, Set<Integer> missingHashes) {
            super(id, status);
            this.targetPos = targetPos;
            this.hashes = hashes;
            this.missingHashes = missingHashes;
        }
        
        @Override
        public HashResponse copy() {
            return new HashResponse(id, status, targetPos, hashes, missingHashes);
        }
        
        @Override
        public String toString() {
            return "Download.HashResponse<" + id + ">";
        }
        
    }
}
