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

package se.sics.gvod.croupier.network;

import java.util.HashMap;
import java.util.Map;
import se.sics.gvod.croupier.msg.intern.Shuffle;
import se.sics.gvod.croupier.pub.common.MyAdapter;
import se.sics.gvod.croupier.pub.common.MyRegistry;
import se.sics.gvod.croupier.pub.util.PeerPublicView;
import se.sics.gvod.croupier.pub.util.PeerPublicViewRegistry;
import se.sics.kompics.KompicsEvent;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierRegistryImpl implements CroupierRegistry {
    public final byte SHUFFLE_REQUEST = 0x01;
    public final byte SHUFFLE_RESPONSE = 0x02;

    private final Map<Byte, MyAdapter> croupierAdapters = new HashMap<Byte, MyAdapter>();
    {
        croupierAdapters.put(SHUFFLE_REQUEST, new ShuffleAdapter.Request());
        croupierAdapters.put(SHUFFLE_RESPONSE, new ShuffleAdapter.Response());
    }
    
    private PeerPublicViewRegistry ppViewRegistry;
    
    public CroupierRegistryImpl(PeerPublicViewRegistry ppViewRegistry) {
        this.ppViewRegistry = ppViewRegistry;
    }

    @Override
    public MyAdapter getAdapter(byte regCode) {
        return croupierAdapters.get(regCode);
    }

    @Override
    public <E extends KompicsEvent> byte getRegCode(E msg) {
         if (msg instanceof Shuffle.Request) {
            return SHUFFLE_REQUEST;
        } else if (msg instanceof Shuffle.Response) {
            return SHUFFLE_RESPONSE;
        }
        throw new RuntimeException("no opcode translation");
    }

    @Override
    public <E extends KompicsEvent, R extends MyRegistry> MyAdapter<E, R> getAdapter(E msg) {
        return getAdapter(getRegCode(msg));
    }
    
     @Override
    public <E extends PeerPublicView> byte getPPViewReqCode(E msg) {
        return ppViewRegistry.getReqCode(msg);
    }

    @Override
    public <E extends PeerPublicView> PeerPublicView.Adapter<E> getPPViewAdapter(E msg) {
        return ppViewRegistry.getAdapter(msg);
    }

    @Override
    public PeerPublicView.Adapter getPPViewAdapter(byte regCode) {
        return ppViewRegistry.getAdapter(regCode);
    }
}
