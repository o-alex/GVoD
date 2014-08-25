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

import java.util.Objects;
import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.ReqStatus;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AddOverlayMsg {

    public static class Request extends GvodMsg.Request {

        public final int overlayId;

        public Request(UUID reqId, int overlayId) {
            super(reqId);
            this.overlayId = overlayId;
        }
        
        public Response fail() {
            return new Response(reqId, ReqStatus.FAIL);
        }
        
        public Response success() {
            return new Response(reqId, ReqStatus.SUCCESS);
        }
        
        @Override
        public String toString() {
            return "AddOverlayRequest " + reqId.toString();
        }
        
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 61 * hash + Objects.hashCode(this.reqId);
            hash = 61 * hash + this.overlayId;
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
            if (!Objects.equals(this.reqId, other.reqId)) {
                return false;
            }
            if (this.overlayId != other.overlayId) {
                return false;
            }
            return true;
        }
    }
    
    public static class Response extends GvodMsg.Response {
        
        public Response(UUID reqId, ReqStatus status) {
            super(reqId, status);
        }
     
        @Override
        public String toString() {
            return "AddOverlayResponse<" + status.toString() + "> "+ reqId.toString();
        }
        
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + Objects.hashCode(this.reqId);
            hash = 23 * hash + Objects.hashCode(this.status);
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
            if (!Objects.equals(this.reqId, other.reqId)) {
                return false;
            }
            if (this.status != other.status) {
                return false;
            }
            return true;
        }
    }
}