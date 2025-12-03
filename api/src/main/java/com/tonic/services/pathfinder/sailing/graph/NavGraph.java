package com.tonic.services.pathfinder.sailing.graph;

import com.tonic.services.pathfinder.collision.SparseBitSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap;

import java.io.InputStream;

/**
 * Navigation graph for high-level boat pathfinding.
 * Loaded from graph.dat resource file via Walker static initializer.
 *
 * The graph contains sparse nodes placed throughout navigable waters,
 * with edges connecting nearby nodes. Each edge stores a bitmask of
 * tile types it traverses, allowing water type avoidance.
 */
public class NavGraph {
    private final SparseBitSet nodeSet;
    private final Int2ObjectOpenHashMap<IntList> adjacencyList;
    private final Long2ShortOpenHashMap edgeMasks;

    /**
     * Creates a NavGraph with the given data structures.
     * Use NavGraphLoader to construct instances.
     */
    NavGraph(SparseBitSet nodeSet,
             Int2ObjectOpenHashMap<IntList> adjacencyList,
             Long2ShortOpenHashMap edgeMasks) {
        this.nodeSet = nodeSet;
        this.adjacencyList = adjacencyList;
        this.edgeMasks = edgeMasks;
    }

    /**
     * Loads the navigation graph from resources.
     * Called by Walker static initializer.
     *
     * @return The loaded NavGraph, or null if loading failed
     */
    public static NavGraph load() {
        try {
            InputStream is = NavGraph.class.getResourceAsStream("/com/tonic/services/pathfinder/graph.dat");
            if (is != null) {
                return NavGraphLoader.load(is);
            } else {
                System.err.println("NavGraph: graph.dat not found in resources");
                return null;
            }
        } catch (Exception e) {
            System.err.println("NavGraph: Failed to load graph: " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a node exists at the given packed coordinates.
     */
    public boolean hasNode(int packed) {
        return nodeSet.get(packed);
    }

    /**
     * Gets all neighbors of a node.
     *
     * @param packed The packed coordinates of the node
     * @return List of neighbor packed coordinates, or empty list if node doesn't exist
     */
    public IntList getNeighbors(int packed) {
        IntList neighbors = adjacencyList.get(packed);
        return neighbors != null ? neighbors : IntArrayList.of();
    }

    /**
     * Gets the tile type mask for an edge between two nodes.
     *
     * @param fromPacked Source node packed coordinates
     * @param toPacked Target node packed coordinates
     * @return Bitmask of tile types on this edge (0 if edge doesn't exist)
     */
    public short getEdgeMask(int fromPacked, int toPacked) {
        long edgeKey = GraphNode.edgeKey(fromPacked, toPacked);
        return edgeMasks.getOrDefault(edgeKey, (short) 0);
    }

    /**
     * Checks if an edge is traversable given the avoid types.
     * An edge is NOT traversable if ANY of its tile types are in the avoid list.
     *
     * @param fromPacked Source node packed coordinates
     * @param toPacked Target node packed coordinates
     * @param avoidTypes Array of tile types to avoid
     * @return true if the edge can be traversed, false otherwise
     */
    public boolean isEdgeTraversable(int fromPacked, int toPacked, byte[] avoidTypes) {
        short mask = getEdgeMask(fromPacked, toPacked);
        if (mask == 0) {
            return true; // No tile types or edge doesn't exist
        }

        for (byte bad : avoidTypes) {
            if (bad >= 1 && bad <= 16) {
                if ((mask & (1 << (bad - 1))) != 0) {
                    return false; // Edge crosses a bad water type
                }
            }
        }
        return true;
    }

    /**
     * Gets the total number of nodes in the graph.
     */
    public int getNodeCount() {
        return nodeSet.cardinality();
    }

    /**
     * Gets the total number of edges in the graph.
     */
    public int getEdgeCount() {
        return edgeMasks.size();
    }

    /**
     * Iterates over all node packed coordinates.
     * Use: for (int node = getNextNode(0); node >= 0; node = getNextNode(node + 1))
     */
    public int getNextNode(int fromIndex) {
        return nodeSet.nextSetBit(fromIndex);
    }
}
