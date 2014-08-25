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
import se.sics.gvod.common.msg.impl.JoinOverlayMsg;
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

            Set<Integer> overlayIds = new HashSet<Integer>();
            int overlayIdSize = buffer.readInt();
            for(int i = 0; i < overlayIdSize; i++) {
                int overlayId = buffer.readInt();
                overlayIds.add(overlayId);
            }
            
            return new JoinOverlayMsg.Request(reqId, overlayIds);
        }

        @Override
        public ByteBuf encode(JoinOverlayMsg.Request req, ByteBuf buffer) {
            buffer.writeByte(GVoDAdapterFactory.ADD_OVERLAY_REQUEST);

            Util.encodeUUID(buffer, req.reqId);
            buffer.writeInt(req.overlayIds.size());
            
            for(int overlayId : req.overlayIds) {
                buffer.writeInt(overlayId);
            }

            return buffer;
        }
        
    }
    
    public static class Response implements GVoDAdapter<JoinOverlayMsg.Response> {

        @Override
        public JoinOverlayMsg.Response decode(ByteBuf buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuf encode(JoinOverlayMsg.Response object, ByteBuf buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}
