/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.gvod.videoplugin.control;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import se.sics.gvod.manager.VoDManager;

/**
 * @author jdowling
 */
public class ControlServer extends Thread {

    private ServerSocket serverSocket = null;
    private boolean listening = true;
    private final VoDManager vodMain;
    private ControlServerThread cs;

    public ControlServer(VoDManager vodMain, int controlPort) throws IOException {

        this.vodMain = vodMain;
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        SocketAddress addr = new InetSocketAddress(controlPort);
        serverSocket.bind(addr);
    }

    @Override
    public void run() {

        while (listening) {
            try {
                cs = new ControlServerThread(vodMain, serverSocket.accept());
                cs.start();
            } catch (IOException ex) {
                throw new RuntimeException();
            }
        }
        try {
            serverSocket.close();
        } catch (IOException ex) {
            throw new RuntimeException();
        }

    }

}
