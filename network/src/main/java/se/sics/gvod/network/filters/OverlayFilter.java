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
package se.sics.gvod.network.filters;

import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.gvod.network.netmsg.OverlayMsgI;
import se.sics.kompics.ChannelFilter;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 * and filter with context and overlayId
 */
public class OverlayFilter extends ChannelFilter<DirectMsg, Integer> {

    public OverlayFilter(Integer overlay) {
        super(DirectMsg.class, overlay, true);
    }

    @Override
    public Integer getValue(DirectMsg msg) {
        if (msg instanceof OverlayMsgI) {
            OverlayMsgI overlayMsg =  (OverlayMsgI) msg;
            return overlayMsg.getOverlay();
        } else {
            return -1;
        }
    }
}
