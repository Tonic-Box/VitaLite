package com.tonic.services.worldselect;

import lombok.Getter;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;

import java.awt.Color;
import java.util.EnumSet;
import java.util.Set;

/**
 * The game modes the world select filters by, in the client's own order; a world is in the first mode whose types it all has, Deadman (Season) tried first, and in Main game when none match.
 */
@Getter
public enum GameMode
{
    MAIN_GAME("Main game", new Color(0x8A8F98), EnumSet.noneOf(WorldType.class)),
    PVP("Main game (PvP)", new Color(0xE04848), EnumSet.of(WorldType.PVP)),
    HARDCORE("Hardcore", new Color(0xE67E22), EnumSet.of(WorldType.HARDCORE_MAIN)),
    DEADMAN_PERMANENT("Deadman (Perm.)", new Color(0xA32626), EnumSet.of(WorldType.DEADMAN)),
    DEADMAN_SEASON("Deadman (Season)", new Color(0xD0453A), EnumSet.of(WorldType.DEADMAN, WorldType.TOURNAMENT)),
    SPECIAL("Special", new Color(0x1ABC9C), EnumSet.of(WorldType.NOSAVE_MODE)),
    LEAGUES("Leagues", new Color(0xF1C40F), EnumSet.of(WorldType.SEASONAL)),
    SPEEDRUNNING("Speedrunning", new Color(0x3498DB), EnumSet.of(WorldType.QUEST_SPEEDRUNNING)),
    FRESH_START("Fresh Start", new Color(0x2ECC71), EnumSet.of(WorldType.FRESH_START_WORLD)),
    PUBLIC_BETA("Public Beta", new Color(0x9B59B6), EnumSet.of(WorldType.BETA_WORLD));

    private static final GameMode[] VALUES = values();

    private final String displayName;
    private final Color color;
    private final Set<WorldType> types;

    GameMode(String displayName, Color color, Set<WorldType> types)
    {
        this.displayName = displayName;
        this.color = color;
        this.types = types;
    }

    /**
     * @param world a world
     * @return the mode the world is in
     */
    public static GameMode of(World world)
    {
        Set<WorldType> types = world.getTypes();
        if (types.containsAll(DEADMAN_SEASON.types))
        {
            return DEADMAN_SEASON;
        }
        for (GameMode mode : VALUES)
        {
            if (mode != MAIN_GAME && mode != DEADMAN_SEASON && types.containsAll(mode.types))
            {
                return mode;
            }
        }
        return MAIN_GAME;
    }

    /**
     * @return whether worlds of this mode have their number drawn in red, as the client does for Deadman
     */
    public boolean isDangerous()
    {
        return this == DEADMAN_PERMANENT || this == DEADMAN_SEASON;
    }
}
