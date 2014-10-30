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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.JoinOverlay;
import se.sics.gvod.common.msg.impl.OverlaySample;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.GVoDAdapterFactory;
import se.sics.gvod.network.Util;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class OverlaySampleAdapter {
    public static class Request implements GVoDAdapter<OverlaySample.Request> {

        @Override
        public OverlaySample.Request decode(ByteBuf buffer) {
            UUID reqId = Util.decodeUUID(buffer);

            int overlayId = buffer.readInt();
            
            return new OverlaySample.Request(reqId, overlayId);
        }

        @Override
        public ByteBuf encode(OverlaySample.Request req, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.OVERLAY_SAMPLE_REQUEST);

            Util.encodeUUID(buffer, req.id);
            buffer.writeInt(req.overlayId);

            return buffer;
        }

        @Override
        public int getEncodedSize(OverlaySample.Request req) {
            int size = 0;
            size += 1; //type
            size += Util.getUUIDEncodedSize();
            size += 4; //overlayId
            return size;
        }
    }

    public static class Response implements GVoDAdapter<OverlaySample.Response> {

        @Override
        public OverlaySample.Response decode(ByteBuf buffer) {
            UUID respId = Util.decodeUUID(buffer);
            ReqStatus status = Util.decodeReqStatus(buffer);
            int overlayId = buffer.readInt();

            Map<VodAddress, Integer> overlaySample = null;
            if (status == ReqStatus.SUCCESS) {
                int sampleSize = buffer.readInt();
                overlaySample = new HashMap<VodAddress, Integer>();
                for (int j = 0; j < sampleSize; j++) {
                    VodAddress peer = Util.decodeVodAddress(buffer);
                    int utility = buffer.readInt();
                    overlaySample.put(peer, utility);
                }
            }

            return new OverlaySample.Response(respId, status, overlayId, overlaySample);
        }

        @Override
        public ByteBuf encode(OverlaySample.Response resp, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.OVERLAY_SAMPLE_RESPONSE);

            Util.encodeUUID(buffer, resp.id);
            Util.encodeReqStatus(buffer, resp.status);
            buffer.writeInt(resp.overlayId);

            if (resp.status == ReqStatus.SUCCESS) {
                buffer.writeInt(resp.overlaySample.size());
                for (Map.Entry<VodAddress, Integer> e : resp.overlaySample.entrySet()) {
                    Util.encodeVodAddress(buffer, e.getKey());
                    buffer.writeInt(e.getValue());
                }
            }
            return buffer;
        }

        @Override
        public int getEncodedSize(OverlaySample.Response resp) {
            int size = 0;
            size += 1; //type
            size += Util.getUUIDEncodedSize();
            size += Util.getReqStatusEncodedSize();
            size += 4; //overlayId
            if (resp.status == ReqStatus.SUCCESS) {
                size += 4; //overlaySampleSize;
                for (Map.Entry<VodAddress, Integer> e : resp.overlaySample.entrySet()) {
                    size += Util.getVodAddressEncodedSize(e.getKey());
                    size += 4; //utility
                }
            }
            return size;
        }
    }
}
