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
import se.sics.gvod.croupier.CroupierMsg;
import se.sics.gvod.croupier.msg.intern.Shuffle;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierRegistry {

    public static byte CROUPIER_NET_REQUEST = 0x00;
    public static byte CROUPIER_NET_RESPONSE = 0x00;
    
    public static void registerCroupierNetMsgs(byte croupierNetRequest, byte croupierNetResponse) {
        if(CROUPIER_NET_REQUEST != 0x00 || CROUPIER_NET_RESPONSE !=0x00) {
            throw new RuntimeException("CroupierRegistry should only initialize net codes once");
        }
        CROUPIER_NET_REQUEST = croupierNetRequest;
        CROUPIER_NET_RESPONSE = croupierNetResponse;
    }
    
    public static void checkRegisteredCroupierNetMsgs() {
        if(CROUPIER_NET_REQUEST == 0x00 || CROUPIER_NET_RESPONSE ==0x00) {
            throw new RuntimeException("CroupierRegistry not properly initialized");
        }
    }
    public static final byte SHUFFLE_REQUEST = 0x01;
    public static final byte SHUFFLE_RESPONSE = 0x02;

    private static final Map<Byte, CroupierAdapter> croupierAdapters = new HashMap<Byte, CroupierAdapter>();

    {
        croupierAdapters.put(SHUFFLE_REQUEST, new ShuffleAdapter.Request());
        croupierAdapters.put(SHUFFLE_RESPONSE, new ShuffleAdapter.Response());
    }

    private static final Map<Integer, CroupierContext> croupierContexts = new HashMap<Integer, CroupierContext>();

    public static CroupierAdapter getAdapter(byte regCode) {
        return croupierAdapters.get(regCode);
    }

    public static <E extends CroupierMsg.CroupierBase> byte getRegCode(E msg) {
        if (msg instanceof Shuffle.Request) {
            return SHUFFLE_REQUEST;
        } else if (msg instanceof Shuffle.Response) {
            return SHUFFLE_RESPONSE;
        }
        throw new RuntimeException("no opcode translation");
    }

    public static <E extends CroupierMsg.CroupierBase> CroupierAdapter<E> getAdapter(E msg) {
        CroupierAdapter<E> adapter = getAdapter(getRegCode(msg));
        if (adapter == null) {
            throw new RuntimeException(new NullPointerException("unregistered adapter for msg" + msg.getClass()));
        }
        return adapter;
    }

    public static CroupierContext getContext(int overlayId) {
        CroupierContext context = croupierContexts.get(overlayId);
        if(context == null) {
            throw new RuntimeException(new NullPointerException("unregistered context for croupier with overlayId " + overlayId));
        }
        return context;
    }
    
    public static void registerContext(int overlayId, CroupierContext context) {
        if(croupierContexts.containsKey(overlayId)) {
            throw new RuntimeException("context already registered for overlay " + overlayId);
        }
        croupierContexts.put(overlayId, context);
    }
    
}
