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
import se.sics.gvod.common.msg.impl.BootstrapGlobalMsg;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.GVoDAdapterFactory;
import se.sics.gvod.network.Util;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapGlobalAdapter {

    public static class Request implements GVoDAdapter<BootstrapGlobalMsg.Request> {

        @Override
        public BootstrapGlobalMsg.Request decode(ByteBuf buffer) {
            UUID reqId = Util.decodeUUID(buffer);
            return new BootstrapGlobalMsg.Request(reqId);
        }

        @Override
        public ByteBuf encode(BootstrapGlobalMsg.Request req, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_REQUEST);
            Util.encodeUUID(buffer, req.reqId);
            return buffer;
        }

        @Override
        public int getEncodedSize(BootstrapGlobalMsg.Request req) {
            int size = 0;
            size += 1; //type
            size += Util.getUUIDEncodedSize();
            return size;
        }
    }

    public static class Response implements GVoDAdapter<BootstrapGlobalMsg.Response> {

        @Override
        public BootstrapGlobalMsg.Response decode(ByteBuf buffer) {

            UUID reqId = Util.decodeUUID(buffer);
            ReqStatus status = Util.decodeReqStatus(buffer);

            Set<VodAddress> systemSample = null;
            
            if (status == ReqStatus.SUCCESS) {
                systemSample = new HashSet<VodAddress>();
                int sampleSize = buffer.readInt();
                for (int i = 0; i < sampleSize; i++) {
                    VodAddress address = Util.decodeVodAddress(buffer);
                    systemSample.add(address);
                }
            }
            return new BootstrapGlobalMsg.Response(reqId, status, systemSample);
        }

        @Override
        public ByteBuf encode(BootstrapGlobalMsg.Response resp, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_RESPONSE);

            Util.encodeUUID(buffer, resp.reqId);
            Util.encodeReqStatus(buffer, resp.status);

            if (resp.status == ReqStatus.SUCCESS) {
                buffer.writeInt(resp.systemSample.size());
                for (VodAddress peer : resp.systemSample) {
                    Util.encodeVodAddress(buffer, peer);
                }
            }
            return buffer;
        }

        @Override
        public int getEncodedSize(BootstrapGlobalMsg.Response resp) {
            int size = 0;
            size += 1; //type
            size += Util.getUUIDEncodedSize();
            size += Util.getReqStatusEncodedSize();
            if (resp.status == ReqStatus.SUCCESS) {
                size += 4; //sampleSize
                for (VodAddress peer : resp.systemSample) {
                    size += Util.getVodAddressEncodedSize(peer);
                }
            }
            return size;
        }
    }
}
