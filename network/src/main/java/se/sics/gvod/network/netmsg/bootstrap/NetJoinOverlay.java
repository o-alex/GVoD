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
package se.sics.gvod.network.netmsg.bootstrap;

import java.util.Map;
import java.util.UUID;
import se.sics.gvod.common.msg.peerMngr.JoinOverlay;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.network.netmsg.HeaderField;
import se.sics.gvod.network.netmsg.NetContentMsg;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetJoinOverlay {

    public static class Request extends NetContentMsg.Request<JoinOverlay.Request> {

        public Request(VodAddress vodSrc, VodAddress vodDest, UUID id, JoinOverlay.Request content) {
            super(vodSrc, vodDest, id, content);
        }

        public Request(VodAddress vodSrc, VodAddress vodDest, UUID id, Map<String, HeaderField> header, JoinOverlay.Request content) {
            super(vodSrc, vodDest, id, header, content);
        }

        @Override
        public RewriteableMsg copy() {
            return new Request(vodSrc, vodDest, id, header, content);
        }

        public Response getResponse(JoinOverlay.Response content) {
            return new Response(vodSrc, vodDest, id, header, content);
        }
    }

    public static class Response extends NetContentMsg.Response<JoinOverlay.Response> {

        public Response(VodAddress vodSrc, VodAddress vodDest, UUID id, JoinOverlay.Response content) {
            super(vodSrc, vodDest, id, content);
        }

        public Response(VodAddress vodSrc, VodAddress vodDest, UUID id, Map<String, HeaderField> header, JoinOverlay.Response content) {
            super(vodSrc, vodDest, id, header, content);
        }

        @Override
        public RewriteableMsg copy() {
            return new Response(vodSrc, vodDest, id, header, content);
        }
    }
}
