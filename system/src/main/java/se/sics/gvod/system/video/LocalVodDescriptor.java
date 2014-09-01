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

package se.sics.gvod.system.video;

import se.sics.gvod.net.VodAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class LocalVodDescriptor {
    public final VodAddress peer;
    private VodDescriptor vodDesc;
    private int age;
    private int pipelineSize;
    private int maxPipeline;
    
    public LocalVodDescriptor(VodAddress peer, VodDescriptor vodDesc, int maxPipeline) {
        this.peer = peer;
        this.vodDesc = vodDesc;
        this.age = 0;
        this.maxPipeline = maxPipeline;
        this.pipelineSize = 0;
    }
    
    public void updateVodDescriptor(VodDescriptor vodDesc) {
        this.vodDesc = vodDesc;
        this.age = 0;
    }
    
    public VodDescriptor getVodDescriptor() {
        return vodDesc;
    }
    
    public void incrementAge() {
        age++;
    }
    
    public int getAge() {
        return age;
    }
    
    public boolean canDownload() {
        return pipelineSize <= maxPipeline;
    }
    
    public void downloadBlock() {
        pipelineSize++;
    }
    
    public void finishedPieceDownload() {
        pipelineSize--;
    }
}
