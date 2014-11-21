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

package se.sics.gvod.network.serializers.net.vod;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import se.sics.gvod.common.msg.vod.Download;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.netmsg.HeaderField;
import se.sics.gvod.network.netmsg.vod.NetDownload;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.net.base.NetMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetDownloadSerializer {
    public static class HashRequest extends NetMsgSerializer.AbsRequest<NetDownload.HashRequest> {
        
        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, NetDownload.HashRequest obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsRequest(context, buf, obj);
            context.getSerializer(Download.HashRequest.class).encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public NetDownload.HashRequest decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absReq = decodeAbsRequest(context, buf);
            Download.HashRequest content = context.getSerializer(Download.HashRequest.class).decode(context, buf);
            return new NetDownload.HashRequest(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absReq.getValue0(), absReq.getValue1(), content);
        }

        @Override
        public int getSize(SerializationContext context, NetDownload.HashRequest obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsRequest(context, obj);
            size += context.getSerializer(Download.HashRequest.class).getSize(context, obj.content);
            return size;
        }

    }
    
    public static class HashResponse extends NetMsgSerializer.AbsResponse<NetDownload.HashResponse> {
        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, NetDownload.HashResponse obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsResponse(context, buf, obj);
            context.getSerializer(Download.HashResponse.class).encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public NetDownload.HashResponse decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absResp = decodeAbsResponse(context, buf);
            Download.HashResponse content = context.getSerializer(Download.HashResponse.class).decode(context, buf);
            return new NetDownload.HashResponse(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absResp.getValue0(), absResp.getValue1(), content);
        }

        @Override
        public int getSize(SerializationContext context, NetDownload.HashResponse obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsResponse(context, obj);
            size += context.getSerializer(Download.HashResponse.class).getSize(context, obj.content);
            return size;
        }

    }
    
    public static class DataRequest extends NetMsgSerializer.AbsRequest<NetDownload.DataRequest>  {
        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, NetDownload.DataRequest obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsRequest(context, buf, obj);
            context.getSerializer(Download.DataRequest.class).encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public NetDownload.DataRequest decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absReq = decodeAbsRequest(context, buf);
            Download.DataRequest content = context.getSerializer(Download.DataRequest.class).decode(context, buf);
            return new NetDownload.DataRequest(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absReq.getValue0(), absReq.getValue1(), content);
        }

        @Override
        public int getSize(SerializationContext context, NetDownload.DataRequest obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsRequest(context, obj);
            size += context.getSerializer(Download.DataRequest.class).getSize(context, obj.content);
            return size;
        }

    }
    
    public static class DataResponse extends NetMsgSerializer.AbsResponse<NetDownload.DataResponse> {
        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, NetDownload.DataResponse obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsResponse(context, buf, obj);
            context.getSerializer(Download.DataResponse.class).encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public NetDownload.DataResponse decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absResp = decodeAbsResponse(context, buf);
            Download.DataResponse content = context.getSerializer(Download.DataResponse.class).decode(context, buf);
            return new NetDownload.DataResponse(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absResp.getValue0(), absResp.getValue1(), content);
        }

        @Override
        public int getSize(SerializationContext context, NetDownload.DataResponse obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsResponse(context, obj);
            size += context.getSerializer(Download.DataResponse.class).getSize(context, obj.content);
            return size;
        }

    }
}
