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
package se.sics.gvod.croupierfake;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.client.BootstrapClientPort;
import se.sics.gvod.common.msg.ReqStatus;
import se.sics.gvod.common.msg.peerMngr.OverlaySample;
import se.sics.gvod.common.util.VodDescriptor;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(CroupierComp.class);

    private Negative<CroupierPort> myPort = provides(CroupierPort.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Positive<BootstrapClientPort> bootstrapClient = requires(BootstrapClientPort.class);
    
    private final int overlayId;
    private final VodAddress self;
    
    public CroupierComp(CroupierInit init) {
        log.info("create CroupierComp");

        this.overlayId = init.overlayId;
        this.self = init.self;
        
        subscribe(handleStart, control);
        subscribe(handleSampleTimeout, timer);
        subscribe(handleSampleResponse, bootstrapClient);
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(1000, 1000);
            spt.setTimeoutEvent(new SampleTimeout(spt));
            trigger(spt, timer);
        }
    };
    
    private Handler<SampleTimeout> handleSampleTimeout = new Handler<SampleTimeout>() {

        @Override
        public void handle(SampleTimeout event) {
            log.debug("sampling");
            trigger(new OverlaySample.Request(UUID.randomUUID(), overlayId), bootstrapClient);
        }
        
    };
    
    private Handler<OverlaySample.Response> handleSampleResponse = new Handler<OverlaySample.Response>() {

        @Override
        public void handle(OverlaySample.Response resp) {
            log.debug("received sample");
            
            if(resp.status != ReqStatus.SUCCESS) {
                log.warn("corrupted sample");
                return;
            }
            
            Map<VodAddress, VodDescriptor> sample = new HashMap<VodAddress, VodDescriptor>();
            for(Map.Entry<VodAddress, Integer> e : resp.overlaySample.entrySet()) {
                if(e.getKey().equals(self)) {
                    continue;
                }
                sample.put(e.getKey(), new VodDescriptor(e.getValue()));
            }
            
            trigger(new CroupierSample(sample), myPort);
        }
        
    };

    public static class CroupierInit extends Init<CroupierComp> {
        public final int overlayId;
        public final VodAddress self;
        
        public CroupierInit(int overlayId, VodAddress self) {
            this.overlayId = overlayId;
            this.self = self;
        }
    }
}
