package com.tonic.testapi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tonic.Static;
import com.tonic.api.TClient;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Minimal REST API server for controlling VitaLite bot.
 * 
 * Usage:
 *   BotControlServer server = new BotControlServer(8888);
 *   server.start();
 */
public class BotControlServer {
    
    private final Javalin app;
    private final Gson gson = new Gson();
    private final int port;
    
    public BotControlServer(int port) {
        this.port = port;
        this.app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.http.defaultContentType = "application/json";
        });
        
        setupRoutes();
    }
    
    private void setupRoutes() {
        // Health check
        app.get("/api/health", ctx -> {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Bot Control API is running");
            ctx.json(response);
        });
        
        // Status endpoint - basic info
        app.get("/api/status", ctx -> {
            executeOnClientThread(() -> {
                TClient client = Static.getClient();
                JsonObject status = new JsonObject();
                
                if (client == null) {
                    status.addProperty("clientInitialized", false);
                } else {
                    status.addProperty("clientInitialized", true);
                    // Add more status info as needed
                }
                
                return success(status);
            }, ctx);
        });
        
        // Login endpoint
        app.post("/api/login", ctx -> {
            String credentials = ctx.body();
            executeOnClientThread(() -> {
                com.tonic.services.AutoLogin.setCredentials(credentials);
                return success("Login initiated");
            }, ctx);
        });
        
        // Walk endpoint - simplified
        app.post("/api/walk", ctx -> {
            JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);
            int x = body.get("x").getAsInt();
            int y = body.get("y").getAsInt();
            
            executeOnClientThread(() -> {
                TClient client = Static.getClient();
                if (client == null) {
                    return error("Client not initialized");
                }
                
                // Use packet writer to send walk packet
                client.getPacketWriter().walkPacket(x, y, false);
                
                JsonObject result = new JsonObject();
                result.addProperty("targetX", x);
                result.addProperty("targetY", y);
                
                return success(result);
            }, ctx);
        });
    }
    
    private void executeOnClientThread(APICallable callable, Context ctx) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        
        try {
            Static.invoke(() -> {
                try {
                    JsonObject result = callable.call();
                    future.complete(result);
                } catch (Exception e) {
                    future.complete(error("Error: " + e.getMessage()));
                }
            });
            
            JsonObject result = future.get(5, TimeUnit.SECONDS);
            ctx.json(result);
            
        } catch (Exception e) {
            ctx.status(500).json(error("Request timeout: " + e.getMessage()));
        }
    }
    
    private JsonObject success(Object data) {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("data", gson.toJsonTree(data));
        return response;
    }
    
    private JsonObject error(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", message);
        return response;
    }
    
    @FunctionalInterface
    private interface APICallable {
        JsonObject call() throws Exception;
    }
    
    public void start() {
        app.start(port);
        System.out.println("Bot Control API started on http://localhost:" + port);
    }
    
    public void stop() {
        app.stop();
        System.out.println("Bot Control API stopped");
    }
    
    public int getPort() {
        return port;
    }
    
    // Main method for standalone testing
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8888;
        BotControlServer server = new BotControlServer(port);
        server.start();
    }
}
