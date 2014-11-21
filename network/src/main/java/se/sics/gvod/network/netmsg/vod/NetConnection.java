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
package se.sics.gvod.network.netmsg.vod;

import java.util.Map;
import java.util.UUID;
import se.sics.gvod.common.msg.vod.Connection;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.network.netmsg.HeaderField;
import se.sics.gvod.network.netmsg.NetContentMsg;
import se.sics.gvod.network.netmsg.OverlayHeaderField;
import se.sics.gvod.network.netmsg.OverlayMsgI;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetConnection {

    public static class Request extends NetContentMsg.Request<Connection.Request> implements OverlayMsgI {
        public Request(VodAddress vodSrc, VodAddress vodDest, UUID id, int overlayId, Connection.Request content) {
            super(vodSrc, vodDest, id, content);
            header.put("overlay", new OverlayHeaderField(overlayId));
        }

        public Request(VodAddress vodSrc, VodAddress vodDest, UUID id, Map<String, HeaderField> header, Connection.Request content) {
            super(vodSrc, vodDest, id, header, content);
        }

        @Override
        public RewriteableMsg copy() {
            return new Request(vodSrc, vodDest, id, header, content);
        }

        public Response getResponse(Connection.Response content) {
            return new Response(vodSrc, vodDest, id, header, content);
        }

        @Override
        public int getOverlay() {
            return ((OverlayHeaderField) header.get("overlay")).overlayId;
        }
    }

    public static class Response extends NetContentMsg.Response<Connection.Response> implements OverlayMsgI {
        public Response(VodAddress vodSrc, VodAddress vodDest, UUID id, int overlayId, Connection.Response content) {
            super(vodSrc, vodDest, id, content);
            header.put("overlay", new OverlayHeaderField(overlayId));
            
        }
        
        public Response(VodAddress vodSrc, VodAddress vodDest, UUID id, Map<String, HeaderField> header, Connection.Response content) {
            super(vodSrc, vodDest, id, header, content);
        }

        @Override
        public RewriteableMsg copy() {
            return new Response(vodSrc, vodDest, id, header, content);
        }

        @Override
        public int getOverlay() {
            return ((OverlayHeaderField) header.get("overlay")).overlayId;
        }
    }

    public static class Update extends NetContentMsg.OneWay<Connection.Update> implements OverlayMsgI {
        public Update(VodAddress vodSrc, VodAddress vodDest, UUID id, int overlayId, Connection.Update content) {
            super(vodSrc, vodDest, id, content);
            header.put("overlay", new OverlayHeaderField(overlayId));
        }

        public Update(VodAddress vodSrc, VodAddress vodDest, UUID id, Map<String, HeaderField> header, Connection.Update content) {
            super(vodSrc, vodDest, id, header, content);
        }
        
        @Override
        public RewriteableMsg copy() {
            return new Update(vodSrc, vodDest, id, header, content);
        }

        @Override
        public int getOverlay() {
            return ((OverlayHeaderField) header.get("overlay")).overlayId;
        }
    }

    public static class Close extends NetContentMsg.OneWay<Connection.Close> implements OverlayMsgI {
        public Close(VodAddress vodSrc, VodAddress vodDest, UUID id, int overlayId, Connection.Close content) {
            super(vodSrc, vodDest, id, content);
            header.put("overlay", new OverlayHeaderField(overlayId));
        }
        
        public Close(VodAddress vodSrc, VodAddress vodDest, UUID id, Map<String, HeaderField> header, Connection.Close content) {
            super(vodSrc, vodDest, id, header, content);
        }

        @Override
        public RewriteableMsg copy() {
            return new Close(vodSrc, vodDest, id, header, content);
        }

        @Override
        public int getOverlay() {
            return ((OverlayHeaderField) header.get("overlay")).overlayId;
        }
    }
}
