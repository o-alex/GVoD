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

import java.util.Map;
import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class OverlaySample {

    public static class Request extends GvodMsg.Request {

        public final int overlayId;

        public Request(UUID id, int overlayId) {
            super(id);
            this.overlayId = overlayId;
        }
        
        public Response success(Map<DecoratedAddress, Integer> overlaySample) {
            return new Response(id, ReqStatus.SUCCESS, overlayId, overlaySample);
        }
        
        public Response fail() {
            return new Response(id, ReqStatus.FAIL, overlayId, null);
        }
        
        @Override
        public Request copy() {
            return new Request(id, overlayId);
        }

        @Override
        public String toString() {
            return "OverlaySample.Request<" + id + "> overlay:" + overlayId;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + (this.id != null ? this.id.hashCode() : 0);
            hash = 59 * hash + this.overlayId;
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
            if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
                return false;
            }
            if (this.overlayId != other.overlayId) {
                return false;
            }
            return true;
        }
    }

    public static class Response extends GvodMsg.Response {

        public final int overlayId;
        public final Map<DecoratedAddress, Integer> overlaySample; //<peer, utility>
        
        public Response(UUID id, ReqStatus status, int overlayId, Map<DecoratedAddress, Integer> overlaySample) {
            super(id, status);
            this.overlayId = overlayId;
            this.overlaySample = overlaySample;
        }

        @Override
        public Response copy() {
            return new Response(id, status, overlayId, overlaySample);
        }
        
        @Override
        public String toString() {
            return "OverlaySample.Response<" + id + "> overlay:" + overlayId;
        }
        
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + (this.id != null ? this.id.hashCode() : 0);
            hash = 37 * hash + this.overlayId;
            hash = 37 * hash + (this.overlaySample != null ? this.overlaySample.hashCode() : 0);
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
            if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
                return false;
            }
            if (this.overlayId != other.overlayId) {
                return false;
            }
            if (this.overlaySample != other.overlaySample && (this.overlaySample == null || !this.overlaySample.equals(other.overlaySample))) {
                return false;
            }
            return true;
        }
    }
}