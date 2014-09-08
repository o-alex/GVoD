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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.JoinOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.GVoDAdapterFactory;
import se.sics.gvod.common.network.NetUtil;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class JoinOverlayAdapter {

    public static class Request implements LocalNettyAdapter<JoinOverlay.Request> {

        @Override
        public JoinOverlay.Request decode(ByteBuf buffer) {
            UUID reqId = NetUtil.decodeUUID(buffer);

            int overlayId = buffer.readInt();
            int utility = buffer.readInt();
            
            return new JoinOverlay.Request(reqId, overlayId, utility);
        }

        @Override
        public ByteBuf encode(JoinOverlay.Request req, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.JOIN_OVERLAY_REQUEST);

            NetUtil.encodeUUID(buffer, req.id);
            buffer.writeInt(req.overlayId);
            buffer.writeInt(req.utility);

            return buffer;
        }

        @Override
        public int getEncodedSize(JoinOverlay.Request req) {
            int size = 0;
            size += 1; //type
            size += NetUtil.getUUIDEncodedSize();
            size += 4; //overlayId
            size += 4; //utility
            return size;
        }
    }

    public static class Response implements LocalNettyAdapter<JoinOverlay.Response> {

        @Override
        public JoinOverlay.Response decode(ByteBuf buffer) {
            UUID respId = NetUtil.decodeUUID(buffer);
            ReqStatus status = NetUtil.decodeReqStatus(buffer);
            int overlayId = buffer.readInt();

            Map<VodAddress, Integer> overlaySample = null;
            FileMetadata fileMeta = null;
            if (status == ReqStatus.SUCCESS) {
                int sampleSize = buffer.readInt();
                overlaySample = new HashMap<VodAddress, Integer>();
                for (int j = 0; j < sampleSize; j++) {
                    VodAddress peer = NetUtil.decodeVodAddress(buffer);
                    int utility = buffer.readInt();
                    overlaySample.put(peer, utility);
                }
                fileMeta = NetUtil.decodeFileMeta(buffer);
            }

            return new JoinOverlay.Response(respId, status, overlayId, overlaySample, fileMeta);
        }

        @Override
        public ByteBuf encode(JoinOverlay.Response resp, ByteBuf buffer
        ) {
            buffer.writeByte(GVoDAdapterFactory.JOIN_OVERLAY_RESPONSE);

            NetUtil.encodeUUID(buffer, resp.id);
            NetUtil.encodeReqStatus(buffer, resp.status);
            buffer.writeInt(resp.overlayId);

            if (resp.status == ReqStatus.SUCCESS) {
                buffer.writeInt(resp.overlaySample.size());
                for (Map.Entry<VodAddress, Integer> e : resp.overlaySample.entrySet()) {
                    NetUtil.encodeVodAddress(buffer, e.getKey());
                    buffer.writeInt(e.getValue());
                }
                NetUtil.encodeFileMeta(buffer, resp.fileMeta);
            }
            return buffer;
        }

        @Override
        public int getEncodedSize(JoinOverlay.Response resp) {
            int size = 0;
            size += 1; //type
            size += NetUtil.getUUIDEncodedSize();
            size += NetUtil.getReqStatusEncodedSize();
            size += 4; //overlayId
            if (resp.status == ReqStatus.SUCCESS) {
                size += 4; //overlaySampleSize;
                for (Map.Entry<VodAddress, Integer> e : resp.overlaySample.entrySet()) {
                    size += NetUtil.getVodAddressEncodedSize(e.getKey());
                    size += 4; //utility
                }
                size += NetUtil.getFileMetaEncodedSize();
            }
            return size;
        }
    }
}
