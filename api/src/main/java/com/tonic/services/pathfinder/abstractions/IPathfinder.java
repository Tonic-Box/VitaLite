package com.tonic.services.pathfinder.abstractions;

import com.tonic.services.pathfinder.teleports.Teleport;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

public interface IPathfinder
{
    Teleport getTeleport();

    List<? extends IStep> find(WorldPoint target);
    List<? extends IStep> find(WorldArea... worldAreas);
    List<? extends IStep> find(List<WorldArea> worldAreas);
}
