package com.tonic.services.pathfinder.model;

import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

public class Step {
    public final int position;
    public final Transport transport;

    public WorldPoint getPosition()
    {
        List<WorldPoint> point = WorldPointUtil.toInstance(WorldPointUtil.fromCompressed(position));
        if(!point.isEmpty())
        {
            return point.get(0);
        }
        return WorldPointUtil.fromCompressed(position);
    }

    public Step(int position, Transport transport) {
        this.position = position;
        this.transport = transport;
    }

    public boolean hasTransport()
    {
        return transport != null;
    }

    public static List<WorldPoint> toWorldPoints(List<Step> steps)
    {
        return steps.stream().map(Step::getPosition).collect(Collectors.toList());
    }
}