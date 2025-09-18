package com.tonic.services.pathfinder.collections;

import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.services.pathfinder.model.Step;
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

    public List<Step> path(int pos)
    {
        int parent = get(pos);
        LinkedList<Step> path = new LinkedList<>();
        Transport transport;
        path.add(0, new Step(pos, null));
        while(parent != -1)
        {
            transport = getTransport(pos, parent);
            pos = parent;
            parent = get(pos);
            path.add(0, new Step(pos, transport));
        }
        return path;
    }

    public Transport getTransport(int pos, int parent)
    {
        ArrayList<Transport> tr = TransportLoader.getTransports().get(parent);
        if(tr != null)
        {
            for (int i = 0; i < tr.size(); i++)
            {
                if(tr.get(i).getDestination() == pos)
                {
                    return tr.get(i);
                }
            }
        }
        return null;
    }
}