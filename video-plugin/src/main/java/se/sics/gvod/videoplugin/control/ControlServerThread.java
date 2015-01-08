/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.gvod.videoplugin.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import se.sics.gvod.manager.VoDManager;

/**
 *
 * @author jdowling
 */
public class ControlServerThread extends Thread {
    private Socket socket = null;
    private final VoDManager vodManager;

    public ControlServerThread(VoDManager vodManager, Socket socket) {
	super("ControlServerThread");
        this.vodManager = vodManager;
	this.socket = socket;
    }

    @Override
    public void run() {

        PrintWriter out = null;
        BufferedReader in = null;
	try {
	    out = new PrintWriter(socket.getOutputStream(), true);
	    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    String inputLine, outputLine;
	    ControlProtocol cp = new ControlProtocol(vodManager);

	    while ((inputLine = in.readLine()) != null) {
		outputLine = cp.processInput(inputLine);
                if (outputLine != null) {
                    out.print(outputLine + "\r\n");
                    out.flush();
                }
	    }
	} catch (IOException ex) {
	    throw new RuntimeException(ex);
	} finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
}