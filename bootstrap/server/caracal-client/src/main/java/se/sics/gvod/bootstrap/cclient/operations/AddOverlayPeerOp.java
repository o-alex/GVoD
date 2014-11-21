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
package se.sics.gvod.bootstrap.cclient.operations;

import java.util.UUID;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.caracaldb.operations.PutRequest;
import se.sics.caracaldb.operations.PutResponse;
import se.sics.caracaldb.operations.ResponseCode;
import se.sics.gvod.bootstrap.cclient.CaracalKeyFactory;
import se.sics.gvod.bootstrap.cclient.CaracalOpManager;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMJoinOverlay;
import se.sics.gvod.common.util.Operation;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AddOverlayPeerOp implements Operation<CaracalOp> {

    private final CaracalOpManager opMngr;
    private final PMJoinOverlay.Request req;

    public AddOverlayPeerOp(CaracalOpManager opMngr, PMJoinOverlay.Request req) {
        this.opMngr = opMngr;
        this.req = req;
    }

    @Override
    public UUID getId() {
        return req.id;
    }

    @Override
    public void start() {
        opMngr.sendCaracalReq(req.id, CaracalKeyFactory.getOverlayPeerKey(req.overlayId, req.nodeId), new PutRequest(UUID.randomUUID(), CaracalKeyFactory.getOverlayPeerKey(req.overlayId, req.nodeId), req.nodeInfo));
    }

    @Override
    public void handle(CaracalOp caracalResp) {
        if (caracalResp instanceof PutResponse) {
            PutResponse phase1Resp = (PutResponse) caracalResp;
            if (phase1Resp.code == ResponseCode.SUCCESS) {
                opMngr.finish(req.success());
            } else {
                opMngr.finish(req.fail());
            }
        } else {
            throw new RuntimeException("wrong phase");
        }
    }
}