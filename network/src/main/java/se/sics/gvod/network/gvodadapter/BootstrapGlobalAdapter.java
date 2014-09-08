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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.BootstrapGlobal;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.GVoDAdapterFactory;
import se.sics.gvod.common.network.NetUtil;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapGlobalAdapter {

    public static class Request implements LocalNettyAdapter<BootstrapGlobal.Request> {

        @Override
        public BootstrapGlobal.Request decode(ByteBuf buffer) {
            UUID reqId = NetUtil.decodeUUID(buffer);
            return new BootstrapGlobal.Request(reqId);
        }

        @Override
        public ByteBuf encode(BootstrapGlobal.Request req, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_REQUEST);
            NetUtil.encodeUUID(buffer, req.id);
            return buffer;
        }

        @Override
        public int getEncodedSize(BootstrapGlobal.Request req) {
            int size = 0;
            size += 1; //type
            size += NetUtil.getUUIDEncodedSize();
            return size;
        }
    }

    public static class Response implements LocalNettyAdapter<BootstrapGlobal.Response> {

        @Override
        public BootstrapGlobal.Response decode(ByteBuf buffer) {

            UUID reqId = NetUtil.decodeUUID(buffer);
            ReqStatus status = NetUtil.decodeReqStatus(buffer);

            Set<VodAddress> systemSample = null;
            
            if (status == ReqStatus.SUCCESS) {
                systemSample = new HashSet<VodAddress>();
                int sampleSize = buffer.readInt();
                for (int i = 0; i < sampleSize; i++) {
                    VodAddress address = NetUtil.decodeVodAddress(buffer);
                    systemSample.add(address);
                }
            }
            return new BootstrapGlobal.Response(reqId, status, systemSample);
        }

        @Override
        public ByteBuf encode(BootstrapGlobal.Response resp, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.BOOTSTRAP_GLOBAL_RESPONSE);

            NetUtil.encodeUUID(buffer, resp.id);
            NetUtil.encodeReqStatus(buffer, resp.status);

            if (resp.status == ReqStatus.SUCCESS) {
                buffer.writeInt(resp.systemSample.size());
                for (VodAddress peer : resp.systemSample) {
                    NetUtil.encodeVodAddress(buffer, peer);
                }
            }
            return buffer;
        }

        @Override
        public int getEncodedSize(BootstrapGlobal.Response resp) {
            int size = 0;
            size += 1; //type
            size += NetUtil.getUUIDEncodedSize();
            size += NetUtil.getReqStatusEncodedSize();
            if (resp.status == ReqStatus.SUCCESS) {
                size += 4; //sampleSize
                for (VodAddress peer : resp.systemSample) {
                    size += NetUtil.getVodAddressEncodedSize(peer);
                }
            }
            return size;
        }
    }
}
