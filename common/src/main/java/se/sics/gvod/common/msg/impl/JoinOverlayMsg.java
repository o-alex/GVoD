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
package se.sics.gvod.common.msg.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.util.BuilderException;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.net.VodAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class JoinOverlayMsg {

    public static class Request extends GvodMsg.Request {

        public final int overlayId;

        public Request(UUID reqId, int overlayId) {
            super(reqId);
            this.overlayId = overlayId;
        }
        
        public ResponseBuilder getResponseBuilder() {
            return new ResponseBuilder(reqId, overlayId);
        }

        public Response success(Set<VodAddress> overlaySample, FileMetadata fileMeta) {
            return new Response(reqId, ReqStatus.SUCCESS, overlayId, overlaySample, fileMeta);
        }

        public Response fail() {
            return new Response(reqId, ReqStatus.FAIL, overlayId, null, null);
        }

        @Override
        public Request copy() {
            return new Request(reqId, overlayId);
        }

        @Override
        public String toString() {
            return "JoinOverlayRequest " + reqId.toString();
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + this.overlayId;
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
            return true;
        }
    }

    public static class Response extends GvodMsg.Response {

        public final int overlayId;
        public final Set<VodAddress> overlaySample;
        public final FileMetadata fileMeta;

        public Response(UUID reqId, ReqStatus status, int overlayId, Set<VodAddress> overlaySample, FileMetadata fileMeta) {
            super(reqId, status);
            this.overlayId = overlayId;
            this.overlaySample = overlaySample;
            this.fileMeta = fileMeta;
        }

        @Override
        public Response copy() {
            return new Response(reqId, status, overlayId, new HashSet<VodAddress>(overlaySample), fileMeta);
        }

        @Override
        public String toString() {
            return "JoinOverlayMsgResponse<" + status.toString() + "> " + reqId.toString();
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 71 * hash + this.overlayId;
            hash = 71 * hash + (this.overlaySample != null ? this.overlaySample.hashCode() : 0);
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
            if (this.overlaySample != other.overlaySample && (this.overlaySample == null || !this.overlaySample.equals(other.overlaySample))) {
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
        private Set<VodAddress> overlaySample = null;
        private FileMetadata fileMetadata = null;
        
        public ResponseBuilder(UUID reqId, int overlayId) {
            this.reqId = reqId;
            this.overlayId = overlayId;
        }
        public void setOverlaySample(Set<VodAddress> overlaySample) {
            this.overlaySample = overlaySample;
        }
        
        public void setFileMetadata(FileMetadata fileMetadata) {
            this.fileMetadata = fileMetadata;
        }
        
        public Response finalise(ReqStatus status) throws BuilderException.Missing {
            if(status == ReqStatus.FAIL) {
                return new Response(reqId, status, overlayId, null, null);
            }
            if(overlaySample == null || fileMetadata == null) {
                throw new BuilderException.Missing();
            }
            return new Response(reqId, status, overlayId, overlaySample, fileMetadata);
        }
    }
}
