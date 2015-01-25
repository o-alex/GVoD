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
import se.sics.caracaldb.KeyRange;
import se.sics.caracaldb.global.SchemaData;
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
    private final SchemaData schemaData;
    private final int sampleSize;

    public GetOverlaySampleOp(CaracalOpManager opMngr, PMGetOverlaySample.Request req, SchemaData schemaData, int sampleSize) {
        this.opMngr = opMngr;
        this.req = req;
        this.sampleSize = sampleSize;
        this.schemaData = schemaData;
    }

    @Override
    public UUID getId() {
        return req.id;
    }

    @Override
    public void start() {
        KeyRange overlayRange = CaracalKeyFactory.getOverlayRange(req.overlayId);
        Key startK = overlayRange.begin.prepend(schemaData.getId("gvod.heartbeat")).get();
        Key endK = overlayRange.end.prepend(schemaData.getId("gvod.heartbeat")).get();
        KeyRange newRange = overlayRange.replaceKeys(startK, endK);
        opMngr.sendCaracalReq(req.id, newRange.begin, new RangeQuery.Request(UUID.randomUUID(), newRange, Limit.toItems(sampleSize), TFFactory.noTF(), ActionFactory.noop(), RangeQuery.Type.SEQUENTIAL));
    }

    @Override
    public void handle(CaracalOp caracalResp) {
        if (caracalResp instanceof RangeQuery.Response) {
            RangeQuery.Response phase1Resp = (RangeQuery.Response) caracalResp;
            if (phase1Resp.code == ResponseCode.SUCCESS) {
                Set<byte[]> overlaySample = new HashSet<byte[]>();
                for (Map.Entry<Key, byte[]> e : phase1Resp.data.entrySet()) {
                    if(e.getValue() == null) {
                        continue;
                    }
                    overlaySample.add(e.getValue());
                }
                opMngr.finish(req.id, req.success(overlaySample));
            } else {
                opMngr.finish(req.id, req.fail());
            }
        } else {
            System.exit(1);
            throw new RuntimeException("wrong phase");
        }
    }
}
