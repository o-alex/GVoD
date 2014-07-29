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

import se.sics.gvod.simulation.cmd.system.StartBSCmd;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation1;
import se.sics.kompics.p2p.experiment.dsl.distribution.ConstantDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.Distribution;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ScenarioGen {

    static Operation1<StartBSCmd, Integer> startBootstrapServer
            = new Operation1<StartBSCmd, Integer>() {

                @Override
                public StartBSCmd generate(Integer id) {
                    System.out.println("A");
                    return new StartBSCmd(id);
                }
            };
    
    public static SimulationScenario simpleBoot(long seed) {
        SimulationScenario scen = new SimulationScenario() {
            {
                final Distribution<Integer> bootstrapIdDist = new ConstantDistribution<>(Integer.class, 0);
                StochasticProcess startBootstrapServerProc = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(0));
                        raise(2, startBootstrapServer, bootstrapIdDist);
                    }
                };

                startBootstrapServerProc.start();
                terminateAfterTerminationOf(10 * 1000, startBootstrapServerProc);
            }
        };

        scen.setSeed(seed);

        return scen;
    }

    
}
