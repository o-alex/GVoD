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
package se.sics.gvod.system.video.storage;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.javatuples.Triplet;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimpleFPTracker implements FilePieceTracker {

    private final TreeSet<Integer> previousPieces;
    private final TreeSet<Integer> readPieces;
    private final int lastPos;
    private int readPos;

    public SimpleFPTracker(int readPos, int finalPiece) {
        this.previousPieces = new TreeSet<Integer>();
        this.readPieces = new TreeSet<Integer>();
        this.readPos = readPos;
        this.lastPos = finalPiece;
    }

    @Override
    public void setReadPos(int pieceId) throws OutOfBoundsException {
        if (pieceId < readPos || lastPos < pieceId) {
            throw new OutOfBoundsException();
        }
        this.readPos = pieceId;
        
        TreeSet<Integer> move = new TreeSet<Integer>(readPieces.headSet(pieceId, true));
        previousPieces.addAll(move);
        readPieces.removeAll(move);
    }

    @Override
    public Triplet<Integer, Integer, Boolean> getReadRange() {
        if(readPieces.isEmpty() || readPieces.first() != readPos) {
           return Triplet.with(readPos, readPos, false); 
        }
        Iterator<Integer> it = readPieces.iterator();
        int previousPiece = readPieces.first();
        int currentPiece;
        while(it.hasNext()) {
            currentPiece = it.next();
            if(currentPiece == previousPiece) {
                previousPiece++;
            } else {
                break;
            }
        }
        return Triplet.with(readPos, previousPiece-1, true);
    }

    @Override
    public void addPiece(int pieceId) throws OutOfBoundsException {
        if(pieceId < readPos) {
            previousPieces.add(pieceId);
        } else {
            readPieces.add(pieceId);
        }
    }

    @Override
    public Set<Integer> nextPieces(int n, int startPos) {
        TreeSet<Integer> result = new TreeSet<Integer>();
        int pieceToAdd = readPos;
        for(Integer piece : readPieces) {
            while(pieceToAdd < piece) {
                result.add(pieceToAdd);
                pieceToAdd++;
                if(result.size() == n) {
                    break;
                }
            }
            pieceToAdd++;
        }
        while(result.size() < n && pieceToAdd <= lastPos) {
            result.add(pieceToAdd);
            pieceToAdd++;
        }
        return result;
    }

    @Override
    public boolean isComplete() {
        return lastPos - readPos + 1 == readPieces.size();
    }

    @Override
    public boolean containsPiece(int pieceId) {
        return previousPieces.contains(pieceId) || readPieces.contains(pieceId);
    }
}
