package com.tonic.services.pathfinder.implimentations.astar;

import com.tonic.services.pathfinder.abstractions.IStep;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

/**
 * Represents a single step in an A* pathfinding result.
 * Stores position as compressed int for memory efficiency.
 */
public class AStarStep implements IStep
{
    private final int position;
    private final Transport transport;

    public AStarStep(int position, Transport transport) {
        this.position = position;
        this.transport = transport;
    }

    @Override
    public WorldPoint getPosition()
    {
        List<WorldPoint> point = WorldPointUtil.toInstance(WorldPointUtil.fromCompressed(position));
        if(!point.isEmpty())
        {
            return point.get(0);
        }
        return WorldPointUtil.fromCompressed(position);
    }

    @Override
    public Transport getTransport() {
        return transport;
    }

    @Override
    public int getPackedPosition() {
        return position;
    }

    @Override
    public boolean hasTransport()
    {
        return transport != null;
    }
}
