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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class RWMemMapFile implements Storage {

    private final String fileName;
    private final int fileLength;
    private final int pieceSize;

    private final MappedByteBuffer mbb;

    RWMemMapFile(File file, int length, int pieceSize) throws IOException {
        this.fileName = file.getName();
        this.fileLength = length;
        this.pieceSize = pieceSize;

        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        mbb = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, fileLength);
        raf.close();
    }
    
    @Override
    public int size() {
        return fileLength;
    }

    @Override
    public byte[] readPiece(int piecePos) {
        byte[] result;
        int lastPiece = fileLength / pieceSize;
        if (lastPiece == piecePos) {
            result = new byte[fileLength % pieceSize];
        } else {
            result = new byte[pieceSize];
        }
        int readStart = piecePos * pieceSize;
        mbb.position(readStart);
        mbb.get(result, 0, result.length);

        return result;
    }

    @Override
    public void writePiece(int piecePos, byte[] piece) {
        int writeStart = piecePos * pieceSize;
        mbb.position(writeStart);
        int writeBytes = (piece.length < pieceSize ? piece.length : pieceSize);
        mbb.put(piece, 0, writeBytes);
        mbb.force();
    }
    
    @Override
    public String toString() {
        return fileName;
    }
}
