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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.net.VodAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class JoinOverlayMsg {

    public static class Request extends GvodMsg.Request {

        public final Set<Integer> overlayIds;

        public Request(UUID reqId, Set<Integer> overlayId) {
            super(reqId);
            this.overlayIds = overlayId;
        }

        public Response success(Map<Integer, Set<VodAddress>> overlaySample) {
            return new Response(reqId, ReqStatus.SUCCESS, overlaySample);
        }

        public Response fail() {
            return new Response(reqId, ReqStatus.FAIL, null);
        }
        
        @Override
        public Request copy() {
            return new Request(reqId, new HashSet<>(overlayIds));
        }
        
        @Override
        public String toString() {
            return "JoinOverlayRequest " + reqId.toString();
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 71 * hash + Objects.hashCode(this.reqId);
            hash = 71 * hash + Objects.hashCode(this.overlayIds);
            return hash;
        }

        @Override
        public boolean equals(Object obj
        ) {
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
            if (!Objects.equals(this.overlayIds, other.overlayIds)) {
                return false;
            }
            return true;
        }
    }

    public static class Response extends GvodMsg.Response {

        public final Map<Integer, Set<VodAddress>> overlaySamples;

        public Response(UUID reqId, ReqStatus status, Map<Integer, Set<VodAddress>> overlaySamples) {
            super(reqId, status);
            this.overlaySamples = overlaySamples;
        }

        @Override
        public Response copy() {
            Map<Integer, Set<VodAddress>> newOverlaySamples = new HashMap<>();
            for(Map.Entry<Integer, Set<VodAddress>> e : overlaySamples.entrySet()) {
                newOverlaySamples.put(e.getKey(), new HashSet<>(e.getValue()));
            }
            return new Response(reqId, status, newOverlaySamples);
        }
        
        @Override
        public String toString() {
            return "JoinOverlayMsgResponse<" + status.toString() + "> " + reqId.toString();
        }
        
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 17 * hash + Objects.hashCode(this.reqId);
            hash = 17 * hash + Objects.hashCode(this.overlaySamples);
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
            if (!Objects.equals(this.overlaySamples, other.overlaySamples)) {
                return false;
            }
            return true;
        }
    }

}