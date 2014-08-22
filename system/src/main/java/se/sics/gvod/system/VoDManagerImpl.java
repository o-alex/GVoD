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

package se.sics.gvod.system;

import se.sics.gvod.manager.VoDManager;
import se.sics.gvod.system.vod.VoDPort;
import se.sics.gvod.system.vod.msg.UploadVideo;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Positive;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VoDManagerImpl extends ComponentDefinition implements VoDManager {

    Positive<VoDPort> vodPort = requires(VoDPort.class);
    
    public VoDManagerImpl() {
    }
    
    public VoDManager getInstance() {
        return this;
    }

    @Override
    public void uploadVideo(int overlayId) {
        trigger(new UploadVideo.Request(overlayId), vodPort);
    }

    @Override
    public void downloadVideo(int overlayId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
