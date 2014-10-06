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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import se.sics.caracaldb.Key;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.caracaldb.operations.RangeQuery;
import se.sics.caracaldb.operations.ResponseCode;
import se.sics.caracaldb.store.ActionFactory;
import se.sics.caracaldb.store.Limit;
import se.sics.caracaldb.store.TFFactory;
import se.sics.gvod.bootstrap.cclient.CaracalKeyFactory;
import se.sics.gvod.bootstrap.cclient.CaracalOpManager;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMGetOverlaySample;
import se.sics.gvod.common.util.Operation;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GetOverlaySampleOp implements Operation<CaracalOp> {
    
    private final CaracalOpManager opMngr;
    private final PMGetOverlaySample.Request req;
    private final int sampleSize;
    
    public GetOverlaySampleOp(CaracalOpManager opMngr, PMGetOverlaySample.Request req, int sampleSize) {
        this.opMngr = opMngr;
        this.req = req;
        this.sampleSize = sampleSize;
    }
        
    @Override
    public UUID getId() {
        return req.id;
    }

    @Override
    public void start() {
        opMngr.sendCaracalReq(req.id, new RangeQuery.Request(UUID.randomUUID(), CaracalKeyFactory.getOverlayRange(req.overlayId), Limit.toItems(sampleSize), TFFactory.noTF(), ActionFactory.noop(), RangeQuery.Type.SEQUENTIAL));
    }

    @Override
    public void handle(CaracalOp caracalResp) {
         if (caracalResp instanceof RangeQuery.Response) {
            RangeQuery.Response phase1Resp = (RangeQuery.Response) caracalResp;
            if (phase1Resp.code == ResponseCode.SUCCESS) {
                Set<byte[]> overlaySample = new HashSet<byte[]>();
                for(Map.Entry<Key,byte[]> e : phase1Resp.data.entrySet()) {
                    overlaySample.add(e.getValue());
                }
                opMngr.finish(req.success(overlaySample));
            } else {
                opMngr.finish(req.fail());
            }
        } else {
            throw new RuntimeException("wrong phase");
        }
    }
}