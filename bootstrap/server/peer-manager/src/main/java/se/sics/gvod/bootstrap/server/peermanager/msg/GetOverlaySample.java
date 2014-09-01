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
package se.sics.gvod.bootstrap.server.peermanager.msg;

import java.util.Set;
import java.util.UUID;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerMsg;
import se.sics.gvod.common.msg.ReqStatus;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GetOverlaySample {

    public static class Request extends PeerManagerMsg.Request {

        public final int overlayId;

        public Request(UUID id, int overlayId) {
            super(id);
            this.overlayId = overlayId;
        }

        public Response success(Set<byte[]> overlaySample) {
            return new Response(id, ReqStatus.SUCCESS, overlayId, overlaySample);
        }

        public Response fail() {
            return new Response(id, ReqStatus.FAIL, overlayId, null);
        }

        @Override
        public String toString() {
            return "GetOverlaySampleRequest " + id;
        }
    }

    public static class Response extends PeerManagerMsg.Response {

        public final int overlayId;
        public final Set<byte[]> overlaySample;

        public Response(UUID id, ReqStatus status, int overlayId, Set<byte[]> overlaySample) {
            super(id, status);
            this.overlayId = overlayId;
            this.overlaySample = overlaySample;
        }

        @Override
        public String toString() {
            return "GetOverlaySampleResponse<" + status + "> " + id;
        }
    }
}
