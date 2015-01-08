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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Random;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import se.sics.gvod.core.storage.CompletePieceTracker;
import se.sics.gvod.core.storage.FileMngr;
import se.sics.gvod.core.storage.PieceTracker;
import se.sics.gvod.core.storage.SimpleFileMngr;
import se.sics.gvod.core.storage.SimplePieceTracker;
import se.sics.gvod.core.storage.Storage;
import se.sics.gvod.core.storage.StorageFactory;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class FileTest {

    //TODO Alex testEquality of MemMapFiles.. will be expensive - check temp File
    
    private String uploadPath;
    private String downloadPath;
    private int fileLength;
    private int pieceSize;
    
    @Test
    public void testSuccess() throws IOException {
        fileLength = 10000;
        pieceSize = 1024;
        prepareFiles();
        System.out.println(uploadPath);
        System.out.println(downloadPath);
        
        Storage upload = StorageFactory.getExistingFile(uploadPath, pieceSize);
        Storage download = StorageFactory.getEmptyFile(downloadPath, fileLength, pieceSize);
        FileMngr uploadMngr = new SimpleFileMngr(upload, new CompletePieceTracker(fileLength/pieceSize + 1));
        FileMngr downloadMngr = new SimpleFileMngr(download, new SimplePieceTracker(fileLength/pieceSize + 1));
        while(!downloadMngr.isComplete()) {
            Set<Integer> nextPieces = downloadMngr.nextPiecesNeeded(5, 0);
            for(Integer pieceId : nextPieces) {
//                System.out.println(pieceId);
                downloadMngr.writePiece(pieceId, upload.readPiece(pieceId));
            }
        }
        
        Assert.assertTrue(downloadMngr.isComplete());
    }
    
    @Test
    public void testSuccessRandomFailingPackages() throws IOException {
        pieceSize = 1024;
        prepareFiles();
        System.out.println(uploadPath);
        System.out.println(downloadPath);

        Random rand = new Random(1234);
        int failingRate = 2; //1 in 2 failing rate
        
        Storage upload = StorageFactory.getExistingFile(uploadPath, pieceSize);
        Storage download = StorageFactory.getEmptyFile(downloadPath, fileLength, pieceSize);
        FileMngr uploadMngr = new SimpleFileMngr(upload, new CompletePieceTracker(fileLength/pieceSize + 1));
        FileMngr downloadMngr = new SimpleFileMngr(download, new SimplePieceTracker(fileLength/pieceSize + 1));
        while(!downloadMngr.isComplete()) {
            Set<Integer> nextPieces = downloadMngr.nextPiecesNeeded(5, 0);
            for(Integer pieceId : nextPieces) {
                if(rand.nextInt(failingRate) == 0) {
                    continue;
                }
//                System.out.println(pieceId);
                downloadMngr.writePiece(pieceId, upload.readPiece(pieceId));
            }
        }
        
        Assert.assertTrue(downloadMngr.isComplete());
    }
        
    private void prepareFiles() throws IOException {
        File uploadFile = File.createTempFile("memMapTest", "upload");
        uploadPath = uploadFile.getPath();
        File downloadFile = File.createTempFile("memMapTest", "download");
        downloadPath = downloadFile.getPath();
        downloadFile.delete();
        
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(uploadFile.getPath())));
        for(int i = 0; i < 10000; i++) {
            writer.write("abc" + i + "\n");
        }
        writer.flush();
        writer.close();
        fileLength = (int)uploadFile.length();
    }
}
