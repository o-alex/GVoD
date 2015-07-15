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
import se.sics.caracaldb.Key;
import se.sics.caracaldb.global.SchemaData;
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
    private final SchemaData schemaData;

    public AddOverlayPeerOp(CaracalOpManager opMngr, PMJoinOverlay.Request req, SchemaData schemaData) {
        this.opMngr = opMngr;
        this.req = req;
        this.schemaData = schemaData;
    }

    @Override
    public UUID getId() {
        return req.id;
    }

    @Override
    public void start() {
        Key target = CaracalKeyFactory.getOverlayPeerKey(req.overlayId, req.nodeId).prepend(schemaData.getId("gvod.heartbeat")).get();
        opMngr.sendCaracalReq(req.id, target, new PutRequest(UUID.randomUUID(), target, req.nodeInfo));
    }

    @Override
    public void handle(CaracalOp caracalResp) {
        if (caracalResp instanceof PutResponse) {
            PutResponse phase1Resp = (PutResponse) caracalResp;
            if (phase1Resp.code == ResponseCode.SUCCESS) {
                opMngr.finish(req.id, req.success());
            } else {
                opMngr.finish(req.id, req.fail());
            }
        } else {
            System.exit(1);
            throw new RuntimeException("wrong phase");
        }
    }
}