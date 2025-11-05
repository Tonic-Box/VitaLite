package com.tonic.services.pathfinder.implimentations.jps;

import com.tonic.services.pathfinder.abstractions.IStep;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a single step in a JPS+ pathfinding result.
 */
public class JPSStep implements IStep
{
    private final int position;
    private final Transport transport;

    public JPSStep(int position, Transport transport) {
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

    public static List<WorldPoint> toWorldPoints(List<JPSStep> steps)
    {
        return steps.stream().map(JPSStep::getPosition).collect(Collectors.toList());
    }
}
