package com.tonic.services.pathfinder;

import com.tonic.Static;

public enum PathfinderAlgo
{
    HYBRID_BFS("com.tonic.services.pathfinder.implimentations.hybridbfs.HybridBFSAlgo"),
    FLOW_FIELD("com.tonic.services.pathfinder.implimentations.flowfield.FlowFieldAlgo"),
    ASTAR("com.tonic.services.pathfinder.implimentations.astar.AStarAlgo")
    ;

    private final String fqdn;

    PathfinderAlgo(String fqdn)
    {
        this.fqdn = fqdn;
    }

    public Class<?> getPathfinder()
    {
        try
        {
            return Static.getClassLoader().loadClass(fqdn);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    public <T> T newInstance()
    {
        try
        {
            return (T) getPathfinder().getDeclaredConstructor().newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
