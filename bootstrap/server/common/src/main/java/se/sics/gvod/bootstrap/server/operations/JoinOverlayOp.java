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
package se.sics.gvod.bootstrap.server.operations;

import com.google.common.base.Optional;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import se.sics.gvod.bootstrap.server.PeerOpManager;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerMsg;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMGetFileMetadata;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.JoinOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class JoinOverlayOp implements Operation {

    private final PeerOpManager opMngr;
    private final JoinOverlay.Request req;
    private final DecoratedAddress src;

    public JoinOverlayOp(PeerOpManager opMngr, JoinOverlay.Request req, DecoratedAddress src) {
        this.opMngr = opMngr;
        this.req = req;
        this.src = src;
    }

    @Override
    public UUID getId() {
        return req.id;
    }

    @Override
    public void start() {
        opMngr.sendPeerManagerReq(getId(), new PMGetFileMetadata.Request(UUID.randomUUID(), req.overlayId));
    }

    @Override
    public void handle(PeerManagerMsg.Response peerResp) {
        if (peerResp instanceof PMGetFileMetadata.Response) {
            PMGetFileMetadata.Response fileResp = (PMGetFileMetadata.Response) peerResp;
            if (fileResp.status == ReqStatus.SUCCESS) {
                FileMetadata fileMetadata = (FileMetadata)Serializers.lookupSerializer(FileMetadata.class).fromBinary(Unpooled.wrappedBuffer(fileResp.fileMetadata), Optional.absent());
                opMngr.finish(getId(), src, req.success(fileMetadata));
            } else {
                opMngr.finish(getId(), src, req.fail());
            }
        } else {
            System.exit(1);
            throw new RuntimeException("wrong phase");
        }
    }
}
