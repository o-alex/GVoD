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
package se.sics.gvod.network.serializers.bootstrap;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.JoinOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.base.GvodMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class JoinOverlaySerializer {

    public static class Request extends GvodMsgSerializer.AbsRequest<JoinOverlay.Request> {

        @Override
        public JoinOverlay.Request decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            int overlayId = buf.readInt();
            int utility = buf.readInt();
            return new JoinOverlay.Request((UUID) shellObj.get(ID_F), overlayId, utility);
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, JoinOverlay.Request obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            buf.writeInt(obj.overlayId);
            buf.writeInt(obj.utility);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, JoinOverlay.Request obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += Integer.SIZE / 8; //overlayId
            size += Integer.SIZE / 8; //utility
            return size;
        }
    }

    public static class Response extends GvodMsgSerializer.AbsResponse<JoinOverlay.Response> {

        @Override
        public JoinOverlay.Response decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Map<String, Object> shellObj = new HashMap<String, Object>();
            super.decodeParent(context, buf, shellObj);
            int overlayId = buf.readInt();
            FileMetadata fileMeta = null;
            if (shellObj.get(STATUS_F).equals(ReqStatus.SUCCESS)) {
                fileMeta = context.getSerializer(FileMetadata.class).decode(context, buf);
            }
            return new JoinOverlay.Response((UUID) shellObj.get(ID_F), (ReqStatus) shellObj.get(STATUS_F), overlayId, fileMeta);
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, JoinOverlay.Response obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            buf.writeInt(obj.overlayId);
            if (obj.status.equals(ReqStatus.SUCCESS)) {
                context.getSerializer(FileMetadata.class).encode(context, buf, obj.fileMeta);
            }
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, JoinOverlay.Response obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += Integer.SIZE / 8; //overlayId
            if (obj.status.equals(ReqStatus.SUCCESS)) {
                size += context.getSerializer(FileMetadata.class).getSize(context, obj.fileMeta);
            }
            return size;
        }
    }
}
