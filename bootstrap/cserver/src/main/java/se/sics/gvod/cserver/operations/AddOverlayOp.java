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
package se.sics.gvod.cserver.operations;

import java.util.UUID;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.caracaldb.operations.PutRequest;
import se.sics.caracaldb.operations.PutResponse;
import se.sics.caracaldb.operations.RangeQuery;
import se.sics.caracaldb.operations.ResponseCode;
import se.sics.caracaldb.store.ActionFactory;
import se.sics.caracaldb.store.Limit;
import se.sics.caracaldb.store.TFFactory;
import se.sics.gvod.bootstrap.common.msg.AddOverlay;
import se.sics.gvod.cserver.CaracalKeyFactory;
import se.sics.gvod.cserver.OperationManager;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AddOverlayOp implements Operation {

    private static enum Phase {

        CHECK_OVERLAY, ADD_OVERLAY
    }

    private final OperationManager opMngr;
    private final AddOverlay.Request req;
    private Phase phase;

    public AddOverlayOp(OperationManager opMngr, AddOverlay.Request req) {
        this.opMngr = opMngr;
        this.req = req;
    }

    @Override
    public UUID getId() {
        return req.id;
    }

    @Override
    public void start() {
        phase = Phase.CHECK_OVERLAY;
        opMngr.sendCaracalOp(req.id, new RangeQuery.Request(UUID.randomUUID(), CaracalKeyFactory.getOverlayRange(req.overlayId), Limit.toItems(1), TFFactory.noTF(), ActionFactory.noop(), RangeQuery.Type.SEQUENTIAL));
    }

    @Override
    public void handle(CaracalOp caracalResp) {
        if (phase == Phase.CHECK_OVERLAY && caracalResp instanceof RangeQuery.Response) {
            RangeQuery.Response phase1Resp = (RangeQuery.Response) caracalResp;
            if (phase1Resp.code == ResponseCode.SUCCESS && phase1Resp.data.isEmpty()) {
                phase = Phase.ADD_OVERLAY;
                opMngr.sendCaracalOp(req.id, new PutRequest(UUID.randomUUID(), CaracalKeyFactory.getOverlayPeerKey(req.overlayId, req.nodeId), req.peer));
            } else {
                opMngr.finish(req.fail());
            }

        } else if (phase == Phase.ADD_OVERLAY && caracalResp instanceof PutResponse) {
            PutResponse phase2Resp = (PutResponse) caracalResp;
            if (phase2Resp.code == ResponseCode.SUCCESS) {
                opMngr.finish(req.success());
            } else {
                opMngr.finish(req.fail());
            }
        } else {
            throw new RuntimeException("wrong phase");
        }
    }
}
