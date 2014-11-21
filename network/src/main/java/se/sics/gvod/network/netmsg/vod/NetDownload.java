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
import se.sics.gvod.common.msg.vod.Download;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.network.netmsg.HeaderField;
import se.sics.gvod.network.netmsg.NetContentMsg;
import se.sics.gvod.network.netmsg.OverlayHeaderField;
import se.sics.gvod.network.netmsg.OverlayMsgI;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetDownload {

    public static class HashRequest extends NetContentMsg.Request<Download.HashRequest> implements OverlayMsgI {

        public HashRequest(VodAddress vodSrc, VodAddress vodDest, UUID id, int overlayId, Download.HashRequest content) {
            super(vodSrc, vodDest, id, content);
            header.put("overlay", new OverlayHeaderField(overlayId));
        }

        public HashRequest(VodAddress vodSrc, VodAddress vodDest, UUID id, Map<String, HeaderField> header, Download.HashRequest content) {
            super(vodSrc, vodDest, id, header, content);
        }

        @Override
        public RewriteableMsg copy() {
            return new HashRequest(vodSrc, vodDest, id, header, content);
        }

        public HashResponse getResponse(Download.HashResponse content) {
            return new HashResponse(vodSrc, vodDest, id, header, content);
        }

        @Override
        public int getOverlay() {
            return ((OverlayHeaderField) header.get("overlay")).overlayId;
        }
    }

    public static class HashResponse extends NetContentMsg.Response<Download.HashResponse> implements OverlayMsgI {

        public HashResponse(VodAddress vodSrc, VodAddress vodDest, UUID id, int overlayId, Download.HashResponse content) {
            super(vodSrc, vodDest, id, content);
            header.put("overlay", new OverlayHeaderField(overlayId));
        }

        public HashResponse(VodAddress vodSrc, VodAddress vodDest, UUID id, Map<String, HeaderField> header, Download.HashResponse content) {
            super(vodSrc, vodDest, id, header, content);
        }

        @Override
        public RewriteableMsg copy() {
            return new HashResponse(vodSrc, vodDest, id, header, content);
        }

        @Override
        public int getOverlay() {
            return ((OverlayHeaderField) header.get("overlay")).overlayId;
        }
    }

    public static class DataRequest extends NetContentMsg.Request<Download.DataRequest> implements OverlayMsgI {

        public DataRequest(VodAddress vodSrc, VodAddress vodDest, UUID id, int overlayId, Download.DataRequest content) {
            super(vodSrc, vodDest, id, content);
            header.put("overlay", new OverlayHeaderField(overlayId));
        }

        public DataRequest(VodAddress vodSrc, VodAddress vodDest, UUID id, Map<String, HeaderField> header, Download.DataRequest content) {
            super(vodSrc, vodDest, id, header, content);
        }

        @Override
        public RewriteableMsg copy() {
            return new DataRequest(vodSrc, vodDest, id, header, content);
        }

        public DataResponse getResponse(Download.DataResponse content) {
            return new DataResponse(vodSrc, vodDest, id, header, content);
        }

        @Override
        public int getOverlay() {
            return ((OverlayHeaderField) header.get("overlay")).overlayId;
        }
    }

    public static class DataResponse extends NetContentMsg.Response<Download.DataResponse> implements OverlayMsgI {

        public DataResponse(VodAddress vodSrc, VodAddress vodDest, UUID id, int overlayId, Download.DataResponse content) {
            super(vodSrc, vodDest, id, content);
            header.put("overlay", new OverlayHeaderField(overlayId));
        }

        public DataResponse(VodAddress vodSrc, VodAddress vodDest, UUID id, Map<String, HeaderField> header, Download.DataResponse content) {
            super(vodSrc, vodDest, id, header, content);
        }

        @Override
        public RewriteableMsg copy() {
            return new DataResponse(vodSrc, vodDest, id, header, content);
        }

        @Override
        public int getOverlay() {
            return ((OverlayHeaderField) header.get("overlay")).overlayId;
        }
    }
}
