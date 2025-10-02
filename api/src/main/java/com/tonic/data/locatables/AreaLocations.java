package com.tonic.data.locatables;

import com.tonic.Static;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldArea;

/**
 * Enum of various notable areas in the game.
 * Each area is represented by a WorldArea object defining its boundaries.
 */
@AllArgsConstructor
@Getter
public enum AreaLocations {
    //Cities
    VARROCK(new WorldArea(3136,3374,158,147,0)),
    LUMBRIDGE(new WorldArea(3195, 3188, 61, 61, 0)),
    AL_KHARID(new WorldArea(3268, 3154, 56, 54, 0)),
    DRAYNOR(new WorldArea(3072, 3239, 35, 45, 0)),
    PORT_SARIM(new WorldArea(3008, 3177, 55, 86, 0)),
    FALADOR(new WorldArea(2934, 3307, 135, 90, 0)),
    TAVERLY(new WorldArea(2873, 3403, 64, 83, 0)),
    BURTHORPE(new WorldArea(2881, 2937, 56, 56, 0)),
    CATHERBY(new WorldArea(2789, 3409, 51, 48, 0)),
    SEERS(new WorldArea(2682, 3455, 61, 57, 0)),
    RELLEKKA(new WorldArea(2617, 3652, 71, 64, 0)),
    WIZARDS_TOWER(new WorldArea(3091, 3142, 37, 62, 0)),
    MAUSOLEUM(new WorldArea(3402, 9880, 42, 29, 0)),
    GOBLIN_VILLAGE(new WorldArea(2944, 3492, 24, 26, 0)),
    GNOME_MAZE_DUNGEON(new WorldArea(2507, 9551, 48, 35, 0)),
    DESERT(new WorldArea(3142, 2671, 389, 499, 0)),
    TREE_GNOME_STRONGHOLD(new WorldArea(2380, 3391, 114, 130, 0)),
    NIGHTMARE_ZONE(new WorldArea(2599, 3111, 11, 11, 0)),
    WINTERTODT(new WorldArea(1611, 3968, 40, 33, 0)),
    ;
    private final WorldArea area;

    public boolean inArea()
    {
        Client client = Static.getClient();
        if(client.getGameState() != GameState.LOGGED_IN)
            return false;

        return area.contains(client.getLocalPlayer().getWorldLocation());
    }
}