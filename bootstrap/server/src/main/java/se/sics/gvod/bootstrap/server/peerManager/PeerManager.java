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

package se.sics.gvod.bootstrap.server.peerManager;

import java.util.Set;
import se.sics.gvod.net.VodAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public interface PeerManager {
    
    public Set<VodAddress> getSystemSample();
    public void addVodPeer(VodAddress peerAdr);
    public void addOverlay(int overlayId) throws PMException;
    public void addOverlayPeer(int overlayId, VodAddress peerAdr) throws PMException;
    public Set<VodAddress> getOverlaySample(int overlayId) throws PMException ;
    
    public static class PMException extends Exception {
        public PMException(String message) {
            super(message);
        }
    }
}