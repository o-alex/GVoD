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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import se.sics.gvod.simulation.cmd.operations.DownloadVideoCmd;
import se.sics.gvod.simulation.cmd.operations.UploadVideoCmd;
import se.sics.gvod.simulation.cmd.system.StartBSCmd;
import se.sics.gvod.simulation.cmd.system.StartVodPeerCmd;
import se.sics.gvod.simulation.cmd.system.StopBSCmd;
import se.sics.gvod.simulation.cmd.system.StopVodPeerCmd;
import se.sics.gvod.simulation.util.IntegerUniformDistribution;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation1;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation2;
import se.sics.kompics.p2p.experiment.dsl.distribution.ConstantDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.Distribution;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ScenarioGen {

    static final Map<Integer, String> fileNames = new HashMap<Integer, String>();
    static final Distribution<Integer> videoIdDist = new ConstantDistribution<Integer>(Integer.class, 1);
    static String experimentDir;

    static {
        fileNames.put(1, "video1.mp4");
        try {
            File f = File.createTempFile("vodExperiment", "");
            f.delete();
            f.mkdir();
            experimentDir = f.getPath();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

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
                public StartVodPeerCmd generate(Integer nodeId) {
                    File nodeLibDir = new File(experimentDir + File.separator + "node" + nodeId);
                    nodeLibDir.mkdirs();
                    return new StartVodPeerCmd(nodeId, nodeLibDir.getPath());
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
                    try {
                        File nodeLibDir = new File(experimentDir + File.separator + "node" + nodeId);
                        String libDir = nodeLibDir.getPath();
                        File file = new File(libDir + File.separator + fileNames.get(overlayId));
                        file.createNewFile();
                        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
                        for (int i = 0; i < 10000; i++) {
                            writer.write("abc" + i + "\n");
                        }
                        writer.flush();
                        writer.close();
                        return new UploadVideoCmd(nodeId, fileNames.get(overlayId), overlayId);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };

    static Operation2<DownloadVideoCmd, Integer, Integer> downloadVideo
            = new Operation2<DownloadVideoCmd, Integer, Integer>() {

                @Override
                public DownloadVideoCmd generate(Integer nodeId, Integer overlayId) {
                    String libDir;
                    File nodeLibDir = new File(experimentDir + File.separator + "node" + nodeId);
                    nodeLibDir.mkdir();
                    libDir = nodeLibDir.getPath();
                    return new DownloadVideoCmd(nodeId, fileNames.get(overlayId), overlayId);
                }
            };

    public static SimulationScenario simpleBoot(final long seed, int peers) {
        SimulationScenario scen = new SimulationScenario() {
            {
                final Random rand = new Random(seed);
                final Distribution<Integer> bootstrapIdDist = new ConstantDistribution<Integer>(Integer.class, 0);
                final Distribution<Integer> vodPeerIdDist = new IntegerUniformDistribution(1, 65535, new Random(seed));
                //generate the same ids - first id will be the uploader
                final Distribution<Integer> videoPeerIdDist = new IntegerUniformDistribution(1, 65535, new Random(seed));
                final Distribution<Integer> uploaderIdDist = new ConstantDistribution<Integer>(Integer.class, videoPeerIdDist.draw());
                final Distribution<Integer> downloaderIdDist = videoPeerIdDist;

                StochasticProcess startBootstrapServerProc = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startBootstrapServer, bootstrapIdDist);
                    }
                };

                StochasticProcess startVodPeersProc = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(10, startVodPeer, vodPeerIdDist);
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

                StochasticProcess downloadVideoProc = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(5, downloadVideo, downloaderIdDist, videoIdDist);
                    }
                };

                startBootstrapServerProc.start();
                startVodPeersProc.startAfterTerminationOf(1000, startBootstrapServerProc);
                uploadVideoProc.startAfterTerminationOf(1000, startVodPeersProc);
                downloadVideoProc.startAfterStartOf(10 * 1000, uploadVideoProc);
                terminateAfterTerminationOf(1000 * 1000, uploadVideoProc);
            }
        };

        scen.setSeed(seed);

        return scen;
    }
}
