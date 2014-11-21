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

import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.network.GVoDNetFrameDecoder;
import se.sics.gvod.network.GVoDNetworkSettings;
import se.sics.gvod.simulation.core.P2pSimulator;
import se.sics.gvod.simulation.core.P2pSimulatorInit;
import se.sics.gvod.simulation.core.network.UniformRandomModel;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Launcher extends ComponentDefinition {

    {
        GVoDNetFrameDecoder.register();
        GVoDNetworkSettings.checkPreCond();
        GVoDNetworkSettings.registerSerializers();
        
        P2pSimulator.setSimulationPortType(VodExperiment.class);
        Component simulator = create(P2pSimulator.class,
                new P2pSimulatorInit(Main.scheduler, Main.scenario, new UniformRandomModel(1, 10)));
        Component simManager = create(SimManagerComp.class,
                new SimManagerInit());
        connect(simManager.getNegative(VodNetwork.class), simulator.getPositive(VodNetwork.class));
        connect(simManager.getNegative(Timer.class), simulator.getPositive(Timer.class));
        connect(simManager.getNegative(VodExperiment.class), simulator.getPositive(VodExperiment.class));
    }
}
