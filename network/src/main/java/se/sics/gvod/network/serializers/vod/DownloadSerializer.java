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
package se.sics.gvod.network.serializers.vod;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msg.builder.GVoDMsgBuilder;
import se.sics.gvod.common.msg.builder.vod.DownloadBuilder;
import se.sics.gvod.common.msg.vod.Download;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.base.GvodMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DownloadSerializer {

    public static abstract class AbsRequest<E extends Download.DataRequest, F extends DownloadBuilder.Request> extends GvodMsgSerializer.AbsRequest<E, F> {

        @Override
        public F decode(SerializationContext context, ByteBuf buf, F shellObj) throws SerializerException, SerializationContext.MissingException {
            super.decode(context, buf, shellObj);
            int overlayId = buf.readInt();
            int pieceId = buf.readInt();
            shellObj.setOverlayId(overlayId);
            shellObj.setPieceId(pieceId);
            return shellObj;
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, E obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            buf.writeInt(obj.overlayId);
            buf.writeInt(obj.pieceId);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, E obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += Integer.SIZE/8; //overlayId
            size += Integer.SIZE/8; //pieceId
            return size;
        }
    }

    public static final class Request extends AbsRequest<Download.DataRequest, DownloadBuilder.Request> {

        @Override
        public Download.DataRequest decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            try {
                return decode(context, buf, new DownloadBuilder.Request()).finalise();
            } catch (GVoDMsgBuilder.IncompleteException ex) {
                throw new SerializerException(ex);
            }
        }

    }

    public static abstract class AbsResponse<E extends Download.DataResponse, F extends DownloadBuilder.Response> extends GvodMsgSerializer.AbsResponse<E, F> {

        @Override
        public F decode(SerializationContext context, ByteBuf buf, F shellObj) throws SerializerException, SerializationContext.MissingException {
            super.decode(context, buf, shellObj);
            int overlayId = buf.readInt();
            int pieceId = buf.readInt();
            int size = buf.readInt();
            byte[] piece = new byte[size];
            buf.readBytes(piece);
            shellObj.setOverlayId(overlayId);
            shellObj.setPieceId(pieceId);
            shellObj.setPiece(piece);
            return shellObj;
        }

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, E obj) throws SerializerException, SerializationContext.MissingException {
            super.encode(context, buf, obj);
            buf.writeInt(obj.overlayId);
            buf.writeInt(obj.pieceId);
            buf.writeInt(obj.piece.length);
            buf.writeBytes(obj.piece);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, E obj) throws SerializerException, SerializationContext.MissingException {
            int size = super.getSize(context, obj);
            size += Integer.SIZE/8; //overlayId
            size += Integer.SIZE/8; //pieceId
            size += Integer.SIZE/8; //size of piece
            size += obj.piece.length * Byte.SIZE/8; //piece
            return size;
        }
    }

    public static final class Response extends AbsResponse<Download.DataResponse, DownloadBuilder.Response> {

        @Override
        public Download.DataResponse decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            try {
                return decode(context, buf, new DownloadBuilder.Response()).finalise();
            } catch (GVoDMsgBuilder.IncompleteException ex) {
                throw new SerializerException(ex);
            }
        }

    }
}
