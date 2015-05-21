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
package se.sics.gvod.core.libraryMngr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Test;
import se.sics.gvod.core.util.FileStatus;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class LibraryMngrTest {

    @Test
    public void test1() throws IOException {
        String libPath = "./src/test/resources/library1";

        //*********setup *******************************************************
        File videoData1 = new File(libPath + File.separator + "video1.mp4");
        videoData1.createNewFile();
        File videoData2 = new File(libPath + File.separator + "video2.mp4");
        videoData2.createNewFile();

        File statusFile = new File(libPath + File.separator + "status.file");
        statusFile.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(libPath + File.separator + "status.file"));
        bw.write("");
        bw.close();
        //**********************************************************************

        LibraryMngr libMngr;
        Map<String, Pair<FileStatus, Integer>> libFiles;
        
        libMngr = new LibraryMngr(libPath);
        libMngr.loadLibrary();
        libFiles = libMngr.getLibrary();
        
        Assert.assertEquals(2, libFiles.size());
        Assert.assertEquals(FileStatus.NONE, libFiles.get("video1.mp4").getValue0());
        Assert.assertEquals(FileStatus.NONE, libFiles.get("video2.mp4").getValue0());

        File videoHash2 = new File(libPath + File.separator + "video2.hash");
        videoHash2.createNewFile();
        Assert.assertTrue(libMngr.pendingDownload("video3.mp4"));
        Assert.assertTrue(libMngr.startDownload("video3.mp4", 10));
        Assert.assertTrue(libMngr.pendingUpload("video2.mp4"));
        Assert.assertTrue(libMngr.upload("video2.mp4", 11));
        
        Assert.assertEquals(3, libFiles.size());
        Assert.assertEquals(FileStatus.NONE, libFiles.get("video1.mp4").getValue0());
        Assert.assertEquals(FileStatus.UPLOADING, libFiles.get("video2.mp4").getValue0());
        Assert.assertEquals(FileStatus.DOWNLOADING, libFiles.get("video3.mp4").getValue0());
        
        libMngr = new LibraryMngr(libPath);
        libMngr.loadLibrary();
        libFiles = libMngr.getLibrary();

        Assert.assertEquals(2, libFiles.size());
        Assert.assertEquals(FileStatus.NONE, libFiles.get("video1.mp4").getValue0());
        Assert.assertEquals(FileStatus.UPLOADING, libFiles.get("video2.mp4").getValue0());
        Assert.assertEquals(new Integer(11), libFiles.get("video2.mp4").getValue1());
    }

    @Test
    public void test2() throws IOException {
        String libPath = "./src/test/resources/library2";

        //*********setup *******************************************************
        File videoData1 = new File(libPath + File.separator + "video1.mp4");
        videoData1.createNewFile();
        File videoData2 = new File(libPath + File.separator + "video2.mp4");
        videoData2.createNewFile();
        File videoData3 = new File(libPath + File.separator + "video3.mp4");
        videoData3.createNewFile();
        File videoHash3 = new File(libPath + File.separator + "video3.hash");
        videoHash3.createNewFile();

        File statusFile = new File(libPath + File.separator + "status.file");
        statusFile.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(libPath + File.separator + "status.file"));
        bw.write("video1.mp4:UPLOADING:10\n");
        bw.write("video2.mp4:DOWNLOADING:11\n");
        bw.write("video3.mp4:UPLOADING:12\n");
        bw.close();
        //**********************************************************************

        LibraryMngr libMngr = new LibraryMngr(libPath);
        libMngr.loadLibrary();
        Map<String, Pair<FileStatus, Integer>> libFiles;

        libFiles = libMngr.getLibrary();
        Assert.assertEquals(3, libFiles.size());
        Assert.assertEquals(FileStatus.NONE, libFiles.get("video1.mp4").getValue0());
        Assert.assertEquals(FileStatus.NONE, libFiles.get("video2.mp4").getValue0());
        Assert.assertEquals(FileStatus.UPLOADING, libFiles.get("video3.mp4").getValue0());
        Assert.assertEquals(new Integer(12), libFiles.get("video3.mp4").getValue1());
    }
}
