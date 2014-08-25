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

package se.sics.gvod.network;

import java.util.HashMap;
import java.util.Map;
import se.sics.gvod.network.gvodadapter.AddOverlayAdapter;
import se.sics.gvod.network.gvodadapter.BootstrapGlobalAdapter;
import se.sics.gvod.network.gvodadapter.GVoDAdapter;
import se.sics.kompics.KompicsEvent;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GVoDAdapterFactory {
    //gvod
    public static final byte BOOTSTRAP_GLOBAL_REQUEST = 0x01;
    public static final byte BOOTSTRAP_GLOBAL_RESPONSE = 0x02;
    public static final byte BOOTSTRAP_OVERLAY_REQUEST = 0x03;
    public static final byte BOOTSTRAP_OVERLAY_RESPONSE = 0x04;
    public static final byte ADD_OVERLAY_REQUEST = 0x05;
    public static final byte ADD_OVERLAY_RESPONSE = 0x06;
    public static final byte JOIN_OVERLAY_REQUEST = 0x07;
    public static final byte JOIN_OVERLAY_RESPONSE = 0x08;
    
    private static final Map<Byte, GVoDAdapter<? extends KompicsEvent>> gvodAdapters = new HashMap<>();
    static {
        gvodAdapters.put(BOOTSTRAP_GLOBAL_REQUEST, new BootstrapGlobalAdapter.Request());
        gvodAdapters.put(BOOTSTRAP_GLOBAL_RESPONSE, new BootstrapGlobalAdapter.Response());
        gvodAdapters.put(ADD_OVERLAY_REQUEST, new AddOverlayAdapter.Request());
        gvodAdapters.put(ADD_OVERLAY_RESPONSE, new AddOverlayAdapter.Response());
    }
    
    public static GVoDAdapter getAdapter(byte opCode) {
        return gvodAdapters.get(opCode);
    }
}
