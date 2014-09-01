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
package se.sics.gvod.system.storage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MemMapFile implements Storage {

    private static final int PIECE_LENGTH = 1024; //1KB

    private final int length;

    private final MappedByteBuffer mbb;
    private final TreeMap<Integer, Integer> pieceRanges;

    public static MemMapFile getExistingFile(String pathname) throws IOException {
        File file = new File(pathname);
        return new MemMapFile(file);
    }

    public static MemMapFile getEmptyFile(String pathname, int length) throws IOException {
        File file = new File(pathname);
        if (!file.createNewFile()) {
            throw new IOException("Could not create file " + pathname);
        }
        return new MemMapFile(file, length);
    }

    private MemMapFile(File file) throws IOException {
        this.length = (int) file.length();

        RandomAccessFile raf = new RandomAccessFile(file, "r");
        mbb = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, length);
        //load whole file in memory
        mbb.load();
        raf.close();

        this.pieceRanges = new TreeMap<Integer, Integer>();
    }

    private MemMapFile(File file, int length) throws IOException {
        this.length = length;

        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        mbb = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, length);
        raf.close();

        this.pieceRanges = new TreeMap<Integer, Integer>();
    }

    @Override
    public synchronized Set<Integer> nextPieces(int n, int startPos) {
        Set<Integer> result = new TreeSet<Integer>();
        int nextPos = startPos;

        Iterator<Map.Entry<Integer, Integer>> it = pieceRanges.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> pieceRange = it.next();
            if (nextPos >= pieceRange.getValue()) {
                continue;
            } else {
                while (nextPos < pieceRange.getKey()) {
                    result.add(nextPos);
                    if (result.size() == n) {
                        break;
                    }
                    nextPos++;
                }
                nextPos = pieceRange.getValue() + 1;
            }
        }
        int lastPiece = length / PIECE_LENGTH;
        while (result.size() < n && nextPos <= lastPiece) {
            result.add(nextPos);
            nextPos++;
        }
        return result;
    }

    @Override
    public synchronized void writePiece(int pieceId, byte[] piece) {
        if (addToRanges(pieceId)) {
            int writeStart = pieceId * PIECE_LENGTH;
            mbb.position(writeStart);
            int writeBytes = (piece.length < PIECE_LENGTH ? piece.length : PIECE_LENGTH);
            mbb.put(piece, 0, writeBytes);
            mbb.force();
            System.out.println(pieceRanges);
        }
    }

    private boolean addToRanges(Integer pieceId) {
        Iterator<Integer> it = pieceRanges.keySet().iterator();
        Integer key = null;
        Integer nextKey = null;
        nextKey = (it.hasNext() ? it.next() : null);
        key = nextKey;
        while (key != null) {
            nextKey = (it.hasNext() ? it.next() : null);
            if (key <= pieceId && pieceId <= pieceRanges.get(key)) {
                //piece exists already
                return false;
            }
            if (pieceRanges.get(key) == pieceId - 1) {
                if (nextKey != null && nextKey == pieceId + 1) {
                    //pieceRange = [a, pieceId-1]
                    //nextPieceRange = [pieceId + 1, b]
                    //remove old intervals and add new [a,b] interval
                    pieceRanges.put(key, pieceRanges.get(nextKey));
                    pieceRanges.remove(nextKey);
                    return true;
                } else {
                    //pieceRange = [a, pieceId-1] change to [a, pieceId]
                    pieceRanges.put(key, pieceId);
                    return true;
                }
            } else if (key == pieceId + 1) {
                //pieceRange = [pieceId + 1, b] chance to [pieceId, b]
                pieceRanges.put(pieceId, pieceRanges.get(key));
                pieceRanges.remove(key);
                return true;
            }
            key = nextKey;
        }
        pieceRanges.put(pieceId, pieceId);
        return true;
    }

    @Override
    public synchronized byte[] readPiece(int pieceId) {
        byte[] result;
        int lastPiece = length / PIECE_LENGTH;
        if (lastPiece == pieceId) {
            result = new byte[length % PIECE_LENGTH];
        } else {
            result = new byte[PIECE_LENGTH];
        }
        int readStart = pieceId * PIECE_LENGTH;
        mbb.position(readStart);
        mbb.get(result, 0, result.length);

        return result;
    }

    @Override
    public boolean isComplete(int readPos) {
        Map.Entry<Integer, Integer> lastPieceRange = pieceRanges.lastEntry();
        if (lastPieceRange == null) {
            return false;
        }
        return (lastPieceRange.getKey() <= readPos && readPos <= lastPieceRange.getValue() && lastPieceRange.getValue() == length / PIECE_LENGTH);
    }
}
