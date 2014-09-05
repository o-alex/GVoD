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
import se.sics.gvod.bootstrap.server.peermanager.msg.AddFileMetadata;
import se.sics.gvod.bootstrap.server.peermanager.msg.GetOverlaySample;
import se.sics.gvod.bootstrap.server.peermanager.msg.JoinOverlay;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.AddOverlayMsg;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.Util;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AddOverlayOp implements Operation {

    private static enum Phase {

        CHECK_OVERLAY, JOIN_OVERLAY, ADD_FILE_METADATA
    }

    private final PeerOpManager opMngr;
    private final AddOverlayMsg.Request req;
    private final VodAddress src;
    private Phase phase;

    public AddOverlayOp(PeerOpManager opMngr, AddOverlayMsg.Request req, VodAddress src) {
        this.opMngr = opMngr;
        this.req = req;
        this.src = src;
    }

    @Override
    public UUID getId() {
        return req.reqId;
    }

    @Override
    public void start() {
        phase = Phase.CHECK_OVERLAY;
        opMngr.sendPeerManagerReq(req.reqId, new GetOverlaySample.Request(UUID.randomUUID(), req.overlayId));
    }

    @Override
    public void handle(PeerManagerMsg.Response peerResp) {
        if (phase == Phase.CHECK_OVERLAY && peerResp instanceof GetOverlaySample.Response) {
            GetOverlaySample.Response phase1Resp = (GetOverlaySample.Response) peerResp;
            if (phase1Resp.status == ReqStatus.SUCCESS) {
                phase = Phase.JOIN_OVERLAY;
                opMngr.sendPeerManagerReq(req.reqId, new JoinOverlay.Request(UUID.randomUUID(), req.overlayId, src.getPeerAddress().getId(), Util.encodeVodAddress(Unpooled.buffer(), src).array()));
            } else {
                opMngr.finish(src, req.fail());
            }
        } else if (phase == Phase.JOIN_OVERLAY && peerResp instanceof JoinOverlay.Response) {
            JoinOverlay.Response phase2Resp = (JoinOverlay.Response) peerResp;
            if (phase2Resp.status == ReqStatus.SUCCESS) {
                phase = Phase.ADD_FILE_METADATA;
                opMngr.sendPeerManagerReq(req.reqId, new AddFileMetadata.Request(UUID.randomUUID(), req.overlayId, Util.encodeFileMeta(Unpooled.buffer(), req.fileMeta).array()));
            } else {
                opMngr.finish(src, req.fail());
            }
        } else if (phase == Phase.ADD_FILE_METADATA && peerResp instanceof AddFileMetadata.Response) {
            AddFileMetadata.Response phase3Resp = (AddFileMetadata.Response) peerResp;
            if (phase3Resp.status == ReqStatus.SUCCESS) {
                opMngr.finish(src, req.success());
            } else {
                opMngr.finish(src, req.fail());
            }
        } else {
            throw new RuntimeException("wrong phase");
        }
    }
}
