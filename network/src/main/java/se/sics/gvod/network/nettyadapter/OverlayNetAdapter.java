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

package se.sics.gvod.network.nettyadapter;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msg.GvodMsg;
import se.sics.gvod.common.msgs.DirectMsgNettyFactory;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.network.GVoDAdapterFactory;
import se.sics.gvod.network.pmadapter.GVoDAdapter;
import se.sics.gvod.network.nettymsg.GvodNetMsg;
import se.sics.gvod.network.nettymsg.OverlayNetMsg;
import se.sics.kompics.KompicsEvent;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class OverlayNetAdapter {
    public static class Request extends DirectMsgNettyFactory.Request implements NettyAdapter {

        //**********NettyAdapter
        @Override
        public RewriteableMsg decodeMsg(ByteBuf buffer) throws NettyAdapter.DecodingException {
            try {
                return decode(buffer);
            } catch (MessageDecodingException ex) {
                throw new NettyAdapter.DecodingException(ex);
            }
        }

        @Override
        public ByteBuf encodeMsg(ByteBuf buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        //**********DirectMsgNettyFactory.Request
        @Override
        protected DirectMsg process(ByteBuf buffer) throws MessageDecodingException {
            byte opCode = buffer.readByte();
            int overlay = buffer.readInt();
            GvodMsg.Request payload = (GvodMsg.Request)GVoDAdapterFactory.getAdapter(opCode).decode(buffer);
            return new OverlayNetMsg.Request(vodSrc, vodDest, overlay, payload);
        }
    }

    public static class Response extends DirectMsgNettyFactory.Response implements NettyAdapter {

        //**********NettyAdapter
        @Override
        public RewriteableMsg decodeMsg(ByteBuf buffer) throws NettyAdapter.DecodingException {
            try {
                return decode(buffer);
            } catch (MessageDecodingException ex) {
                throw new NettyAdapter.DecodingException(ex);
            }
        }

        @Override
        public ByteBuf encodeMsg(ByteBuf buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        //**********DirectMsgNettyFactory.Request
        @Override
        protected DirectMsg process(ByteBuf buffer) throws MessageDecodingException {
            byte opCode = buffer.readByte();
            int overlay = buffer.readInt();
            GvodMsg.Response payload = (GvodMsg.Response) GVoDAdapterFactory.getAdapter(opCode).decode(buffer);
            return new OverlayNetMsg.Response(vodSrc, vodDest, overlay, payload);
        }
    }
    
    public static class OneWay extends DirectMsgNettyFactory.Oneway implements NettyAdapter {

        //**********NettyAdapter
        @Override
        public RewriteableMsg decodeMsg(ByteBuf buffer) throws NettyAdapter.DecodingException {
            try {
                return decode(buffer);
            } catch (MessageDecodingException ex) {
                throw new NettyAdapter.DecodingException(ex);
            }
        }

        @Override
        public ByteBuf encodeMsg(ByteBuf buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        //**********DirectMsgNettyFactory.Request
        @Override
        protected DirectMsg process(ByteBuf buffer) throws MessageDecodingException {
            byte opCode = buffer.readByte();
            int overlay = buffer.readInt();
            GvodMsg.OneWay payload = (GvodMsg.OneWay) GVoDAdapterFactory.getAdapter(opCode).decode(buffer);
            return new OverlayNetMsg.OneWay(vodSrc, vodDest, overlay, payload);
        }
    }
}
