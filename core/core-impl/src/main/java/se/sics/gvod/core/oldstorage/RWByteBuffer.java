///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//
//package se.sics.gvod.core.oldstorage;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.Unpooled;
//import java.io.IOException;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class RWByteBuffer implements OldStorage {
//    private final int bufLength;
//    private final int pieceSize;
//    private final ByteBuf buf;
//    private final int lastPiece;
//
//    /**
//     * @param pieceSize a piece is [0,pieceSize)
//     * @param nrPieces
//     * @throws IOException 
//     */
//    RWByteBuffer(int bufLength, int pieceSize) throws IOException {
//        this.bufLength = bufLength;
//        this.pieceSize = pieceSize;
//        this.buf = Unpooled.wrappedBuffer(new byte[bufLength]);
//        this.lastPiece = (bufLength % pieceSize == 0) ? (int) (bufLength / pieceSize) - 1 : (int) (bufLength / pieceSize);
//    }
//    
//    @Override
//    public int nrPieces() {
//        return lastPiece + 1;
//    }
//
//    @Override
//    public byte[] read(long startPos, int readLength) {
//        if(startPos > bufLength) {
//            return new byte[0];
//        }
//        if(startPos + readLength > bufLength) {
//            readLength =  bufLength - (int)startPos;
//        }
//        byte[] result = new byte[readLength];
//        buf.readerIndex((int)startPos);
//        buf.readBytes(result);
//        return result;
//    }
//    @Override
//    public byte[] readPiece(int piecePos) {
//        int readLength;
//        if (lastPiece == piecePos) {
//            if (bufLength % pieceSize == 0) {
//                readLength = pieceSize;
//            } else {
//                readLength = bufLength % pieceSize;
//            }
//        } else {
//            readLength = pieceSize;
//        }
//        return read(piecePos*pieceSize, readLength);
//    }
//
//    @Override
//    public void writePiece(int piecePos, byte[] piece) {
//        int writeStart = piecePos * pieceSize;
//        buf.writerIndex(writeStart);
//        long rest = bufLength - writeStart;
//        int writeBytes = (piece.length < rest ? piece.length : (int)rest);
//        buf.writeBytes(piece, 0, writeBytes);
//    }
//    
//    @Override
//    public String toString() {
//        return "RWByteBuffer";
//    }
//    
//}
