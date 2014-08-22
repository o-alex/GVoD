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

import java.util.Random;
import se.sics.gvod.simulation.cmd.operations.UploadVideoCmd;
import se.sics.gvod.simulation.cmd.system.StartBSCmd;
import se.sics.gvod.simulation.cmd.system.StartVodPeerCmd;
import se.sics.gvod.simulation.cmd.system.StopBSCmd;
import se.sics.gvod.simulation.cmd.system.StopVodPeerCmd;
import se.sics.gvod.simulation.util.IntegerUniformDistribution;
import se.sics.gvod.system.vod.msg.UploadVideo;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation1;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation2;
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
                    return new StartBSCmd(id);
                }
            };

    static Operation1<StopBSCmd, Integer> stopBootstrapServer
            = new Operation1<StopBSCmd, Integer>() {

                @Override
                public StopBSCmd generate(Integer id) {
                    return new StopBSCmd(id);
                }
            };
    static Operation1<StartVodPeerCmd, Integer> startVodPeer
            = new Operation1<StartVodPeerCmd, Integer>() {

                @Override
                public StartVodPeerCmd generate(Integer id) {
                    return new StartVodPeerCmd(id);
                }
            };

    static Operation1<StopVodPeerCmd, Integer> stopVodPeer
            = new Operation1<StopVodPeerCmd, Integer>() {

                @Override
                public StopVodPeerCmd generate(Integer id) {
                    return new StopVodPeerCmd(id);
                }
            };

    static Operation2<UploadVideoCmd, Integer, Integer> uploadVideo
            = new Operation2<UploadVideoCmd, Integer, Integer>() {

                @Override
                public UploadVideoCmd generate(Integer nodeId, Integer overlayId) {
                    return new UploadVideoCmd(nodeId, overlayId);
                }
            };

    public static SimulationScenario simpleBoot(final long seed, int peers) {
        SimulationScenario scen = new SimulationScenario() {
            {
                final Random rand = new Random(seed);
                final Distribution<Integer> bootstrapIdDist = new ConstantDistribution<>(Integer.class, 0);
                final Distribution<Integer> vodPeerIdDist = new IntegerUniformDistribution(1, 65535, new Random(seed));
                final Distribution<Integer> videoIdDist = new ConstantDistribution<>(Integer.class, 1);
                //generate the same ids - first id will be the uploader
                final Distribution<Integer> videoPeerIdDist = new IntegerUniformDistribution(1, 65535, new Random(seed));
                final Distribution<Integer> uploaderIdDist = new ConstantDistribution<>(Integer.class, videoPeerIdDist.draw());
                

                StochasticProcess startBootstrapServerProc = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startBootstrapServer, bootstrapIdDist);
                    }
                };

                StochasticProcess startVodPeersProc = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(2, startVodPeer, vodPeerIdDist);
                    }
                };

                StochasticProcess stopVodPeersProc = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, stopVodPeer, vodPeerIdDist);
                    }
                };

                StochasticProcess stopBootstrapServerProc = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(0));
                        raise(1, stopBootstrapServer, bootstrapIdDist);
                    }
                };

                StochasticProcess uploadVideoProc = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(0));
                        raise(1, uploadVideo, uploaderIdDist, videoIdDist);
                    }
                };

                startBootstrapServerProc.start();
                startVodPeersProc.startAfterTerminationOf(1000, startBootstrapServerProc);
                uploadVideoProc.startAfterTerminationOf(1000, startVodPeersProc);
                terminateAfterTerminationOf(1000 * 1000, uploadVideoProc);
            }
        };

        scen.setSeed(seed);

        return scen;
    }
}
