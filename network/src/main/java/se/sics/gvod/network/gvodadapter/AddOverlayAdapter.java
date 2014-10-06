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
package se.sics.gvod.network.gvodadapter;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.AddOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.network.GVoDAdapterFactory;
import se.sics.gvod.network.Util;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AddOverlayAdapter {

    public static class Request implements GVoDAdapter<AddOverlay.Request> {

        @Override
        public AddOverlay.Request decode(ByteBuf buffer) {
            UUID reqId = Util.decodeUUID(buffer);
            int overlayId = buffer.readInt();
            FileMetadata fileMeta = Util.decodeFileMeta(buffer);

            return new AddOverlay.Request(reqId, overlayId, fileMeta);
        }

        @Override
        public ByteBuf encode(AddOverlay.Request req, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.ADD_OVERLAY_REQUEST);

            Util.encodeUUID(buffer, req.id);
            buffer.writeInt(req.overlayId);
            Util.encodeFileMeta(buffer, req.fileMeta);
            
            return buffer;
        }
        
        @Override
        public int getEncodedSize(AddOverlay.Request req) {
            int size = 0;
            size += 1; //type
            size += Util.getUUIDEncodedSize();
            size += 4; //overlayId;
            size += Util.getFileMetaEncodedSize();
            return size;
        }

    }

    public static class Response implements GVoDAdapter<AddOverlay.Response> {

        @Override
        public AddOverlay.Response decode(ByteBuf buffer) {
            UUID respId = Util.decodeUUID(buffer);
            ReqStatus status = Util.decodeReqStatus(buffer);
            int overlayId = buffer.readInt();
            
            return new AddOverlay.Response(respId, status, overlayId);
        }

        @Override
        public ByteBuf encode(AddOverlay.Response resp, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.ADD_OVERLAY_RESPONSE);

            Util.encodeUUID(buffer, resp.id);
            Util.encodeReqStatus(buffer, resp.status);
            buffer.writeInt(resp.overlayId);

            return buffer;
        }

        @Override
        public int getEncodedSize(AddOverlay.Response resp) {
            int size = 0;
            size += 1; //type
            size += Util.getUUIDEncodedSize();
            size += Util.getReqStatusEncodedSize();
            size += 4; //overlayId
            return size;
        }
    }
}
