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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import se.sics.caracaldb.Key;
import se.sics.caracaldb.KeyRange;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CaracalKeyFactory {

    private static byte[] prefix;

    static {
        String schema = "gvod-v1";
        try {
            prefix = MessageDigest.getInstance("MD5").digest(schema.getBytes());
        } catch (NoSuchAlgorithmException ex) {
            System.exit(1);
            throw new RuntimeException(ex);
        }
    }

    private final static byte peerKey = 0x01;
    private final static byte fileMetaKey = 0x02;

    public static KeyRange getOverlayRange(int overlayId) {
        ByteBuffer startKey = ByteBuffer.allocate(sizeofOverlayKeyPrefix());
        startKey.put(prefix);
        startKey.putInt(overlayId);
        startKey.put(peerKey);
        ByteBuffer endKey = ByteBuffer.allocate(sizeofOverlayKeyPrefix());
        endKey.put(prefix);
        endKey.putInt(overlayId);
        endKey.put(fileMetaKey);
        return new KeyRange(KeyRange.Bound.CLOSED, new Key(startKey), new Key(endKey), KeyRange.Bound.OPEN);
    }

    public static Key getOverlayPeerKey(int overlayId, int nodeId) {
        ByteBuffer oKey = ByteBuffer.allocate(sizeofOverlayPeerKey());
        oKey.put(prefix);
        oKey.putInt(overlayId);
        oKey.put(peerKey);
        oKey.putInt(nodeId);
        return new Key(oKey);
    }

    public static Key getFileMetadataKey(int overlayId) {
        ByteBuffer byteKey = ByteBuffer.allocate(sizeofOverlayKeyPrefix());
        byteKey.put(prefix);
        byteKey.putInt(overlayId);
        byteKey.put(fileMetaKey);
        return new Key(byteKey);
    }

    private static int sizeofOverlayKeyPrefix() {
        int size = 0;
        size += prefix.length;
        size += Integer.SIZE/8; //overlayId;
        size += Byte.SIZE/8; //key type = overlay peer key
        return size;
    }

    private static int sizeofOverlayPeerKey() {
        int size = 0;
        size += sizeofOverlayKeyPrefix();
        size += Integer.SIZE/8; //nodeId;
        return size;
    }

    public static class KeyException extends Exception {

        public KeyException(String message) {
            super(message);
        }
    }
}
