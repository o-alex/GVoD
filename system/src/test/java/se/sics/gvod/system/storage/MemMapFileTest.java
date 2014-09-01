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
import java.security.SecureRandom;
import java.util.Random;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MemMapFileTest {

//    @Test
    public void testSuccess() throws IOException {
        File uploadFile = File.createTempFile("memMapTest", "upload");
        System.out.println(uploadFile.getPath());
        File downloadFile = File.createTempFile("memMapTest", "download");
        String downloadPath = downloadFile.getPath();
        downloadFile.delete();
        System.out.println(downloadPath);
        
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(uploadFile.getPath())));
        for(int i = 0; i < 10000; i++) {
            writer.write("abc" + i + "\n");
        }
        writer.flush();
        writer.close();
        
        Storage upload = MemMapFile.getExistingFile(uploadFile.getPath());
        Storage download = MemMapFile.getEmptyFile(downloadPath, (int)uploadFile.length());
        
        while(!download.isComplete(0)) {
            Set<Integer> nextPieces = download.nextPieces(5, 0);
            for(Integer pieceId : nextPieces) {
//                System.out.println(pieceId);
                download.writePiece(pieceId, upload.readPiece(pieceId));
            }
        }
        
        Assert.assertEquals(uploadFile.length(), downloadFile.length());
    }
    
    @Test
    public void testSuccessRandomFailingPackages() throws IOException {
        Random rand = new Random(1234);
        int failingRate = 2; //1 in 5 failing rate
        
        File uploadFile = File.createTempFile("memMapTest", "upload");
        System.out.println(uploadFile.getPath());
        File downloadFile = File.createTempFile("memMapTest", "download");
        String downloadPath = downloadFile.getPath();
        downloadFile.delete();
        System.out.println(downloadPath);
        
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(uploadFile.getPath())));
        for(int i = 0; i < 10000; i++) {
            writer.write("abc" + i + "\n");
        }
        writer.flush();
        writer.close();
        
        Storage upload = MemMapFile.getExistingFile(uploadFile.getPath());
        Storage download = MemMapFile.getEmptyFile(downloadPath, (int)uploadFile.length());
        
        while(!download.isComplete(0)) {
            Set<Integer> nextPieces = download.nextPieces(5, 0);
            for(Integer pieceId : nextPieces) {
                if(rand.nextInt(failingRate) == 0) {
                    continue;
                }
                System.out.println(pieceId);
                download.writePiece(pieceId, upload.readPiece(pieceId));
            }
        }
        
        Assert.assertEquals(uploadFile.length(), downloadFile.length());
    }
}
