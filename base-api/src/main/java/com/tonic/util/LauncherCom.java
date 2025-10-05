package com.tonic.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class LauncherCom {
    public static void sendReadySignal(int port, String message) {
        try (Socket socket = new Socket("localhost", port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            writer.println(message);

        } catch (IOException e) {
            // Ignore - launcher may have already exited
            System.err.println("Could not send ready signal: " + e.getMessage());
        }
    }
}
