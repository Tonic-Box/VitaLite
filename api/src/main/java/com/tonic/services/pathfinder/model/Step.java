package com.tonic.services.pathfinder.model;

import com.tonic.util.WorldPointUtil;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

public class Step {
    public final int position;

    public WorldPoint getPosition()
    {
        List<WorldPoint> point = WorldPointUtil.toInstance(WorldPointUtil.fromCompressed(position));
        if(!point.isEmpty())
        {
            return point.get(0);
        }
        return WorldPointUtil.fromCompressed(position);
    }

    public Step(int position) {
        this.position = position;
    }

    public boolean hasTransport()
    {
        return false;
    }
}