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

import io.netty.buffer.Unpooled;
import java.util.UUID;
import se.sics.gvod.bootstrap.server.PeerOpManager;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerMsg;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMAddFileMetadata;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMGetOverlaySample;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.AddOverlay;
import se.sics.gvod.common.util.FileMetadata;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AddOverlayOp implements Operation {

    private static enum Phase {
        CHECK_OVERLAY, ADD_FILE_METADATA
    }

    private final PeerOpManager opMngr;
    private final SerializationContext context;
    private final AddOverlay.Request req;
    private final VodAddress src;
    
    private Phase phase;
    

    public AddOverlayOp(PeerOpManager opMngr, SerializationContext context, AddOverlay.Request req, VodAddress src) {
        this.opMngr = opMngr;
        this.context = context;
        this.req = req;
        this.src = src;
    }

    @Override
    public UUID getId() {
        return req.id;
    }

    @Override
    public void start() {
        phase = Phase.CHECK_OVERLAY;
        opMngr.sendPeerManagerReq(getId(), new PMGetOverlaySample.Request(UUID.randomUUID(), req.overlayId));
    }

    @Override
    public void handle(PeerManagerMsg.Response peerResp) {
        if (phase == Phase.CHECK_OVERLAY && peerResp instanceof PMGetOverlaySample.Response) {
            PMGetOverlaySample.Response phase1Resp = (PMGetOverlaySample.Response) peerResp;
            if (phase1Resp.status == ReqStatus.SUCCESS) {
                phase = Phase.ADD_FILE_METADATA;
                byte[] bytes;
                try {
                    bytes = context.getSerializer(FileMetadata.class).encode(context, Unpooled.buffer(), req.fileMeta).array();
                } catch (Serializer.SerializerException ex) {
                    System.exit(1);
                    throw new RuntimeException(ex);
                } catch (SerializationContext.MissingException ex) {
                    System.exit(1);
                    throw new RuntimeException(ex);
                }
                opMngr.sendPeerManagerReq(getId(), new PMAddFileMetadata.Request(UUID.randomUUID(), req.overlayId, bytes));
            } else {
                opMngr.finish(getId(), src, req.fail());
            }
        } else if (phase == Phase.ADD_FILE_METADATA && peerResp instanceof PMAddFileMetadata.Response) {
            PMAddFileMetadata.Response phase2Resp = (PMAddFileMetadata.Response) peerResp;
            if (phase2Resp.status == ReqStatus.SUCCESS) {
                opMngr.finish(getId(), src, req.success());
            } else {
                opMngr.finish(getId(), src, req.fail());
            }
        } else {
            System.exit(1);
            throw new RuntimeException("wrong phase");
        }
    }
}
