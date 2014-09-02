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
import java.util.Set;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class RWMemMapFile implements Storage {

    private final int fileLength;
    
    private final MappedByteBuffer mbb;
    private final FilePieceTracker fpt;

    RWMemMapFile(File file, int length) throws IOException {
        this.fileLength = length;
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        mbb = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, fileLength);
        raf.close();

        this.fpt = new SimpleFPTracker(0, length / Storage.PIECE_LENGTH);
    }

    @Override
    public synchronized void setReadPosition(int pieceId) throws FilePieceTracker.OutOfBoundsException {
        fpt.setReadPos(pieceId);
    }

    @Override
    public synchronized int getReadPosition() {
        return fpt.getReadRange().getValue0();
    }

    @Override
    public synchronized Set<Integer> nextPieces(int n, int startPos) throws FilePieceTracker.OutOfBoundsException {
        return fpt.nextPieces(n, startPos);
    }

    @Override
    public synchronized void writePiece(int pieceId, byte[] piece) throws FilePieceTracker.OutOfBoundsException {
        fpt.addPiece(pieceId);
        int writeStart = pieceId * Storage.PIECE_LENGTH;
        mbb.position(writeStart);
        int writeBytes = (piece.length < Storage.PIECE_LENGTH ? piece.length : Storage.PIECE_LENGTH);
        mbb.put(piece, 0, writeBytes);
        mbb.force();
    }

    @Override
    public synchronized byte[] readPiece(int pieceId) throws FilePieceTracker.OutOfBoundsException, FilePieceTracker.PieceNotReadyException {
        if(!fpt.containsPiece(pieceId)) {
            throw new FilePieceTracker.PieceNotReadyException();
        }
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
        return fpt.isComplete();
    }
}
