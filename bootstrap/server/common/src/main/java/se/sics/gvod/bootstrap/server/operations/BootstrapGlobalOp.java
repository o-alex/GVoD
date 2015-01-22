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

import java.util.UUID;
import se.sics.gvod.bootstrap.server.PeerOpManager;
import se.sics.gvod.bootstrap.server.operations.util.Helper;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerMsg;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMJoinOverlay;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMGetOverlaySample;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.BootstrapGlobal;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.network.serializers.SerializationContext;
import se.sics.gvod.network.serializers.Serializer;
import se.sics.gvod.network.serializers.util.SerializerHelper;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapGlobalOp implements Operation {

    private static enum Phase {

        GET_SYSTEM_SAMPLE
    }

    private final PeerOpManager opMngr;
    private final SerializationContext context;
    private final BootstrapGlobal.Request req;
    private final VodAddress src;
    private Phase phase;
    private BootstrapGlobal.Response resp;

    public BootstrapGlobalOp(PeerOpManager opMngr, SerializationContext context, BootstrapGlobal.Request req, VodAddress src) {
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
        phase = Phase.GET_SYSTEM_SAMPLE;
        opMngr.sendPeerManagerReq(getId(), new PMGetOverlaySample.Request(UUID.randomUUID(), 0));
    }

    @Override
    public void handle(PeerManagerMsg.Response peerResp) {
        if (phase == Phase.GET_SYSTEM_SAMPLE && peerResp instanceof PMGetOverlaySample.Response) {
            PMGetOverlaySample.Response phase1Resp = (PMGetOverlaySample.Response) peerResp;
            if (phase1Resp.status == ReqStatus.SUCCESS) {
                byte[] bytes;
                try {
                    bytes = SerializerHelper.serializeOverlayData(context, src, System.nanoTime(), 0);
                    opMngr.sendPeerManagerReq(getId(), new PMJoinOverlay.Request(UUID.randomUUID(), 0, src.getPeerAddress().getId(), bytes));
                    resp = req.success(Helper.processOverlaySample(context, phase1Resp.overlaySample).keySet());
                } catch (Serializer.SerializerException ex) {
                    throw new RuntimeException(ex);
                } catch (SerializationContext.MissingException ex) {
                    throw new RuntimeException(ex);
                }
                opMngr.finish(getId(), src, resp);
            } else {
                opMngr.finish(getId(), src, req.fail());
            }
        } else {
            throw new RuntimeException("wrong phase");
        }
    }
}
