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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.javatuples.Pair;
import se.sics.gvod.bootstrap.server.PeerOpManager;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerMsg;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMGetFileMetadata;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMGetOverlaySample;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMJoinOverlay;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.JoinOverlay;
import se.sics.gvod.common.util.BuilderException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.Util;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class JoinOverlayOp implements Operation {

    private final PeerOpManager opMngr;
    private final JoinOverlay.Request req;
    private final VodAddress src;
    private final JoinOverlay.ResponseBuilder resp;

    public JoinOverlayOp(PeerOpManager opMngr, JoinOverlay.Request req, VodAddress src) {
        this.opMngr = opMngr;
        this.req = req;
        this.src = src;
        this.resp = req.getResponseBuilder();
    }

    @Override
    public UUID getId() {
        return req.id;
    }

    @Override
    public void start() {
        opMngr.sendPeerManagerReq(getId(), new PMGetFileMetadata.Request(UUID.randomUUID(), req.overlayId));
        opMngr.sendPeerManagerReq(getId(), new PMJoinOverlay.Request(UUID.randomUUID(), req.overlayId, src.getPeerAddress().getId(), Util.encodeVodAddress(Unpooled.buffer(), src).array()));

    }

    @Override
    public void handle(PeerManagerMsg.Response peerResp) {
        if (peerResp instanceof PMJoinOverlay.Response) {
            PMJoinOverlay.Response joinResp = (PMJoinOverlay.Response) peerResp;
            if (joinResp.status == ReqStatus.SUCCESS) {
                try {
                    opMngr.finish(getId(), src, resp.finalise(joinResp.status));
                } catch (BuilderException.Missing ex) {
                    return;
                }
            } else {
                opMngr.finish(getId(), src, req.fail());
            }
        } else if (peerResp instanceof PMGetFileMetadata.Response) {
            PMGetFileMetadata.Response fileResp = (PMGetFileMetadata.Response) peerResp;
            if (fileResp.status == ReqStatus.SUCCESS) {
                resp.setFileMetadata(Util.decodeFileMeta(Unpooled.wrappedBuffer(fileResp.fileMetadata)));
                try {
                    opMngr.finish(getId(), src, resp.finalise(fileResp.status));
                } catch (BuilderException.Missing ex) {
                    return;
                }
            } else {
                opMngr.finish(getId(), src, req.fail());
            }
        } else {
            throw new RuntimeException("wrong phase");
        }
    }
}
