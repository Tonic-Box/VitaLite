// Example code showing how to use the VitaLite runtime environment

// Get the RuneLite client instance with proper typing
Client client = Static.getClient();
Player localPlayer = client.getLocalPlayer();
if (localPlayer != null) {
    out.println("Local player name: " + localPlayer.getName());
    WorldPoint location = localPlayer.getWorldLocation();
    out.println("Player location: " + location);
} else {
    out.println("No local player found (not logged in?)");
}

List<PlayerEx> players = GameManager.playerList();
out.println("Players in area: " + players.size());

List<NpcEx> npcs = GameManager.npcList();
out.println("NPCs in area: " + npcs.size());