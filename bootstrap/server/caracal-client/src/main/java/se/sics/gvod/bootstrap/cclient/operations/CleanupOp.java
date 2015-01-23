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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import se.sics.caracaldb.Key;
import se.sics.caracaldb.KeyRange;
import se.sics.caracaldb.global.SchemaData;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.caracaldb.operations.PutRequest;
import se.sics.caracaldb.operations.RangeQuery;
import se.sics.caracaldb.operations.ResponseCode;
import se.sics.caracaldb.store.ActionFactory;
import se.sics.caracaldb.store.Limit;
import se.sics.caracaldb.store.TFFactory;
import se.sics.gvod.bootstrap.cclient.CaracalOpManager;
import se.sics.gvod.common.util.Operation;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */

//TODO Alex change to cleaner version as soon as possible
public class CleanupOp implements Operation<CaracalOp> {

    private final CaracalOpManager opMngr;
    private final SchemaData schemaData;
    private final UUID id;
    private final Random rand;

    public CleanupOp(CaracalOpManager opMngr, SchemaData schemaData, Random rand) {
        this.opMngr = opMngr;
        this.schemaData = schemaData;
        this.id = UUID.randomUUID();
        this.rand = rand;
    }

    @Override
    public void start() {
        Key key1 = randomKey(8);
        Key key2 = new Key(schemaData.getId("gvod.heartbeat")).inc();
        KeyRange range = KeyRange.closed(key1).open(key2);
        opMngr.sendCaracalReq(id, range.begin, new RangeQuery.Request(id, range, Limit.toItems(20), TFFactory.noTF(), ActionFactory.noop(), RangeQuery.Type.SEQUENTIAL));
    }

    @Override
    public void handle(CaracalOp caracalResp) {
        if (caracalResp instanceof RangeQuery.Response) {
            RangeQuery.Response phase1Resp = (RangeQuery.Response) caracalResp;
            if (phase1Resp.code == ResponseCode.SUCCESS) {
                Set<byte[]> overlaySample = new HashSet<byte[]>();
                for (Map.Entry<Key, byte[]> e : phase1Resp.data.entrySet()) {
                    overlaySample.add(e.getValue());
                }
                Set<Key> deleteKeys = processOverlaySample(phase1Resp.data);
                for(Key key : deleteKeys) {
                    opMngr.sendCaracalReq(id, key, new PutRequest(id, key, null));
                }
                opMngr.finish(id, null);
            } else {
                opMngr.finish(id, null);
            }
        } else {
            throw new RuntimeException("wrong phase");
        }
    }

    private Key randomKey(int size) {
        byte[] bytes = new byte[size];
        rand.nextBytes(bytes);
        Key key = new Key(bytes);
        return key.append(schemaData.getId("gvod.heartbeat")).get();
    }

    //TODO Alex duplicate code from SerializerHelper and Helper
    public static long getTimestamp(ByteBuf buf) {
        long timestamp = buf.readLong();
        return timestamp;
    }

    public static Set<Key> processOverlaySample(Map<Key, byte[]> overlaySample) {
        Set<Key> oldSamples = new HashSet<Key>();
        //TODO Alex fix hardcoded timestamp old
        long newT = System.nanoTime();
        long difT = 60l * 1000 * 1000 * 1000; //1min 
        for (Map.Entry<Key, byte[]> e : overlaySample.entrySet()) {
            if(e.getValue() == null) {
                continue;
            }
            long timestamp = getTimestamp(Unpooled.wrappedBuffer(e.getValue()));
            if (timestamp + difT < newT) {
                oldSamples.add(e.getKey());
            }
        }
        return oldSamples;
    }

    @Override
    public UUID getId() {
        return id;
    }
}
