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
import java.util.HashSet;
import java.util.Set;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class RMemMapFile implements Storage {

    private final int fileLength;

    private final MappedByteBuffer mbb;

    RMemMapFile(File file) throws IOException {
        this.fileLength = (int)file.length();
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        mbb = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
        raf.close();
    }

    @Override
    public void setReadPosition(int pieceId) throws FilePieceTracker.OutOfBoundsException {
        throw new UnsupportedOperationException("Read MemMapFile does not support read jump");
    }

    @Override
    public int getReadPosition() {
        return 0;
    }

    @Override
    public Set<Integer> nextPieces(int n, int startPos) throws FilePieceTracker.OutOfBoundsException {
        return new HashSet<Integer>();
    }

    @Override
    public void writePiece(int pieceId, byte[] piece) throws FilePieceTracker.OutOfBoundsException {
        throw new UnsupportedOperationException("Read MemMapFile does not support write");
    }

    @Override
    public byte[] readPiece(int pieceId) throws FilePieceTracker.PieceNotReadyException, FilePieceTracker.OutOfBoundsException {
        byte[] result;
        int lastPiece = fileLength / Storage.PIECE_LENGTH;
        if (lastPiece == pieceId) {
            result = new byte[fileLength % Storage.PIECE_LENGTH];
        } else {
            result = new byte[Storage.PIECE_LENGTH];
        }
        int readStart = pieceId * Storage.PIECE_LENGTH;
        mbb.position(readStart);
        mbb.get(result, 0, result.length);

        return result;
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
