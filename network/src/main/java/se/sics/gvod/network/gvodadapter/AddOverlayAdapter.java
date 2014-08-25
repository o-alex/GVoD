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
import se.sics.gvod.common.msg.impl.AddOverlayMsg;
import se.sics.gvod.network.GVoDAdapterFactory;
import se.sics.gvod.network.Util;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AddOverlayAdapter {

    public static class Request implements GVoDAdapter<AddOverlayMsg.Request> {

        @Override
        public AddOverlayMsg.Request decode(ByteBuf buffer) {
            UUID reqId = Util.decodeUUID(buffer);
            int overlayId = buffer.readInt();

            return new AddOverlayMsg.Request(reqId, overlayId);
        }

        @Override
        public ByteBuf encode(AddOverlayMsg.Request req, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.ADD_OVERLAY_REQUEST);

            Util.encodeUUID(buffer, req.reqId);
            buffer.writeInt(req.overlayId);

            return buffer;
        }

    }

    public static class Response implements GVoDAdapter<AddOverlayMsg.Response> {

        @Override
        public AddOverlayMsg.Response decode(ByteBuf buffer) {
            UUID respId = Util.decodeUUID(buffer);
            ReqStatus status = Util.decodeReqStatus(buffer);

            return new AddOverlayMsg.Response(respId, status);
        }

        @Override
        public ByteBuf encode(AddOverlayMsg.Response resp, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.ADD_OVERLAY_RESPONSE);

            Util.encodeUUID(buffer, resp.reqId);
            Util.encodeReqStatus(buffer, resp.status);

            return buffer;
        }
    }
}
