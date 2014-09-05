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

package se.sics.gvod.system.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimplePieceTracker implements PieceTracker {
    private final Map<Integer, Integer> pieces;
    public final int size;
    
    public SimplePieceTracker(int size) {
        this.pieces = new HashMap<Integer, Integer>();
        this.size = size;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Set<Integer> nextPiecesNeeded(int n, int startPos) {
        TreeSet<Integer> result = new TreeSet<Integer>();
        int pieceToAdd = startPos;
        int lastPiece = size - 1;
        for(Map.Entry<Integer, Integer> pieceRange : pieces.entrySet()) {
            while(pieceToAdd < pieceRange.getKey()) {
                result.add(pieceToAdd);
                pieceToAdd++;
                if(result.size() == n) {
                    break;
                }
            }
            pieceToAdd = pieceRange.getValue() + 1;
        }
        while(result.size() < n && pieceToAdd <= lastPiece) {
            result.add(pieceToAdd);
            pieceToAdd++;
        }
        return result;
    }

    @Override
    public void addPiece(int piecePos) {
        pieces.put(piecePos, piecePos);
    }

    @Override
    public void addPieces(int startPos, int endPos) {
        pieces.put(startPos, endPos);
    }

    @Override
    public void removePieces(int startPos, int endPos) {
        
        for(Map.Entry<Integer, Integer> pieceRange : pieces.entrySet()) {
            while(pieceToAdd < pieceRange.getKey()) {
                result.add(pieceToAdd);
                pieceToAdd++;
                if(result.size() == n) {
                    break;
                }
            }
            pieceToAdd = pieceRange.getValue() + 1;
        }
        while(result.size() < n && pieceToAdd <= lastPiece) {
            result.add(pieceToAdd);
            pieceToAdd++;
        }
    }

    @Override
    public int contiguousForward(int startPos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int contiguousBackward(int startPos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
