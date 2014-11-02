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
package se.sics.gvod.common.msg.peerMngr;

import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.util.BuilderException;
import se.sics.gvod.common.util.FileMetadata;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class JoinOverlay {

    public static class Request extends GvodMsg.Request {

        public final int overlayId;
        public final int utility;

        public Request(UUID reqId, int overlayId, int utility) {
            super(reqId);
            this.overlayId = overlayId;
            this.utility = utility;
        }

        public ResponseBuilder getResponseBuilder() {
            return new ResponseBuilder(id, overlayId);
        }

        public Response success(FileMetadata fileMeta) {
            return new Response(id, ReqStatus.SUCCESS, overlayId, fileMeta);
        }

        public Response fail() {
            return new Response(id, ReqStatus.FAIL, overlayId, null);
        }

        @Override
        public Request copy() {
            return new Request(id, overlayId, utility);
        }

        @Override
        public String toString() {
            return "JoinOverlayRequest " + id.toString();
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + this.overlayId;
            hash = 29 * hash + this.utility;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Request other = (Request) obj;
            if (this.overlayId != other.overlayId) {
                return false;
            }
            if (this.utility != other.utility) {
                return false;
            }
            return true;
        }
    }

    public static class Response extends GvodMsg.Response {

        public final int overlayId;
        public final FileMetadata fileMeta;

        public Response(UUID reqId, ReqStatus status, int overlayId, FileMetadata fileMeta) {
            super(reqId, status);
            this.overlayId = overlayId;
            this.fileMeta = fileMeta;
        }

        @Override
        public Response copy() {
            return new Response(id, status, overlayId, fileMeta);
        }

        @Override
        public String toString() {
            return "JoinOverlayMsgResponse<" + status.toString() + "> " + id.toString();
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 71 * hash + this.overlayId;
            hash = 71 * hash + (this.fileMeta != null ? this.fileMeta.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Response other = (Response) obj;
            if (this.overlayId != other.overlayId) {
                return false;
            }
            if (this.fileMeta != other.fileMeta && (this.fileMeta == null || !this.fileMeta.equals(other.fileMeta))) {
                return false;
            }
            return true;
        }
    }

    public static class ResponseBuilder {

        public final UUID reqId;
        public final int overlayId;
        private FileMetadata fileMetadata = null;

        public ResponseBuilder(UUID reqId, int overlayId) {
            this.reqId = reqId;
            this.overlayId = overlayId;
        }


        public void setFileMetadata(FileMetadata fileMetadata) {
            this.fileMetadata = fileMetadata;
        }

        public Response finalise(ReqStatus status) throws BuilderException.Missing {
            if (status == ReqStatus.FAIL) {
                return new Response(reqId, status, overlayId, null);
            }
            if (fileMetadata == null) {
                throw new BuilderException.Missing();
            }
            return new Response(reqId, status, overlayId, fileMetadata);
        }
    }
}
