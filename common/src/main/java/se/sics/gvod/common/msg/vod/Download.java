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

import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Download {
    public static class Request extends GvodMsg.Request {
        public final int overlayId;
        public final int pieceId;
        
        public Request(UUID id, int overlayId, int pieceId) {
            super(id);
            this.overlayId = overlayId;
            this.pieceId = pieceId;
        }
        
        @Override
        public Request copy() {
            return new Request(id, overlayId, pieceId);
        }
        
        @Override
        public String toString() {
            return "Download.Request<" + id + ">";
        }
        
        public Response success(byte[] piece) {
            return new Response(id, ReqStatus.SUCCESS, overlayId, pieceId, piece);
        }
        
        public Response missingPiece() {
            return new Response(id, ReqStatus.FAIL, overlayId, pieceId, null);
        }
        
    }        
    
    public static class Response extends GvodMsg.Response {
        
        public final int overlayId;
        public final int pieceId;
        public final byte[] piece;
        
        public Response(UUID id, ReqStatus status, int overlayId, int pieceId, byte[] piece) {
            super(id, status);
            this.overlayId = overlayId;
            this.pieceId = pieceId;
            this.piece = piece;
        }
        
        @Override
        public Response copy() {
            return new Response(id, status, overlayId, pieceId, piece);
        }
        
        @Override
        public String toString() {
            return "Download.Response<" + id + ">";
        }
        
    }
    
    public static class ReqTimeout extends Timeout {
        public final int pieceId;
        
        public ReqTimeout(ScheduleTimeout schedule, int pieceId) {
            super(schedule);
            this.pieceId = pieceId;
        }
        
        @Override
        public String toString() {
            return "Download.Timeout<" + getTimeoutId() + ">";
        }
    }
}
