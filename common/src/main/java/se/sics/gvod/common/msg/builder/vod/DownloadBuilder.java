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

package se.sics.gvod.common.msg.builder.vod;

import se.sics.gvod.common.msg.builder.GVoDMsgBuilder;
import se.sics.gvod.common.msg.vod.Download;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DownloadBuilder {
    public static class Request extends GVoDMsgBuilder.Request {
        protected Integer overlayId;
        protected Integer pieceId;
        
        public void setOverlayId(int overlayId) {
            this.overlayId = overlayId;
        } 
        
        public void setPieceId(int pieceId) {
            this.pieceId = pieceId;
        }
        
        @Override
        public boolean checkComplete() {
            if(!super.checkComplete()) {
                return false;
            }
            return overlayId != null && pieceId != null;
        }
 
        @Override
        public Download.DataRequest finalise() throws GVoDMsgBuilder.IncompleteException {
            if(!checkComplete()) {
                throw new GVoDMsgBuilder.IncompleteException();
            }
            return new Download.DataRequest(id, overlayId, pieceId);
        }
        
    }
    
    public static class Response extends GVoDMsgBuilder.Response {
        protected Integer overlayId;
        protected Integer pieceId;
        protected byte[] piece;
        
        
        public void setOverlayId(int overlayId) {
            this.overlayId = overlayId;
        } 
        
        public void setPieceId(int pieceId) {
            this.pieceId = pieceId;
        }
        
        public void setPiece(byte[] piece) {
            this.piece = piece;
        }
        
        @Override
        public boolean checkComplete() {
            if(!super.checkComplete()) {
                return false;
            }
            return overlayId != null && pieceId != null && piece != null;
        }
        
        @Override 
        public Download.DataResponse finalise() throws GVoDMsgBuilder.IncompleteException {
            if(!checkComplete()) {
                throw new GVoDMsgBuilder.IncompleteException();
            }
            return new Download.DataResponse(id, status, overlayId, pieceId, piece);
        }
    }
}
