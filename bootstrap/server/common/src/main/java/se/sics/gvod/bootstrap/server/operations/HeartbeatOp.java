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
package se.sics.gvod.bootstrap.server.operations;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import se.sics.gvod.bootstrap.server.PeerOpManager;
import se.sics.gvod.bootstrap.server.operations.util.Helper;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerMsg;
import se.sics.gvod.bootstrap.server.peermanager.msg.PMJoinOverlay;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.Heartbeat;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HeartbeatOp implements Operation {

    private final PeerOpManager opMngr;
    private final Heartbeat.OneWay oneWay;
    private final DecoratedAddress src;

    public final Set<UUID> pendingJoins;

    public HeartbeatOp(PeerOpManager opMngr, Heartbeat.OneWay oneWay, DecoratedAddress src) {
        this.opMngr = opMngr;
        this.src = src;
        this.oneWay = oneWay;
        this.pendingJoins = new HashSet<UUID>();
    }

    @Override
    public UUID getId() {
        return oneWay.id;
    }

    @Override
    public void start() {
        byte[] bytesBootOverlay = Helper.serializeOverlayData(src, 0);
        PMJoinOverlay.Request joinSystem = new PMJoinOverlay.Request(UUID.randomUUID(), 0, src.getId(), bytesBootOverlay);
        opMngr.sendPeerManagerReq(getId(), joinSystem);
        pendingJoins.add(joinSystem.id);

        for (Map.Entry<Integer, Integer> e : oneWay.overlayUtilities.entrySet()) {
            byte[] bytesOverlay = Helper.serializeOverlayData(src, e.getValue());
            PMJoinOverlay.Request joinOverlay = new PMJoinOverlay.Request(UUID.randomUUID(), e.getKey(), src.getId(), bytesOverlay);
            opMngr.sendPeerManagerReq(getId(), joinOverlay);
            pendingJoins.add(joinOverlay.id);
        }
    }

    @Override
    public void handle(PeerManagerMsg.Response peerResp) {
        if (peerResp instanceof PMJoinOverlay.Response) {
            PMJoinOverlay.Response joinResp = (PMJoinOverlay.Response) peerResp;
            if (joinResp.status == ReqStatus.SUCCESS) {
                pendingJoins.remove(joinResp.id);
                if (pendingJoins.isEmpty()) {
                    opMngr.finish(getId());
                }
            } else {
                opMngr.finish(getId());
            }
        } else {
            System.exit(1);
            throw new RuntimeException("wrong phase");
        }
    }
}
