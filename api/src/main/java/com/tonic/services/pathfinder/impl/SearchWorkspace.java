package com.tonic.services.pathfinder.impl;

import com.tonic.services.pathfinder.collections.BFSCache;
import com.tonic.services.pathfinder.collections.HybridIntQueue;
import com.tonic.services.pathfinder.collections.TileBitSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Everything one search writes to: the route tree, the queue, the transport arrivals still waiting and the tiles they wait on, and the tiles a transport leaves from; borrowed from SearchWorkspaces for one search at a time.
 */
final class SearchWorkspace
{
    private static final int FIRST_QUEUE = 1 << 16;

    final BFSCache visited = new BFSCache();
    final HybridIntQueue queue = new HybridIntQueue(FIRST_QUEUE);
    final Int2ObjectOpenHashMap<Pathfinder.Arrival> pending = new Int2ObjectOpenHashMap<>();
    final TileBitSet pendingTiles = new TileBitSet();
    final TileBitSet transportTiles = new TileBitSet();

    /**
     * Empties every part for the next search, keeping what each has grown to.
     */
    void reset()
    {
        visited.clear();
        queue.reset();
        pending.clear();
        pendingTiles.clear();
        transportTiles.clear();
    }
}
