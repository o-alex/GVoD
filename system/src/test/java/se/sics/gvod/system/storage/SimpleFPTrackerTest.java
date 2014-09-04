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

import se.sics.gvod.system.video.storage.FilePieceTracker;
import se.sics.gvod.system.video.storage.SimpleFPTracker;
import java.util.Set;
import junit.framework.Assert;
import org.javatuples.Triplet;
import org.junit.Test;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimpleFPTrackerTest {

    @Test
    public void testNoJump() throws FilePieceTracker.OutOfBoundsException {
        Set<Integer> pieces;
        FilePieceTracker fpt = new SimpleFPTracker(0, 10);

        pieces = fpt.nextPieces(3, 0);
        Assert.assertEquals(3, pieces.size());
        Assert.assertTrue(pieces.contains(0));
        Assert.assertTrue(pieces.contains(1));
        Assert.assertTrue(pieces.contains(2));
        
        Assert.assertFalse(fpt.containsPiece(0));
        fpt.addPiece(0);
        Assert.assertTrue(fpt.containsPiece(0));
        Assert.assertFalse(fpt.containsPiece(1));
        fpt.addPiece(2);
        fpt.addPiece(5);
        
        pieces = fpt.nextPieces(3, 0);
        Assert.assertEquals(3, pieces.size());
        Assert.assertTrue(pieces.contains(1));
        Assert.assertTrue(pieces.contains(3));
        Assert.assertTrue(pieces.contains(4));
        Assert.assertFalse(fpt.isComplete());
        Assert.assertEquals(Triplet.with(0,0,true), fpt.getReadRange());
        
        fpt.addPiece(1);
        fpt.addPiece(3);
        fpt.addPiece(4);
        fpt.addPiece(6);
        fpt.addPiece(7);
        fpt.addPiece(8);
        fpt.addPiece(9);
        
        pieces = fpt.nextPieces(3, 0);
        Assert.assertEquals(1, pieces.size());
        Assert.assertTrue(pieces.contains(10));
        Assert.assertFalse(fpt.isComplete());
        Assert.assertEquals(Triplet.with(0,9,true), fpt.getReadRange());

        fpt.addPiece(10);
        pieces = fpt.nextPieces(3, 0);
        Assert.assertEquals(0, pieces.size());
        Assert.assertTrue(fpt.isComplete());
        Assert.assertEquals(Triplet.with(0,10,true), fpt.getReadRange());
    }
    
    @Test
    public void testJump() throws FilePieceTracker.OutOfBoundsException {
        Set<Integer> pieces;
        FilePieceTracker fpt = new SimpleFPTracker(0, 10);

        pieces = fpt.nextPieces(3, 0);
        Assert.assertEquals(3, pieces.size());
        Assert.assertTrue(pieces.contains(0));
        Assert.assertTrue(pieces.contains(1));
        Assert.assertTrue(pieces.contains(2));
        
        fpt.setReadPos(8);
        fpt.addPiece(0);
        fpt.addPiece(2);
        fpt.addPiece(5);
        
        pieces = fpt.nextPieces(3, 0);
        Assert.assertEquals(3, pieces.size());
        Assert.assertTrue(pieces.contains(8));
        Assert.assertTrue(pieces.contains(9));
        Assert.assertTrue(pieces.contains(10));
        Assert.assertFalse(fpt.isComplete());
        Assert.assertEquals(Triplet.with(8,8,false), fpt.getReadRange());
        
        fpt.addPiece(8);
        fpt.addPiece(9);
        
        pieces = fpt.nextPieces(3, 0);
        Assert.assertEquals(1, pieces.size());
        Assert.assertTrue(pieces.contains(10));
        Assert.assertFalse(fpt.isComplete());
        Assert.assertEquals(Triplet.with(8,9,true), fpt.getReadRange());

        fpt.addPiece(10);
        pieces = fpt.nextPieces(3, 0);
        Assert.assertEquals(0, pieces.size());
        Assert.assertTrue(fpt.isComplete());
        Assert.assertEquals(Triplet.with(8,10,true), fpt.getReadRange());
    }
}
