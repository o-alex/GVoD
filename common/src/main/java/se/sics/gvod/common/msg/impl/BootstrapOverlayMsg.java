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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.net.VodAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapOverlayMsg {

    public static class Request extends GvodMsg.Request {

        public final Set<Integer> overlayIds;

        public Request(UUID reqId, Set<Integer> overlayIds) {
            super(reqId);
            this.overlayIds = overlayIds;
        }

        @Override
        public String toString() {
            return "BootstrapOverlayMsg Request " + reqId.toString();
        }

        public Response success(Map<Integer, VodAddress> overlaySample) {
            return new Response(reqId, ReqStatus.SUCCESS, overlaySample);
        }
    }

    public static class Response extends GvodMsg.Response {

        public final Map<Integer, VodAddress> overlaySample;

        private Response(UUID reqId, ReqStatus status, Map<Integer, VodAddress> overlaySample) {
            super(reqId, status);
            this.overlaySample = this.overlaySample;
        }

        @Override
        public String toString() {
            return "BootstrapOverlayMsg Response " + reqId.toString() + " " + status.toString();
        }
    }
}
