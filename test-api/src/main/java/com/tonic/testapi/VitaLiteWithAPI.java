package com.tonic.testapi;

/**
 * Wrapper to launch VitaLite with Bot Control API automatically enabled.
 * 
 * Usage:
 *   java -cp "..." com.tonic.testapi.VitaLiteWithAPI --proxy 127.0.255.3:43701
 */
public class VitaLiteWithAPI {
    
    public static void main(String[] args) {
        System.out.println("=== Starting VitaLite with Bot Control API ===");
        
        // Start Bot Control API in background thread
        Thread apiThread = new Thread(() -> {
            try {
                Thread.sleep(10000); // Wait for VitaLite to initialize
                System.out.println("[API] Starting Bot Control API on port 8888...");
                StartAPI.start(8888);
                System.out.println("[API] Bot Control API started successfully");
                System.out.println("[API] Test with: curl http://localhost:8888/api/health");
            } catch (Exception e) {
                System.err.println("[API] Failed to start: " + e.getMessage());
                e.printStackTrace();
            }
        });
        apiThread.setDaemon(true);
        apiThread.start();
        
        // Launch VitaLite main
        try {
            System.out.println("[VitaLite] Launching main client...");
            Class.forName("com.tonic.vitalite.Main")
                .getMethod("main", String[].class)
                .invoke(null, (Object) args);
        } catch (Exception e) {
            System.err.println("[VitaLite] Failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
