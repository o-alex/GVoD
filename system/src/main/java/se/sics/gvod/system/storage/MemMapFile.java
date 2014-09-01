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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.javatuples.Pair;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MemMapFile implements Storage {
    private static final int PIECE_LENGTH = 1024;

    private final long length;

    private final MappedByteBuffer mbb;
    private final List<Pair<Long,Long>> pieceRanges;
    private final long readPos;

    public static MemMapFile getExistingFile(String pathname) throws IOException {
        File file = new File(pathname);
        return new MemMapFile(file);
    }

    public static MemMapFile getEmptyFile(String pathname, long length) throws IOException {
        File file = new File(pathname);
        if (!file.createNewFile()) {
            throw new IOException("Could not create file " + pathname);
        }
        return new MemMapFile(file, length);
    }

    private MemMapFile(File file) throws IOException {
        this.length = file.length();
        
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        mbb = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, length);
        //load whole file in memory
        mbb.load();
        raf.close();
        
        this.pieceRanges = new ArrayList<Pair<Long,Long>>();
        this.readPos = 0;
    }
    
    private MemMapFile(File file, long length) throws IOException {
        this.length = length;
        
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        mbb = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, length);
        raf.close();
        
        this.pieceRanges = new ArrayList<Pair<Long,Long>>();
        this.readPos = 0;
    }
    
    @Override
    public synchronized Set<Integer> nextPieces(int n) {
        Set<Integer> result = new TreeSet<Integer>();
        for(Pair<Long, Long> pieceRange : pieceRanges) {
            if(readPos > pieceRange.getValue1()) {
                continue;
            }
            long startPos = (readPos < pieceRange.getValue0() ? readPos : pieceRange.getValue0());
            for(long i = startPos; i < pieceRange.getValue1(); i++) {
                //here
            }
            
        }
        return result;
    }

    @Override
    public synchronized void writePiece(int pieceId, byte[] piece) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public synchronized byte[] readPiece(int pieceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public synchronized int getLastCompletePieceId() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
