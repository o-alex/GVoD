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

import se.sics.gvod.system.Launcher;
import se.sics.kompics.Kompics;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */

public class Main {
    public static void runMain() {
        (new Thread(new RunMain())).start();
    }
    
    public static class RunMain implements Runnable {

        @Override
        public void run() {
            main(new String[0]);
        }
    }
    
    public static void main(String[] args) {
        if(args.length == 3) {
            boolean download = (args[0].equals("download") ? true : false);
            String fileName = args[1];
            int overlayId = Integer.parseInt(args[2]);
            Launcher.firstCmd = new Launcher.CMD(download, fileName, overlayId);
        }
        start();
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    public static void start() {
        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
        Kompics.createAndStart(Launcher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
    }

    public static void stop() {
        Kompics.shutdown();
    }
}
