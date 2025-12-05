package com.tonic.services.pathfinder.collections;

import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.services.pathfinder.implimentations.hybridbfs.HybridBFSStep;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BFSCache
{
    private final TIntIntHashMap cache = new TIntIntHashMap(20000);

    public boolean put(final int point, final int parent)
    {
        if(cache.contains(point))
            return false;
        cache.put(point, parent);
        return true;
    }

    public int get(final int position)
    {
        return cache.get(position);
    }

    public void clear()
    {
        cache.clear();
    }

    public int size()
    {
        return cache.size();
    }

    public List<HybridBFSStep> path(int pos)
    {
        // First: Build path positions
        int parent = get(pos);
        LinkedList<Integer> positions = new LinkedList<>();
        positions.add(0, pos);
        while(parent != -1)
        {
            positions.add(0, parent);
            parent = get(parent);
        }

        // Second: Convert to steps, checking for transports between consecutive positions
        LinkedList<HybridBFSStep> path = new LinkedList<>();
        for(int i = 0; i < positions.size(); i++)
        {
            int currentPos = positions.get(i);
            Transport transport = null;

            // If there's a next position, check for transport from current to next
            if(i + 1 < positions.size())
            {
                int nextPos = positions.get(i + 1);
                transport = findTransport(currentPos, nextPos);
            }

            path.add(new HybridBFSStep(currentPos, transport));
        }

        return path;
    }

    private Transport findTransport(int source, int destination)
    {
        ArrayList<Transport> tr = TransportLoader.getTransports().get(source);
        if(tr != null)
        {
            for(Transport t : tr)
            {
                if(t.getDestination() == destination)
                {
                    return t;
                }
            }
        }
        return null;
    }
}