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
package se.sics.gvod.croupier.msg.intern;

import se.sics.gvod.croupier.CroupierMsg;
import java.util.List;
import java.util.UUID;
import se.sics.gvod.croupier.pub.util.PeerPublicView;

/**
 * immutable
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Shuffle {

    public static class Request extends CroupierMsg.Request {

        public final List<PeerPublicView> publicNodes;
        public final List<PeerPublicView> privateNodes;
        public final PeerPublicView self;

        public Request(UUID id, int overlayId, List<PeerPublicView> publicNodes, List<PeerPublicView> privateNodes, PeerPublicView self) {
            super(id, overlayId);
            this.publicNodes = publicNodes;
            this.privateNodes = privateNodes;
            this.self = self;
        }

        @Override
        public Request copy() {
            return new Request(id, overlayId, publicNodes, privateNodes, self);
        }

        @Override
        public String toString() {
            return "ShuffleRequest<" + overlayId + "> " + id;
        }
    }

    public static class Response extends CroupierMsg.Response {

        public final List<PeerPublicView> publicNodes;
        public final List<PeerPublicView> privateNodes;
        public final PeerPublicView self;

        public Response(UUID id, int overlayId, List<PeerPublicView> publicNodes, List<PeerPublicView> privateNodes, PeerPublicView self) {
            super(id, overlayId);
            this.publicNodes = publicNodes;
            this.privateNodes = privateNodes;
            this.self = self;
        }

        @Override
        public Response copy() {
            return new Response(id, overlayId, publicNodes, privateNodes, self);
        }

        @Override
        public String toString() {
            return "ShuffleResponse<" + overlayId + ">" + id;
        }
    }
}
