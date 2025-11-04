package com.tonic.services.pathfinder.implimentations.bidirbfs;

import com.tonic.services.pathfinder.abstractions.IStep;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

public class BiDirBFSStep implements IStep
{
    private final int position;
    private final Transport transport;

    public BiDirBFSStep(int position, Transport transport) {
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

    public boolean hasTransport()
    {
        return transport != null;
    }

    public static List<WorldPoint> toWorldPoints(List<BiDirBFSStep> steps)
    {
        return steps.stream().map(BiDirBFSStep::getPosition).collect(Collectors.toList());
    }
}
