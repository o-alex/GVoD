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
import se.sics.gvod.bootstrap.server.peermanager.msg.PMGetOverlaySample;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.impl.OverlaySample;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.Util;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class OverlaySampleOp implements Operation {

    private final PeerOpManager opMngr;
    private final OverlaySample.Request req;
    private final VodAddress src;

    public OverlaySampleOp(PeerOpManager opMngr, OverlaySample.Request req, VodAddress src) {
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
        opMngr.sendPeerManagerReq(getId(), new PMGetOverlaySample.Request(UUID.randomUUID(), req.overlayId));
    }

    @Override
    public void handle(PeerManagerMsg.Response resp) {
        if (resp instanceof PMGetOverlaySample.Response) {
            PMGetOverlaySample.Response sampleResp = (PMGetOverlaySample.Response) resp;
            OverlaySample.Response opResp;
            if (sampleResp.status == ReqStatus.SUCCESS) {
                opResp = req.success(processOverlaySample(sampleResp.overlaySample));
            } else {
                opResp = req.fail();
            }
            opMngr.finish(getId(), src, opResp);
        } else {
            throw new RuntimeException("wrong phase");
        }
    }

    private Map<VodAddress, Integer> processOverlaySample(Set<byte[]> boverlaySample) {
        Map<VodAddress, Integer> overlaySample = new HashMap<VodAddress, Integer>();
        for (byte[] data : boverlaySample) {
            Pair<VodAddress, Integer> heartbeatEntry = Util.decodeHeartbeatEntry(Unpooled.wrappedBuffer(data));
            overlaySample.put(heartbeatEntry.getValue0(), heartbeatEntry.getValue1());
        }
        return overlaySample;
    }
}
