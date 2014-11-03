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
//package se.sics.gvod.manager.gson;
//
//import com.google.gson.TypeAdapter;
//import com.google.gson.stream.JsonReader;
//import com.google.gson.stream.JsonWriter;
//import java.io.IOException;
//import se.sics.gvod.manager.FileDescriptor;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class FileDescriptorAdapter extends TypeAdapter<FileDescriptor> {
//
//    @Override
//    public void write(JsonWriter writer, FileDescriptor file) throws IOException {
//        writer.beginObject();
//        writer.name("path").value(file.path);
//        writer.name("name").value(file.name);
//        writer.name("size").value(file.size);
//        writer.name("status").value(file.status.toString());
//        writer.endObject();
//    }
//
//    @Override
//    public FileDescriptor read(JsonReader reader) throws IOException {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//    
//}
