package com.tonic.services.pathfinder;

import com.tonic.Static;
import com.tonic.services.pathfinder.collections.BFSCache;
import com.tonic.services.pathfinder.collections.IntQueue;
import com.tonic.services.pathfinder.local.CollisionUtil;
import com.tonic.services.pathfinder.implimentations.hybridbfs.HybridBFSStep;
import com.tonic.util.WorldPointUtil;
import gnu.trove.map.hash.TIntIntHashMap;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

public class LocalPathfinder
{
    private static final LocalPathfinder instance = new LocalPathfinder();
    public static LocalPathfinder get()
    {
        return instance;
    }
    private CollisionUtil collisionUtil;
    private int target;
    private final BFSCache visited = new BFSCache();
    private final IntQueue queue = new IntQueue(5000);
    private LocalPathfinder()
    {
    }

    public List<HybridBFSStep> findPath(WorldPoint start, WorldPoint end)
    {
        collisionUtil = new CollisionUtil(getCollision());

        target = WorldPointUtil.compress(end.getX(), end.getY(), end.getPlane());
        visited.clear();
        visited.put(WorldPointUtil.compress(start.getX(), start.getY(), start.getPlane()), -1);
        queue.clear();
        queue.enqueue(WorldPointUtil.compress(start.getX(), start.getY(), start.getPlane()));
        return bfs();
    }

    private List<HybridBFSStep> bfs()
    {
        int current;
        while(!queue.isEmpty())
        {
            current = queue.dequeue();
            if(current == target)
            {
                //Logger.info("Nodes visited: " + visited.size());
                return visited.path(current);
            }
            addNeighbors(current);
        }
        return new ArrayList<>();
    }

    private void addNeighbors(int current)
    {
        int x = WorldPointUtil.getCompressedX(current);
        int y = WorldPointUtil.getCompressedY(current);

        if(!collisionUtil.blockedWest(x, y))
        {
            addNeighbor(current, WorldPointUtil.dx(current, -1));
        }

        if(!collisionUtil.blockedEast(x, y))
        {
            addNeighbor(current, WorldPointUtil.dx(current, 1));
        }

        if(!collisionUtil.blockedNorth(x, y))
        {
            addNeighbor(current, WorldPointUtil.dy(current, 1));
        }

        if(!collisionUtil.blockedSouth(x, y))
        {
            addNeighbor(current, WorldPointUtil.dy(current, -1));
        }

        if(!collisionUtil.blockedNorthWest(x, y))
        {
            addNeighbor(current, WorldPointUtil.dy(WorldPointUtil.dx(current, -1), 1));
        }

        if(!collisionUtil.blockedNorthEast(x, y))
        {
            addNeighbor(current, WorldPointUtil.dy(WorldPointUtil.dx(current, 1), 1));
        }

        if(!collisionUtil.blockedSouthWest(x, y))
        {
            addNeighbor(current, WorldPointUtil.dy(WorldPointUtil.dx(current, -1), -1));
        }

        if(!collisionUtil.blockedSouthEast(x, y))
        {
            addNeighbor(current, WorldPointUtil.dy(WorldPointUtil.dx(current, 1), -1));
        }
    }

    private void addNeighbor(final int node, final int neighbor) {
        if (visited.put(neighbor, node))
        {
            queue.enqueue(neighbor);
        }
    }
    private TIntIntHashMap getCollision()
    {
        Client client = Static.getClient();
        TIntIntHashMap collisionMap = new TIntIntHashMap();
        WorldView wv = client.getTopLevelWorldView();
        if(wv.getCollisionMaps() == null || wv.getCollisionMaps()[wv.getPlane()] == null)
            return collisionMap;

        int[][] flags = wv.getCollisionMaps()[wv.getPlane()].getFlags();
        WorldPoint point;
        for(int x = 0; x < flags.length; x++)
        {
            for(int y = 0; y < flags[x].length; y++)
            {
                point = WorldPoint.fromScene(client, x, y, wv.getPlane());
                collisionMap.put(WorldPointUtil.compress(point.getX(), point.getY(), wv.getPlane()), flags[x][y]);
            }
        }
        return collisionMap;
    }
}
