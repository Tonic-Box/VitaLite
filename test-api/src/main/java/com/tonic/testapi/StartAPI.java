package com.tonic.testapi;

/**
 * Simple launcher to start the Bot Control API from within VitaLite.
 * 
 * Call this from VitaLite console or startup:
 *   new com.tonic.testapi.StartAPI().start();
 */
public class StartAPI {
    private static BotControlServer server;
    
    public static void start() {
        start(8888);
    }
    
    public static void start(int port) {
        if (server != null) {
            System.out.println("Bot Control API is already running");
            return;
        }
        
        server = new BotControlServer(port);
        server.start();
        System.out.println("Bot Control API started on port " + port);
        System.out.println("Test with: curl http://localhost:" + port + "/api/health");
    }
    
    public static void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
    
    public static boolean isRunning() {
        return server != null;
    }
}
