package com.tonic.services.pathfinder.sailing.graph;

import com.tonic.services.pathfinder.collision.SparseBitSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap;

import java.io.*;

/**
 * Binary loader for navigation graph files.
 *
 * Format:
 *   Header: "GWEB" (4 bytes) + version (1 byte)
 *   Nodes: SparseBitSet serialized via ObjectInputStream (length:4 + bytes)
 *   Edges: VarInt count + (source_packed:4, target_packed:4, tile_mask:2) per edge
 */
public final class NavGraphLoader {
    private static final byte[] MAGIC = {'G', 'W', 'E', 'B'};
    private static final byte VERSION = 1;

    private NavGraphLoader() {
        // Utility class
    }

    /**
     * Loads a navigation graph from an input stream.
     *
     * @param is The input stream to read from
     * @return Loaded NavGraph, or null on failure
     */
    public static NavGraph load(InputStream is) {
        if (is == null) {
            return null;
        }

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(is))) {
            // Read and verify header
            byte[] magic = new byte[4];
            dis.readFully(magic);
            if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1] ||
                magic[2] != MAGIC[2] || magic[3] != MAGIC[3]) {
                System.err.println("NavGraphLoader: Invalid graph file magic number");
                return null;
            }

            byte version = dis.readByte();
            if (version != VERSION) {
                System.err.println("NavGraphLoader: Unsupported graph file version: " + version);
                return null;
            }

            // Read SparseBitSet for nodes
            int nodeByteLen = dis.readInt();
            byte[] nodeBytes = new byte[nodeByteLen];
            dis.readFully(nodeBytes);

            SparseBitSet nodeSet;
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(nodeBytes))) {
                nodeSet = (SparseBitSet) ois.readObject();
            } catch (ClassNotFoundException e) {
                System.err.println("NavGraphLoader: Failed to deserialize node set: " + e.getMessage());
                return null;
            }

            // Read edges and build adjacency list
            int edgeCount = VarInt.readVarInt(dis);
            Int2ObjectOpenHashMap<IntList> adjacencyList = new Int2ObjectOpenHashMap<>();
            Long2ShortOpenHashMap edgeMasks = new Long2ShortOpenHashMap();

            for (int i = 0; i < edgeCount; i++) {
                int sourcePacked = dis.readInt();
                int targetPacked = dis.readInt();
                short tileTypeMask = dis.readShort();

                // Add bidirectional adjacency
                adjacencyList.computeIfAbsent(sourcePacked, k -> new IntArrayList()).add(targetPacked);
                adjacencyList.computeIfAbsent(targetPacked, k -> new IntArrayList()).add(sourcePacked);

                // Store edge mask (use normalized key)
                long edgeKey = GraphNode.edgeKey(sourcePacked, targetPacked);
                edgeMasks.put(edgeKey, tileTypeMask);
            }

            System.out.println("NavGraphLoader: Loaded " + nodeSet.cardinality() + " nodes and " + edgeCount + " edges");
            return new NavGraph(nodeSet, adjacencyList, edgeMasks);

        } catch (IOException e) {
            System.err.println("NavGraphLoader: Failed to load graph: " + e.getMessage());
            return null;
        }
    }
}
