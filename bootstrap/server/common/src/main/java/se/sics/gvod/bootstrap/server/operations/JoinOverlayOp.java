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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.sics.gvod.bootstrap.server.PeerOpManager;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerMsg;
import se.sics.gvod.bootstrap.server.peermanager.msg.GetFileMetadata;
import se.sics.gvod.bootstrap.server.peermanager.msg.GetOverlaySample;
import se.sics.gvod.bootstrap.server.peermanager.msg.JoinOverlay;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.JoinOverlayMsg;
import se.sics.gvod.common.util.BuilderException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.Util;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class JoinOverlayOp implements Operation {

    private final PeerOpManager opMngr;
    private final JoinOverlayMsg.Request req;
    private final VodAddress src;
    private JoinOverlayMsg.ResponseBuilder resp;

    public JoinOverlayOp(PeerOpManager opMngr, JoinOverlayMsg.Request req, VodAddress src) {
        this.opMngr = opMngr;
        this.req = req;
        this.src = src;
        this.resp = req.getResponseBuilder();
    }

    @Override
    public UUID getId() {
        return req.reqId;
    }

    @Override
    public void start() {
        opMngr.sendPeerManagerReq(req.reqId, new GetOverlaySample.Request(UUID.randomUUID(), req.overlayId));
    }

    @Override
    public void handle(PeerManagerMsg.Response peerResp) {
        if (peerResp instanceof GetOverlaySample.Response) {
            GetOverlaySample.Response sampleResp = (GetOverlaySample.Response) peerResp;
            if (sampleResp.status == ReqStatus.SUCCESS) {
                resp.setOverlaySample(processOverlaySample(sampleResp.overlaySample));
                opMngr.sendPeerManagerReq(req.reqId, new GetFileMetadata.Request(UUID.randomUUID(), req.overlayId));
                opMngr.sendPeerManagerReq(req.reqId, new JoinOverlay.Request(UUID.randomUUID(), sampleResp.overlayId, src.getPeerAddress().getId(), Util.encodeVodAddress(Unpooled.buffer(), src).array()));
            } else {
                opMngr.finish(src, req.fail());
            }
        } else if (peerResp instanceof JoinOverlay.Response) {
            JoinOverlay.Response joinResp = (JoinOverlay.Response) peerResp;
            if (joinResp.status == ReqStatus.SUCCESS) {
                try {
                    opMngr.finish(src, resp.finalise(joinResp.status));
                } catch (BuilderException.Missing ex) {

                }
            } else {
                opMngr.finish(src, req.fail());
            }
        } else if (peerResp instanceof GetFileMetadata.Response) {
            GetFileMetadata.Response fileResp = (GetFileMetadata.Response) peerResp;
            if (fileResp.status == ReqStatus.SUCCESS) {
                resp.setFileMetadata(Util.decodeFileMeta(Unpooled.wrappedBuffer(fileResp.fileMetadata)));
                try {
                    opMngr.finish(src, resp.finalise(fileResp.status));
                } catch (BuilderException.Missing ex) {

                }
            } else {
                opMngr.finish(src, req.fail());
            }
        } else {
            throw new RuntimeException("wrong phase");
        }
    }

    private Set<VodAddress> processOverlaySample(Set<byte[]> boverlaySample) {
        Set<VodAddress> overlaySample = new HashSet<VodAddress>();
        for (byte[] peer : boverlaySample) {
            overlaySample.add(Util.decodeVodAddress(Unpooled.wrappedBuffer(peer)));
        }
        return overlaySample;
    }
}
