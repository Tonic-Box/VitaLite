# VitaLite Bot Control API & Integration Tests

REST API and integration test framework for programmatic control of VitaLite bot.

## Overview

This module provides:
- **REST API Server** - Control VitaLite bot via HTTP endpoints
- **Integration Tests** - Automated tests for login, movement, combat, and game interactions
- **VitaLite Plugin** - Auto-starts API server when client launches

## Quick Start

### 1. Build the Module

```bash
cd /home/linus/pitchfork/seedpatch/external/VitaLite
./gradlew :test-api:build
```

### 2. Start VitaLite with API

The API server starts automatically when VitaLite launches (if BotControlPlugin is enabled).

**Manual start:**
```java
import com.tonic.testapi.BotControlServer;

BotControlServer server = new BotControlServer(8888);
server.start();
```

### 3. Run Integration Tests

```bash
# Set credentials (optional)
export OSRS_CREDENTIALS="testuser:testpass"

# Run all integration tests
./gradlew :test-api:test

# Run specific test
./gradlew :test-api:test --tests BotIntegrationTest.testLogin
```

## API Reference

Base URL: `http://localhost:8888/api`

### Health & Status

#### `GET /api/health`
Health check endpoint.

**Response:**
```json
{
  "success": true,
  "data": "Bot Control API is running"
}
```

#### `GET /api/status`
Get current bot status (position, health, logged in state, etc.)

**Response:**
```json
{
  "success": true,
  "data": {
    "loggedIn": true,
    "gameState": 30,
    "playerName": "TestPlayer",
    "x": 3200,
    "y": 3200,
    "plane": 0,
    "hp": 99,
    "maxHp": 99,
    "prayer": 43,
    "maxPrayer": 43,
    "runEnergy": 10000
  }
}
```

### Authentication

#### `POST /api/login`
Login with credentials.

**Request Body (text/plain):**
```
sessionId:characterId:displayName
```

**Response:**
```json
{
  "success": true,
  "data": "Login initiated with credentials"
}
```

#### `POST /api/logout`
Logout from the game.

**Response:**
```json
{
  "success": true,
  "data": "Logout initiated"
}
```

### Movement

#### `POST /api/walk`
Walk to coordinates.

**Request Body:**
```json
{
  "x": 3200,
  "y": 3200
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "targetX": 3200,
    "targetY": 3200,
    "currentX": 3195,
    "currentY": 3195
  }
}
```

#### `POST /api/run/toggle`
Toggle run mode.

**Response:**
```json
{
  "success": true,
  "data": {
    "runEnabled": true,
    "runEnergy": 10000
  }
}
```

### Inventory

#### `GET /api/inventory`
Get inventory state.

**Response:**
```json
{
  "success": true,
  "data": {
    "count": 15,
    "freeSlots": 13
  }
}
```

#### `POST /api/inventory/use`
Use an inventory item.

**Request Body:**
```json
{
  "itemId": 995,
  "action": "Use"
}
```

### Combat

#### `GET /api/combat/stats`
Get combat stats (health, prayer, special attack).

**Response:**
```json
{
  "success": true,
  "data": {
    "health": 99,
    "maxHealth": 99,
    "prayer": 43,
    "maxPrayer": 43,
    "specialAttack": 100
  }
}
```

#### `POST /api/combat/attack`
Attack an NPC.

**Request Body:**
```json
{
  "npcIndex": 5
}
```

### Interactions

#### `POST /api/interact/object`
Interact with a game object.

**Request Body:**
```json
{
  "objectId": 2213,
  "x": 3200,
  "y": 3200,
  "action": 0
}
```

#### `POST /api/interact/npc`
Interact with an NPC.

**Request Body:**
```json
{
  "npcIndex": 5,
  "action": 0
}
```

#### `POST /api/interact/player`
Interact with another player.

**Request Body:**
```json
{
  "playerIndex": 3,
  "action": 0
}
```

### Game State

#### `GET /api/game/state`
Get current game state.

**Response:**
```json
{
  "success": true,
  "data": {
    "gameState": 30,
    "loggedIn": true,
    "inWilderness": false,
    "wildernessLevel": 0
  }
}
```

#### `GET /api/player/skills`
Get all skill levels and experience.

**Response:**
```json
{
  "success": true,
  "data": {
    "0": {
      "level": 99,
      "boosted": 99,
      "experience": 13034431
    },
    ...
  }
}
```

## Integration Test Examples

### Login Test

```java
@Test
void testLogin() throws Exception {
    String credentials = "testuser:testpass";
    
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8888/api/login"))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofString(credentials))
            .build();
    
    HttpResponse<String> response = client.send(request, 
        HttpResponse.BodyHandlers.ofString());
    
    assertEquals(200, response.statusCode());
}
```

### Movement Test

```java
@Test
void testWalk() throws Exception {
    JsonObject walkRequest = new JsonObject();
    walkRequest.addProperty("x", 3200);
    walkRequest.addProperty("y", 3200);
    
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8888/api/walk"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(walkRequest)))
            .build();
    
    HttpResponse<String> response = client.send(request, 
        HttpResponse.BodyHandlers.ofString());
    
    assertEquals(200, response.statusCode());
}
```

### Combat Test

```java
@Test
void testAttackNpc() throws Exception {
    JsonObject attackRequest = new JsonObject();
    attackRequest.addProperty("npcIndex", 5);
    
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8888/api/combat/attack"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(attackRequest)))
            .build();
    
    HttpResponse<String> response = client.send(request, 
        HttpResponse.BodyHandlers.ofString());
    
    assertEquals(200, response.statusCode());
}
```

## Python Example

```python
import requests
import time

API_BASE = "http://localhost:8888/api"

# Check health
response = requests.get(f"{API_BASE}/health")
print(response.json())

# Login
response = requests.post(f"{API_BASE}/login", 
    data="testuser:testpass",
    headers={"Content-Type": "text/plain"})
print(response.json())

time.sleep(5)  # Wait for login

# Get status
response = requests.get(f"{API_BASE}/status")
status = response.json()
print(f"Logged in: {status['data']['loggedIn']}")
print(f"Position: ({status['data']['x']}, {status['data']['y']})")

# Walk
response = requests.post(f"{API_BASE}/walk", json={
    "x": 3200,
    "y": 3200
})
print(response.json())

# Get inventory
response = requests.get(f"{API_BASE}/inventory")
print(response.json())
```

## Configuration

### Plugin Settings

In VitaLite client:
1. Open Settings
2. Navigate to "Bot Control API"
3. Configure:
   - **Auto-start**: Enable/disable auto-start on client launch
   - **Port**: API server port (default: 8888)

### Environment Variables

```bash
# Test credentials
export OSRS_CREDENTIALS="sessionId:characterId:displayName"

# API port override
export BOT_API_PORT=8888
```

## Architecture

```
┌─────────────────┐
│  External Test  │
│    Framework    │
└────────┬────────┘
         │ HTTP REST
         ▼
┌─────────────────┐
│ BotControlServer│ (Port 8888)
│   Javalin API   │
└────────┬────────┘
         │ Static.invoke()
         ▼
┌─────────────────┐
│  VitaLite APIs  │
│  (MovementAPI,  │
│   GameAPI, etc) │
└────────┬────────┘
         │ TPacketWriter
         ▼
┌─────────────────┐
│   Game Client   │
│    (TClient)    │
└────────┬────────┘
         │ SOCKS Proxy
         ▼
┌─────────────────┐
│     RSProx      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│     RSMod       │
│  (Game Server)  │
└─────────────────┘
```

## Thread Safety

All API calls are executed on the client thread using `Static.invoke()` to ensure thread safety:

```java
Static.invoke(() -> {
    // Safe to call VitaLite APIs here
    MovementAPI.walkTo(x, y);
});
```

## Troubleshooting

### API not accessible
- Verify VitaLite is running
- Check BotControlPlugin is enabled
- Verify port 8888 is not in use: `ss -tuln | grep 8888`

### Tests failing
- Ensure bot is logged in before running movement/combat tests
- Check RSMod game server is running
- Verify RSProx proxy is running

### Timeout errors
- Increase timeout in test config (default: 5 seconds)
- Check network connectivity between components

## License

Same as VitaLite
