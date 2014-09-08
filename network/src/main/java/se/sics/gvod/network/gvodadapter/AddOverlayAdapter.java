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

import se.sics.gvod.common.network.LocalNettyAdapter;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.AddOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.network.GVoDAdapterFactory;
import se.sics.gvod.common.network.NetUtil;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AddOverlayAdapter {

    public static class Request implements LocalNettyAdapter<AddOverlay.Request> {

        @Override
        public AddOverlay.Request decode(ByteBuf buffer) {
            UUID reqId = NetUtil.decodeUUID(buffer);
            int overlayId = buffer.readInt();
            FileMetadata fileMeta = NetUtil.decodeFileMeta(buffer);

            return new AddOverlay.Request(reqId, overlayId, fileMeta);
        }

        @Override
        public ByteBuf encode(AddOverlay.Request req, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.ADD_OVERLAY_REQUEST);

            NetUtil.encodeUUID(buffer, req.id);
            buffer.writeInt(req.overlayId);
            NetUtil.encodeFileMeta(buffer, req.fileMeta);
            
            return buffer;
        }
        
        @Override
        public int getEncodedSize(AddOverlay.Request req) {
            int size = 0;
            size += 1; //type
            size += NetUtil.getUUIDEncodedSize();
            size += 4; //overlayId;
            size += NetUtil.getFileMetaEncodedSize();
            return size;
        }

    }

    public static class Response implements LocalNettyAdapter<AddOverlay.Response> {

        @Override
        public AddOverlay.Response decode(ByteBuf buffer) {
            UUID respId = NetUtil.decodeUUID(buffer);
            ReqStatus status = NetUtil.decodeReqStatus(buffer);
            int overlayId = buffer.readInt();
            
            return new AddOverlay.Response(respId, status, overlayId);
        }

        @Override
        public ByteBuf encode(AddOverlay.Response resp, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.ADD_OVERLAY_RESPONSE);

            NetUtil.encodeUUID(buffer, resp.id);
            NetUtil.encodeReqStatus(buffer, resp.status);
            buffer.writeInt(resp.overlayId);

            return buffer;
        }

        @Override
        public int getEncodedSize(AddOverlay.Response resp) {
            int size = 0;
            size += 1; //type
            size += NetUtil.getUUIDEncodedSize();
            size += NetUtil.getReqStatusEncodedSize();
            size += 4; //overlayId
            return size;
        }
    }
}
