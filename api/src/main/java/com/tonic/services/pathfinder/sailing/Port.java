package com.tonic.services.pathfinder.sailing;

import com.tonic.services.pathfinder.requirements.Requirements;
import net.runelite.api.Quest;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.List;

/**
 * A dock from the game's dock table, with where it lies on the sea map and what boarding or docking there needs
 */
public final class Port
{
    private final int dockId;
    private final int row;
    private final String name;
    private final int level;
    private final List<Quest> quests;
    private final int gangplankLocId;
    private final int buoyLocId;
    private final WorldPoint land;
    private final WorldPoint approach;
    private final Requirements requirements;

    Port(int dockId, int row, String name, int level, List<Quest> quests, int gangplankLocId, int buoyLocId, WorldPoint land, WorldPoint approach, Requirements requirements)
    {
        this.dockId = dockId;
        this.row = row;
        this.name = name;
        this.level = level;
        this.quests = Collections.unmodifiableList(quests);
        this.gangplankLocId = gangplankLocId;
        this.buoyLocId = buoyLocId;
        this.land = land;
        this.approach = approach;
        this.requirements = requirements;
    }

    public int dockId()
    {
        return dockId;
    }

    public int row()
    {
        return row;
    }

    public String name()
    {
        return name;
    }

    public int level()
    {
        return level;
    }

    public List<Quest> quests()
    {
        return quests;
    }

    public int gangplankLocId()
    {
        return gangplankLocId;
    }

    public int buoyLocId()
    {
        return buoyLocId;
    }

    /**
     * Gets the gangplank's tile on land
     * @return the land tile, or null when the sea map has no dock for it
     */
    public WorldPoint land()
    {
        return land;
    }

    /**
     * Gets the open water tile a boat sails to before docking
     * @return the approach tile, or null when not placed
     */
    public WorldPoint approach()
    {
        return approach;
    }

    /**
     * Checks whether the sea map places the dock
     * @return true if the dock is placed, false otherwise
     */
    public boolean placed()
    {
        return land != null && approach != null && gangplankLocId >= 0;
    }

    public Requirements requirements()
    {
        return requirements;
    }

    /**
     * Checks whether the player may board or dock here
     * @return true if the player meets the requirements, false otherwise
     */
    public boolean allowed()
    {
        return requirements == null || requirements.fulfilled();
    }

    @Override
    public String toString()
    {
        return name;
    }
}
