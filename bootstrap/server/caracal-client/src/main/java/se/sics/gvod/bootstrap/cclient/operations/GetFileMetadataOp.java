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
import se.sics.caracaldb.operations.GetRequest;
import se.sics.caracaldb.operations.GetResponse;
import se.sics.caracaldb.operations.ResponseCode;
import se.sics.gvod.bootstrap.cclient.CaracalKeyFactory;
import se.sics.gvod.bootstrap.cclient.CaracalOpManager;
import se.sics.gvod.bootstrap.server.peermanager.msg.GetFileMetadata;
import se.sics.gvod.common.util.Operation;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GetFileMetadataOp implements Operation<CaracalOp> {
    private final CaracalOpManager opMngr;
    private final GetFileMetadata.Request req;

    public GetFileMetadataOp(CaracalOpManager opMngr, GetFileMetadata.Request req) {
        this.opMngr = opMngr;
        this.req = req;
    }

    @Override
    public UUID getId() {
        return req.id;
    }

    @Override
    public void start() {
        opMngr.sendCaracalReq(req.id, new GetRequest(UUID.randomUUID(), CaracalKeyFactory.getFileMetadataKey(req.overlayId)));
    }

    @Override
    public void handle(CaracalOp caracalResp) {
        if (caracalResp instanceof GetResponse) {
            GetResponse phase1Resp = (GetResponse) caracalResp;
            if (phase1Resp.code == ResponseCode.SUCCESS) {
                opMngr.finish(req.success(phase1Resp.data));
            } else {
                opMngr.finish(req.fail());
            }
        } else {
            throw new RuntimeException("wrong phase");
        }
    }
}
