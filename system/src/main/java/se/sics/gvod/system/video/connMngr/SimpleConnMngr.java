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
package se.sics.gvod.system.video.connMngr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.system.video.LocalVodDescriptor;
import se.sics.gvod.system.video.VodDescriptor;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimpleConnMngr implements ConnectionManager {
    private final ConnectionManagerConfig config;
    private final Map<Integer, LocalVodDescriptor> myDownloadConn;

    public SimpleConnMngr(ConnectionManagerConfig config) {
        this.config = config;
        this.myDownloadConn = new HashMap<Integer, LocalVodDescriptor>();
    }

    @Override
    public void updateConnections(Map<VodAddress, VodDescriptor> vodSamples) {
        for (Map.Entry<VodAddress, VodDescriptor> vodSample : vodSamples.entrySet()) {
            LocalVodDescriptor localVodDesc = myDownloadConn.get(vodSample.getKey().getId());
            if (localVodDesc == null) {
                localVodDesc = new LocalVodDescriptor(vodSample.getKey(), vodSample.getValue(), config.defaultMaxPipeline);
                myDownloadConn.put(vodSample.getKey().getId(), localVodDesc);
            } else {
                localVodDesc.updateVodDescriptor(vodSample.getValue());
            }
        }
    }

    /**
     * @param pieces
     * @return
     */
    @Override
    public Map<Integer, VodAddress> getPeersForPieces(Set<Integer> pieces) {
        Map<Integer, VodAddress> result = new HashMap<Integer, VodAddress>();

        LocalVodDescriptor candidate;
        for (Integer pieceId : pieces) {
            candidate = null;
            for (LocalVodDescriptor localVodDesc : myDownloadConn.values()) {
                if (!localVodDesc.canDownload()) {
                    continue;
                }
                if (localVodDesc.getVodDescriptor().downloadPos < pieceId) {
                    continue;
                }
                if (candidate == null) {
                    candidate = localVodDesc;
                } else if (candidate.getVodDescriptor().downloadPos > localVodDesc.getVodDescriptor().downloadPos) {
                    candidate = localVodDesc;
                }
            }
            if(candidate != null) {
                result.put(pieceId, candidate.peer);
                candidate.downloadBlock();
            }
        }
        return result;
    }

    @Override
    public void finishedPieceDownload(int peerId) {
        LocalVodDescriptor localVodDesc = myDownloadConn.get(peerId);
        if(localVodDesc == null) {
            return;
        }
        localVodDesc.finishedPieceDownload();
    }
}
