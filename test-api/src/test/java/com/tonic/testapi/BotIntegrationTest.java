package com.tonic.testapi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for bot functionality via REST API.
 * 
 * Prerequisites:
 * - VitaLite must be running with BotControlServer started
 * - Bot must have valid credentials configured
 * - RSMod game server must be running
 * 
 * Run with: ./gradlew :test-api:test
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BotIntegrationTest {
    
    private static final String API_BASE = "http://localhost:8888/api";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson gson = new Gson();
    
    @BeforeAll
    static void setup() {
        // Verify API is running
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/health"))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), "Bot Control API must be running");
            
        } catch (Exception e) {
            fail("Bot Control API is not accessible. Start VitaLite with BotControlServer first: " + e.getMessage());
        }
    }
    
    // ===== LOGIN TESTS =====
    
    @Test
    @Order(1)
    @DisplayName("Login - Should successfully login with valid credentials")
    void testLogin() throws Exception {
        // Use credentials from environment or test config
        String credentials = System.getenv("OSRS_CREDENTIALS");
        if (credentials == null) {
            credentials = "testuser:testpass"; // Fallback for local testing
        }
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/login"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(credentials))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        
        JsonObject result = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(result.get("success").getAsBoolean(), "Login should succeed");
        
        // Wait for login to complete
        Thread.sleep(5000);
        
        // Verify logged in
        JsonObject status = getStatus();
        assertTrue(status.get("loggedIn").getAsBoolean(), "Should be logged in after login");
    }
    
    @Test
    @Order(2)
    @DisplayName("Status - Should return valid player status when logged in")
    void testGetStatusWhenLoggedIn() throws Exception {
        JsonObject status = getStatus();
        
        assertTrue(status.get("loggedIn").getAsBoolean(), "Should be logged in");
        assertNotNull(status.get("playerName"), "Player name should be set");
        assertTrue(status.get("x").getAsInt() > 0, "X coordinate should be valid");
        assertTrue(status.get("y").getAsInt() > 0, "Y coordinate should be valid");
        assertTrue(status.get("hp").getAsInt() > 0, "HP should be greater than 0");
        assertTrue(status.get("maxHp").getAsInt() > 0, "Max HP should be greater than 0");
    }
    
    // ===== MOVEMENT TESTS =====
    
    @Test
    @Order(3)
    @DisplayName("Movement - Should walk to target coordinates")
    void testWalkToCoordinates() throws Exception {
        JsonObject initialStatus = getStatus();
        int startX = initialStatus.get("x").getAsInt();
        int startY = initialStatus.get("y").getAsInt();
        
        // Walk 5 tiles north
        int targetX = startX;
        int targetY = startY + 5;
        
        JsonObject walkRequest = new JsonObject();
        walkRequest.addProperty("x", targetX);
        walkRequest.addProperty("y", targetY);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/walk"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(walkRequest)))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        
        JsonObject result = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(result.get("success").getAsBoolean(), "Walk command should succeed");
        
        // Wait for movement
        Thread.sleep(3000);
        
        // Verify position changed
        JsonObject finalStatus = getStatus();
        int endX = finalStatus.get("x").getAsInt();
        int endY = finalStatus.get("y").getAsInt();
        
        // Player should have moved closer to target
        int initialDistance = Math.abs(startX - targetX) + Math.abs(startY - targetY);
        int finalDistance = Math.abs(endX - targetX) + Math.abs(endY - targetY);
        
        assertTrue(finalDistance < initialDistance, "Player should move closer to target");
    }
    
    @Test
    @Order(4)
    @DisplayName("Movement - Should toggle run mode")
    void testToggleRun() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/run/toggle"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        
        JsonObject result = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(result.get("success").getAsBoolean());
        
        JsonObject data = result.getAsJsonObject("data");
        assertNotNull(data.get("runEnabled"), "Run enabled status should be returned");
        assertNotNull(data.get("runEnergy"), "Run energy should be returned");
    }
    
    // ===== GAME STATE TESTS =====
    
    @Test
    @Order(5)
    @DisplayName("Game State - Should return valid game state information")
    void testGetGameState() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/game/state"))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        
        JsonObject result = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(result.get("success").getAsBoolean());
        
        JsonObject state = result.getAsJsonObject("data");
        assertTrue(state.get("loggedIn").getAsBoolean(), "Should be logged in");
        assertNotNull(state.get("gameState"), "Game state should be present");
        assertNotNull(state.get("inWilderness"), "Wilderness status should be present");
    }
    
    @Test
    @Order(6)
    @DisplayName("Skills - Should return player skill levels")
    void testGetSkills() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/player/skills"))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        
        JsonObject result = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(result.get("success").getAsBoolean());
        
        JsonObject skills = result.getAsJsonObject("data");
        
        // Verify Attack skill (skill 0)
        JsonObject attack = skills.getAsJsonObject("0");
        assertNotNull(attack, "Attack skill should be present");
        assertTrue(attack.get("level").getAsInt() >= 1, "Attack level should be at least 1");
        assertTrue(attack.get("experience").getAsInt() >= 0, "Experience should be non-negative");
    }
    
    // ===== INVENTORY TESTS =====
    
    @Test
    @Order(7)
    @DisplayName("Inventory - Should return inventory state")
    void testGetInventory() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/inventory"))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        
        JsonObject result = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(result.get("success").getAsBoolean());
        
        JsonObject inventory = result.getAsJsonObject("data");
        assertTrue(inventory.get("count").getAsInt() >= 0, "Item count should be non-negative");
        assertTrue(inventory.get("count").getAsInt() <= 28, "Item count should not exceed 28");
        assertTrue(inventory.get("freeSlots").getAsInt() >= 0, "Free slots should be non-negative");
        assertTrue(inventory.get("freeSlots").getAsInt() <= 28, "Free slots should not exceed 28");
    }
    
    // ===== COMBAT TESTS =====
    
    @Test
    @Order(8)
    @DisplayName("Combat - Should return combat stats")
    void testGetCombatStats() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/combat/stats"))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        
        JsonObject result = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(result.get("success").getAsBoolean());
        
        JsonObject stats = result.getAsJsonObject("data");
        assertTrue(stats.get("health").getAsInt() > 0, "Health should be positive");
        assertTrue(stats.get("maxHealth").getAsInt() > 0, "Max health should be positive");
        assertTrue(stats.get("health").getAsInt() <= stats.get("maxHealth").getAsInt(), 
                   "Health should not exceed max health");
        assertTrue(stats.get("prayer").getAsInt() >= 0, "Prayer should be non-negative");
        assertTrue(stats.get("specialAttack").getAsInt() >= 0, "Special attack should be non-negative");
    }
    
    // ===== LOGOUT TEST =====
    
    @Test
    @Order(99)
    @DisplayName("Logout - Should successfully logout")
    void testLogout() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/logout"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        
        JsonObject result = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(result.get("success").getAsBoolean(), "Logout should succeed");
        
        // Wait for logout
        Thread.sleep(2000);
        
        // Verify logged out
        JsonObject status = getStatus();
        assertFalse(status.get("loggedIn").getAsBoolean(), "Should be logged out after logout");
    }
    
    // ===== HELPER METHODS =====
    
    private JsonObject getStatus() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/status"))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        
        JsonObject result = gson.fromJson(response.body(), JsonObject.class);
        assertTrue(result.get("success").getAsBoolean());
        
        return result.getAsJsonObject("data");
    }
}
