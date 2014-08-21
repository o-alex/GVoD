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
package se.sics.gvod.manager.gson;

import com.google.common.io.Files;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.Test;
import se.sics.gvod.manager.Library;
import se.sics.gvod.manager.ManagerExceptions;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GsonTest {

    @Test
    public void testLibrary() throws IOException, ManagerExceptions.LibraryException {
        File dir = Files.createTempDir();
        File f1 = File.createTempFile("file1", ".mp4", dir);
        File f2 = File.createTempFile("file2", ".mp4", dir);
        File f3 = File.createTempFile("file3", ".exe", dir);
        File f4 = File.createTempFile("file4", ".mp4", dir);
        
        Library l = new Library(dir.getAbsolutePath());
        Assert.assertEquals(3, l.view.size());
        
        Gson gson = GsonHelper.getGson();
        String gsonLib = gson.toJson(l);
//        System.out.println(gsonLib);
    }
}
