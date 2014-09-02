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

import java.util.Set;
import org.javatuples.Triplet;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public interface FilePieceTracker {
    public void setReadPos(int pieceId) throws OutOfBoundsException;
    public Triplet<Integer, Integer, Boolean> getReadRange();
    public void addPiece(int pieceId) throws OutOfBoundsException;
    public Set<Integer> nextPieces(int n, int startPos);
    
    public static class FPTrackerException extends Exception {
        public FPTrackerException(String message) {
            super(message);
        }
        public FPTrackerException() {
            super();
        }
    }
    
    public static class PieceNotReadyException extends FPTrackerException {
        public PieceNotReadyException(String message) {
            super(message);
        }
    }
    
    public static class OutOfBoundsException extends FPTrackerException {
        public OutOfBoundsException() {
            super();
        }
    }
}
