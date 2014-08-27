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
package se.sics.gvod.manager;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Library {

    public final String path;
    public final Map<String, FileDescriptor> view;
    private final Map<String, FileDescriptor> files;

    public Library(String path) throws ManagerExceptions.LibraryException {
        this.path = path;
        this.files = new TreeMap<String, FileDescriptor>();
        this.view = Collections.unmodifiableMap(files);

        loadLibrary();
    }

    private void loadLibrary() throws ManagerExceptions.LibraryException {
        
        File dir = new File(path);
        if (!dir.isDirectory()) {
            throw new ManagerExceptions.LibraryException("bad library path");
        }
        
        FileFilter mp4Filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (!file.isFile()) {
                    return false;
                }
                return file.getName().endsWith(".mp4");
            }
        };
        
        for (File video : dir.listFiles(mp4Filter)) {
            files.put(video.getName(), new FileDescriptor(video.getAbsolutePath(), video.getName(), video.length()/1024, FileDescriptor.Status.NONE));
        }
    }
}
