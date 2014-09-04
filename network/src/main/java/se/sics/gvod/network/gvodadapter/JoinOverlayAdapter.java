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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.JoinOverlayMsg;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.GVoDAdapterFactory;
import se.sics.gvod.network.Util;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class JoinOverlayAdapter {

    public static class Request implements GVoDAdapter<JoinOverlayMsg.Request> {

        @Override
        public JoinOverlayMsg.Request decode(ByteBuf buffer) {
            UUID reqId = Util.decodeUUID(buffer);

            int overlayId = buffer.readInt();

            return new JoinOverlayMsg.Request(reqId, overlayId);
        }

        @Override
        public ByteBuf encode(JoinOverlayMsg.Request req, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.JOIN_OVERLAY_REQUEST);

            Util.encodeUUID(buffer, req.reqId);
            buffer.writeInt(req.overlayId);

            return buffer;
        }

        @Override
        public int getEncodedSize(JoinOverlayMsg.Request req) {
            int size = 0;
            size += 1; //type
            size += Util.getUUIDEncodedSize();
            size += 4; //overlayId
            return size;
        }
    }

    public static class Response implements GVoDAdapter<JoinOverlayMsg.Response> {

        @Override
        public JoinOverlayMsg.Response decode(ByteBuf buffer) {
            UUID respId = Util.decodeUUID(buffer);
            ReqStatus status = Util.decodeReqStatus(buffer);
            int overlayId = buffer.readInt();

            Set<VodAddress> overlaySample = null;
            FileMetadata fileMeta = null;
            if (status == ReqStatus.SUCCESS) {
                int sampleSize = buffer.readInt();
                overlaySample = new HashSet<VodAddress>();
                for (int j = 0; j < sampleSize; j++) {
                    VodAddress peer = Util.decodeVodAddress(buffer);
                    overlaySample.add(peer);
                }
                fileMeta = Util.decodeFileMeta(buffer);
            }

            return new JoinOverlayMsg.Response(respId, status, overlayId, overlaySample, fileMeta);
        }

        @Override
        public ByteBuf encode(JoinOverlayMsg.Response resp, ByteBuf buffer
        ) {
            buffer.writeByte(GVoDAdapterFactory.JOIN_OVERLAY_RESPONSE);

            Util.encodeUUID(buffer, resp.reqId);
            Util.encodeReqStatus(buffer, resp.status);
            buffer.writeInt(resp.overlayId);

            if (resp.status == ReqStatus.SUCCESS) {
                buffer.writeInt(resp.overlaySample.size());
                for (VodAddress peer : resp.overlaySample) {
                    Util.encodeVodAddress(buffer, peer);
                }
                Util.encodeFileMeta(buffer, resp.fileMeta);
            }
            return buffer;
        }

        @Override
        public int getEncodedSize(JoinOverlayMsg.Response resp) {
            int size = 0;
            size += 1; //type
            size += Util.getUUIDEncodedSize();
            size += Util.getReqStatusEncodedSize();
            size += 4; //overlayId
            if (resp.status == ReqStatus.SUCCESS) {
                size += 4; //overlaySampleSize;
                for (VodAddress peer : resp.overlaySample) {
                    size += Util.getVodAddressEncodedSize(peer);
                }
                size += Util.getFileMetaSize();
            }
            return size;
        }
    }
}
