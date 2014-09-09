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
package se.sics.gvod.croupier.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import se.sics.gvod.common.network.NetUtil;
import se.sics.gvod.croupier.msg.intern.Shuffle;
import se.sics.gvod.croupier.pub.util.PeerView;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ShuffleAdapter {

    public static class Request implements CroupierAdapter.Request<Shuffle.Request> {

        @Override
        public Shuffle.Request decode(CroupierContext context, ByteBuf buffer) {
            UUID id = NetUtil.decodeUUID(buffer);
            int overlayId = buffer.readInt();

            byte regCode = buffer.readByte();
            PeerView.Adapter adapter = context.getPPViewAdapter(regCode);

            int publicNSize = buffer.readInt();
            List<PeerView> publicNodes = new ArrayList<PeerView>();
            for (int i = 0; i < publicNSize; i++) {
                publicNodes.add(adapter.decode(buffer));
            }

            int privateNSize = buffer.readInt();
            List<PeerView> privateNodes = new ArrayList<PeerView>();
            for (int i = 0; i < privateNSize; i++) {
                privateNodes.add(adapter.decode(buffer));
            }

            PeerView self = adapter.decode(buffer);

            return new Shuffle.Request(id, overlayId, publicNodes, privateNodes, self);
        }

        @Override
        public ByteBuf encode(CroupierContext context, Shuffle.Request req, ByteBuf buffer) {
            NetUtil.encodeUUID(buffer, req.id);
            buffer.writeInt(req.croupierId);

            buffer.writeByte(context.getPPViewReqCode(req.self));
            PeerView.Adapter adapter = context.getPPViewAdapter(req.self);

            buffer.writeInt(req.publicNodes.size());
            for (PeerView ppView : req.publicNodes) {
                adapter.encode(ppView, buffer);
            }

            buffer.writeInt(req.privateNodes.size());
            for (PeerView ppView : req.privateNodes) {
                adapter.encode(ppView, buffer);
            }

            adapter.encode(req.self, buffer);

            return buffer;
        }

        @Override
        public int getEncodedSize(CroupierContext context, Shuffle.Request req) {
            PeerView.Adapter adapter = context.getPPViewAdapter(req.self);
            int size = 0;
            size += NetUtil.getUUIDEncodedSize();
            size += 4; //overlayId
            size += 1; //PeerPublicView regCode
            size += 4; //publicNode size
            for (PeerView ppView : req.publicNodes) {
                size += req.publicNodes.size() * adapter.getEncodedSize(ppView);
            }
            size += 4; //privateNode size
            for (PeerView ppView : req.privateNodes) {
                size += req.privateNodes.size() * adapter.getEncodedSize(ppView);
            }
            size += adapter.getEncodedSize(req.self); //self

            return size;
        }
    }

    public static class Response implements CroupierAdapter.Response<Shuffle.Response> {

        @Override
        public Shuffle.Response decode(CroupierContext context, ByteBuf buffer) {
            UUID id = NetUtil.decodeUUID(buffer);
            int overlayId = buffer.readInt();

            byte regCode = buffer.readByte();
            PeerView.Adapter adapter = context.getPPViewAdapter(regCode);

            int publicNSize = buffer.readInt();
            List<PeerView> publicNodes = new ArrayList<PeerView>();
            for (int i = 0; i < publicNSize; i++) {
                publicNodes.add(adapter.decode(buffer));
            }

            int privateNSize = buffer.readInt();
            List<PeerView> privateNodes = new ArrayList<PeerView>();
            for (int i = 0; i < privateNSize; i++) {
                privateNodes.add(adapter.decode(buffer));
            }

            PeerView self = adapter.decode(buffer);

            return new Shuffle.Response(id, overlayId, publicNodes, privateNodes, self);
        }

        @Override
        public ByteBuf encode(CroupierContext context, Shuffle.Response resp, ByteBuf buffer) {
            NetUtil.encodeUUID(buffer, resp.id);
            buffer.writeInt(resp.croupierId);

            buffer.writeByte(context.getPPViewReqCode(resp.self));
            PeerView.Adapter adapter = context.getPPViewAdapter(resp.self);

            buffer.writeInt(resp.publicNodes.size());
            for (PeerView ppView : resp.publicNodes) {
                adapter.encode(ppView, buffer);
            }

            buffer.writeInt(resp.privateNodes.size());
            for (PeerView ppView : resp.privateNodes) {
                adapter.encode(ppView, buffer);
            }

            adapter.encode(resp.self, buffer);

            return buffer;
        }

        @Override
        public int getEncodedSize(CroupierContext context, Shuffle.Response resp) {
            PeerView.Adapter adapter = context.getPPViewAdapter(resp.self);
            int size = 0;
            size += NetUtil.getUUIDEncodedSize();
            size += 4; //overlayId
            size += 1; //PeerPublicView regCode
            size += 4; //publicNode size
            for (PeerView ppView : resp.publicNodes) {
                size += resp.publicNodes.size() * adapter.getEncodedSize(ppView);
            }
            size += 4; //privateNode size
            for (PeerView ppView : resp.privateNodes) {
                size += resp.privateNodes.size() * adapter.getEncodedSize(ppView);
            }
            size += adapter.getEncodedSize(resp.self); //self

            return size;
        }
    }
}
