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
package se.sics.gvod.bootstrap.client.msg;

import java.util.Set;
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
        
        @Override
        public String toString() {
            return "AddOverlayMsg Request " + reqId.toString();
        }
        
        public Response getResponse(ReqStatus status) {
            return new Response(reqId, status, overlayId);
        }
    }
    
    public static class Response extends GvodMsg.Response {
        public final int overlayId;
        
        public Response(UUID reqId, ReqStatus status, int overlayId) {
            super(reqId, status);
            this.overlayId = overlayId;
        }
     
        @Override
        public String toString() {
            return "AddOverlayMsg Response " + reqId.toString() + " " + status.toString();
        }
    }
}