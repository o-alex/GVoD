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
package se.sics.gvod.bootstrap.cclient;

import java.nio.ByteBuffer;
import se.sics.caracaldb.Key;
import se.sics.caracaldb.KeyRange;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CaracalKeyFactory {

    private final static byte peerKey = 0x01;
    private final static byte fileMetaKey = 0x02;

    public static KeyRange getOverlayRange(int overlayId) {
        if (overlayId == Integer.MAX_VALUE) {
            ByteBuffer startKey = ByteBuffer.allocate(4 + 1);
            startKey.putInt(overlayId);
            startKey.put(peerKey);
            return new KeyRange(KeyRange.Bound.CLOSED, new Key(startKey), Key.INF, KeyRange.Bound.OPEN);
        } else {
            ByteBuffer startKey = ByteBuffer.allocate(4 + 1);
            startKey.putInt(overlayId);
            startKey.put(peerKey);
            ByteBuffer endKey = ByteBuffer.allocate(4 + 1);
            endKey.putInt(overlayId + 1);
            endKey.put(peerKey);
            return new KeyRange(KeyRange.Bound.CLOSED, new Key(startKey), new Key(endKey), KeyRange.Bound.OPEN);
        }
    }

    public static Key getOverlayPeerKey(int overlayId, int nodeId) {
        return new Key(overlayId, nodeId);
    }

    public static Key getFileMetadataKey(int overlayId) {
        ByteBuffer byteKey = ByteBuffer.allocate(4 + 1);
        byteKey.putInt(overlayId);
        byteKey.put(fileMetaKey);
        return new Key(byteKey);
    }

    public static class KeyException extends Exception {

        public KeyException(String message) {
            super(message);
        }
    }
}
