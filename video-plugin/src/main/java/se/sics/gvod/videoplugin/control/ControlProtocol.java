/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.gvod.videoplugin.control;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.manager.VoDManager;

/**
 * @author jdowling
 */
public class ControlProtocol {

    private static final Logger log = LoggerFactory.getLogger(ControlProtocol.class);

    private static enum ProtocolCmd {

        START("START"), PLAY("PLAY"), PAUSE("PAUSE"), STOP("STOP"), SHUTDOWN("SHUTDOWN"), RESUME("RESUME"), UNKNOWN("UNKNOWN");

        public final String value;

        ProtocolCmd(String value) {
            this.value = value;
        }
    }

    private static enum ProtocolState {

        WAITING, PLAYING, ERROR, PAUSED, STOPPED
    }

    private ProtocolState state = ProtocolState.WAITING;
    private String activeTorrentUrl = "";
    private String activeGuiUrl = "";
    private final VoDManager vodManager;

    public ControlProtocol(VoDManager vodManager) {
        this.vodManager = vodManager;
    }

    public String processInput(String theInput) {
        String theOutput = null;

        log.debug("received input: {}", theInput);
        if (theInput == null) {
            return theOutput;
        }
        Pair<ProtocolCmd, String> parsedInput = parseInput(theInput);

        if (parsedInput.getValue0().equals(ProtocolCmd.START)) {
            state = ProtocolState.PLAYING;
            //TODO Alex is this a space in the input?
            String torrentUrl = theInput.substring(1);
            if (activeTorrentUrl.compareTo(torrentUrl) != 0) {
                long maxSleep = 60 * 1000;
                long totalSleep = 0;
                long sleepTime = 100;
                while (!vodManager.isInitialized() && totalSleep < maxSleep) {
                    try {
                        totalSleep += sleepTime;
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                if (vodManager.isInitialized()) {
                    theOutput = vodManager.downloadVideoFromUrl(torrentUrl);
                }
                if (theOutput == null) {
                    theOutput = "ERROR - peer already running for " + torrentUrl;
                } else {
                    activeGuiUrl = theOutput;
                }
            } else {
                log.warn("Tried to start {} when already playing", activeTorrentUrl);
                // return the same URL to the torrent for viewing - http://127.0.0.1:58026/...
                theOutput = activeGuiUrl;
            }
        } else if (parsedInput.getValue0().equals(ProtocolCmd.PAUSE)) {
            if (state == ProtocolState.PLAYING) {
                state = ProtocolState.PAUSED;
            }

        } else if (parsedInput.getValue0().equals(ProtocolCmd.SHUTDOWN)) {
        } else if (parsedInput.getValue0().equals(ProtocolCmd.RESUME)) {
        }

        log.debug("sending output {}", theOutput);
        return theOutput;
    }

    private Pair<ProtocolCmd, String> parseInput(String input) {
        for (ProtocolCmd cmd : ProtocolCmd.values()) {
            if (cmd.value.length() <= input.length() && cmd.value.equalsIgnoreCase(input.substring(0, cmd.value.length()))) {
                return Pair.with(cmd, input.substring(cmd.value.length()));
            }
        }
        return Pair.with(ProtocolCmd.UNKNOWN, input);
    }
}
