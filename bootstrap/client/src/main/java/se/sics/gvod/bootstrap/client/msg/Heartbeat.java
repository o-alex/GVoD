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
package se.sics.gvod.bootstrap.client.msg;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.common.msg.GvodMsg;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public final class Heartbeat extends GvodMsg.OneWay {

    public final Set<Integer> overlayIds;

    public Heartbeat(UUID reqId, Set<Integer> overlayIds) {
        super(reqId);
        this.overlayIds = overlayIds;
    }

    @Override
    public Heartbeat copy() {
        return new Heartbeat(reqId, new HashSet<Integer>(overlayIds));
    }
    
    @Override
    public String toString() {
        return "Heartbeat " + reqId.toString();
    }
}