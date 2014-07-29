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

package se.sics.gvod.simulation;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.simulation.cmd.system.StartBSCmd;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimManagerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(SimManagerComp.class);

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Positive<VodExperiment> experiment = requires(VodExperiment.class);

    private HashMap<Integer, Component> systemComp;
    
    public SimManagerComp(SimManagerInit init) {
        log.debug("init");
        systemComp = new HashMap<Integer, Component>();
        
        subscribe(handleStartBS, experiment);
    }
    
    public Handler<StartBSCmd> handleStartBS = new Handler<StartBSCmd>() {

        @Override
        public void handle(StartBSCmd event) {
            log.info("starting bootstrap server");
            try {
                se.sics.gvod.bootstrap.server.HostConfiguration bsConfig = new se.sics.gvod.bootstrap.server.HostConfiguration.Builder().
                        loadConfig("bootstrap.conf").setId(event.id).finalise();
                
                Component bs = create(se.sics.gvod.bootstrap.server.HostManagerComp.class, 
                        new se.sics.gvod.bootstrap.server.HostManagerInit(bsConfig));
                systemComp.put(event.id, bs);
                
                connect(bs.getNegative(Network.class), network);
                connect(bs.getNegative(Timer.class), timer);
                
                trigger(Start.event, bs.control());
            } catch (se.sics.gvod.bootstrap.server.HostConfiguration.ConfigException ex) {
                log.error("error loading bootstrap server configuration");
                throw new RuntimeException(ex);
            }
        }
    };
} 
