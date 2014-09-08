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
import se.sics.gvod.common.msg.impl.AddOverlay;
import se.sics.gvod.common.msg.impl.BootstrapGlobal;
import se.sics.gvod.common.msg.impl.Heartbeat;
import se.sics.gvod.common.msg.impl.JoinOverlay;
import se.sics.gvod.network.gvodadapter.AddOverlayAdapter;
import se.sics.gvod.network.gvodadapter.BootstrapGlobalAdapter;
import se.sics.gvod.common.network.LocalNettyAdapter;
import se.sics.gvod.network.gvodadapter.HeartbeatAdapter;
import se.sics.gvod.network.gvodadapter.JoinOverlayAdapter;
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
    public static final byte OVERLAY_HEARTBEAT = 0x09;

    private static final Map<Byte, LocalNettyAdapter<? extends KompicsEvent>> gvodAdapters = new HashMap<Byte, LocalNettyAdapter<? extends KompicsEvent>>();

    static {
        gvodAdapters.put(BOOTSTRAP_GLOBAL_REQUEST, new BootstrapGlobalAdapter.Request());
        gvodAdapters.put(BOOTSTRAP_GLOBAL_RESPONSE, new BootstrapGlobalAdapter.Response());
        gvodAdapters.put(ADD_OVERLAY_REQUEST, new AddOverlayAdapter.Request());
        gvodAdapters.put(ADD_OVERLAY_RESPONSE, new AddOverlayAdapter.Response());
        gvodAdapters.put(JOIN_OVERLAY_REQUEST, new JoinOverlayAdapter.Request());
        gvodAdapters.put(JOIN_OVERLAY_RESPONSE, new JoinOverlayAdapter.Response());
        gvodAdapters.put(OVERLAY_HEARTBEAT, new HeartbeatAdapter.OneWay());
    }

    public static LocalNettyAdapter getAdapter(byte opCode) {
        return gvodAdapters.get(opCode);
    }

    public static <E extends KompicsEvent> byte getOpcode(E msg) {
        if (msg instanceof BootstrapGlobal.Request) {
            return BOOTSTRAP_GLOBAL_REQUEST;
        } else if (msg instanceof BootstrapGlobal.Response) {
            return BOOTSTRAP_GLOBAL_RESPONSE;
        } else if (msg instanceof AddOverlay.Request) {
            return ADD_OVERLAY_REQUEST;
        } else if (msg instanceof AddOverlay.Response) {
            return ADD_OVERLAY_RESPONSE;
        } else if (msg instanceof JoinOverlay.Request) {
            return JOIN_OVERLAY_REQUEST;
        } else if (msg instanceof JoinOverlay.Response) {
            return JOIN_OVERLAY_RESPONSE;
        } else if (msg instanceof Heartbeat.OneWay) {
            return OVERLAY_HEARTBEAT;
        }
        throw new RuntimeException("no opcode translation");
    }
    
    public static <M extends KompicsEvent> LocalNettyAdapter getAdapter(M msg) {
        return getAdapter(getOpcode(msg));
    }
}
