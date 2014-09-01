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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.bootstrap.server.PeerOpManager;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerMsg;
import se.sics.gvod.bootstrap.server.peermanager.msg.JoinOverlay;
import se.sics.gvod.bootstrap.server.peermanager.msg.GetOverlaySample;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.BootstrapGlobalMsg;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.Util;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapGlobalOp implements Operation {

    private static enum Phase {

        GET_SYSTEM_SAMPLE, JOIN_SYSTEM
    }

    public final UUID id;
    private final PeerOpManager opMngr;
    private final BootstrapGlobalMsg.Request req;
    private final VodAddress src;
    private Phase phase;
    private BootstrapGlobalMsg.Response resp;

    public BootstrapGlobalOp(UUID id, PeerOpManager opMngr, BootstrapGlobalMsg.Request req, VodAddress src) {
        this.id = id;
        this.opMngr = opMngr;
        this.req = req;
        this.src = src;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void start() {
        phase = Phase.GET_SYSTEM_SAMPLE;
        opMngr.sendPeerManagerReq(id, new GetOverlaySample.Request(UUID.randomUUID(), 0));
    }

    @Override
    public void handle(PeerManagerMsg.Response peerResp) {
        if (phase == Phase.GET_SYSTEM_SAMPLE && peerResp instanceof GetOverlaySample.Response) {
            GetOverlaySample.Response phase1Resp = (GetOverlaySample.Response) peerResp;
            if (phase1Resp.status == ReqStatus.SUCCESS) {
                phase = Phase.JOIN_SYSTEM;
                opMngr.sendPeerManagerReq(req.reqId, new JoinOverlay.Request(UUID.randomUUID(), 0, src.getPeerAddress().getId(), Util.encodeVodAddress(Unpooled.buffer(), src).array()));
                resp = req.success(processOverlaySample(phase1Resp.overlaySample));
            } else {
                opMngr.finish(src, req.fail());
            }
        } else if (phase == Phase.JOIN_SYSTEM && peerResp instanceof JoinOverlay.Response) {
            JoinOverlay.Response phase2Resp = (JoinOverlay.Response) peerResp;
            if (phase2Resp.status == ReqStatus.SUCCESS) {
                opMngr.finish(src, resp);
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
