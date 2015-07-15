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
package se.sics.gvod.core;

import se.sics.gvod.common.utility.GVoDHostConfig;
import se.sics.gvod.common.utility.GVoDReferenceConfig;
import se.sics.gvod.core.downloadMngr.DownloadMngrConfig;
import se.sics.gvod.core.connMngr.ConnMngrConfig;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VoDConfig {

    private GVoDHostConfig hostConfig;
    private GVoDReferenceConfig referenceConfig;

    public VoDConfig(GVoDHostConfig hostConfig, GVoDReferenceConfig referenceConfig) {
        this.hostConfig = hostConfig;
        this.referenceConfig = referenceConfig;
    }

    public DownloadMngrConfig.Builder getDownloadMngrConfig(int overlayId) {
        return new DownloadMngrConfig.Builder(hostConfig.getConfig(), hostConfig.getSelf(), overlayId);
    }

    public ConnMngrConfig.Builder getConnMngrConfig(int overlayId) {
        return new ConnMngrConfig.Builder(hostConfig.getConfig(), hostConfig.getSelf(), overlayId);
    }
    
    public DecoratedAddress getSelf() {
        return hostConfig.getSelf();
    }
    
    public String getVideoLibrary() {
        return hostConfig.getVideoLibrary();
    }
    
    public int getPiecesPerBlock() {
        return referenceConfig.getPiecesPerBlock();
    }
    
    public int getPieceSize() {
        return referenceConfig.getPieceSize();
    }
    
    public String getHashAlg() {
        return referenceConfig.getHashAlg();
    }
}