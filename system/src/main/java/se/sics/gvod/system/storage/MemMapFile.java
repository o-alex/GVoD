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
 import org.javatuples.Triplet;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MemMapFile implements Storage {

    private static final int PIECE_LENGTH = 1024; //1KB

    private final int length;
    private final int lastPiece;

    private final MappedByteBuffer mbb;
    private final TreeMap<Integer, Integer> pieceRanges;

    private int readPos;
    private Triplet<Integer, Integer, Boolean> readRange; //largest contiguous range [a,b] such that a <= readPos <= b; boolean sets the range type - true [], false ()

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
        this.lastPiece = length / PIECE_LENGTH;
        this.readPos = 0;
        this.readRange = Triplet.with(0, lastPiece, true);

    }

    private MemMapFile(File file, int length) throws IOException {
        this.length = length;

        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        mbb = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, length);
        raf.close();

        this.pieceRanges = new TreeMap<Integer, Integer>();
        this.lastPiece = length / PIECE_LENGTH;
        this.readPos = 0;
        this.readRange = Triplet.with(0, 0, false);
    }

    @Override
    public synchronized void setReadPosition(int pieceId) throws OutOfBoundsException {
        if (pieceId > lastPiece) {
            throw new OutOfBoundsException();
        }
        readPos = pieceId;
        setReadRange();
    }
    
    private void setReadRange() {
        if(readRange.getValue0() <= readPos && readPos <= readRange.getValue1()) {
            return;
        }
        for(Integer startRPos : pieceRanges.keySet()) {
            if(startRPos > readPos) {
                break;
            }
            int endRPos = pieceRanges.get(startRPos);
            if(startRPos <= readPos && readPos <= endRPos) {
                readRange = Triplet.with(startRPos, endRPos, true);
                return;
            }
        }
        readRange = Triplet.with(readPos,readPos, false);
    }
    
    private void checkReadRange(int startRPos, int endRPos) {
        if(startRPos <= readRange.getValue0() && readRange.getValue1() < endRPos) {
            readRange = Triplet.with(startRPos, endRPos, true);
        }
    }

    @Override
    public synchronized int getReadPosition() {
        return readPos;
    }

    @Override
    public synchronized Set<Integer> nextPieces(int n, int startPos) throws OutOfBoundsException {
        if (startPos < readPos || startPos > lastPiece) {
            throw new OutOfBoundsException();
        }
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
        }
    }

    private boolean addToRanges(Integer pieceId) {
        
    }

    @Override
    public synchronized byte[] readPiece(int pieceId) throws OutOfBoundsException {
        if (pieceId < readPos || lastPiece < pieceId) {
            throw new OutOfBoundsException();
        }
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
