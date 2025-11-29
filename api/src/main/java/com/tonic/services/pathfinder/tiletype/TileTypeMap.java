package com.tonic.services.pathfinder.tiletype;

import com.tonic.data.wrappers.PlayerEx;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.collision.SparseBitSet;

import java.io.*;

/**
 * Reader for tile type data stored in SparseBitSet format.
 * Retrieves 4-bit tile type values per coordinate using 4 separate bit sets.
 */
public class TileTypeMap {
    private final SparseBitSet[] bitSets;

    /**
     * Creates a new TileTypeMap with the given bit sets.
     *
     * @param bitSets the array of 4 bit sets containing tile type data
     */
    private TileTypeMap(SparseBitSet[] bitSets) {
        this.bitSets = bitSets;
    }

    /**
     * Gets the tile type at the specified coordinate.
     * Reconstructs the 4-bit tile type from individual bits stored in separate bit sets.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z/plane coordinate
     * @return the tile type (0-15), where 0 means no special type
     */
    public byte getTileType(int x, int y, int z) {
        int index = (x & 8191) | ((y & 32767) << 13) | (z << 28);
        byte type = 0;
        // Reconstruct 4-bit tile type from individual bit sets
        for (int bit = 0; bit < 4; bit++) {
            if (bitSets[bit].get(index)) {
                type |= (byte) (1 << bit);
            }
        }
        return type;
    }

    public TileType getTileTypeEnum(int x, int y, int z) {
        byte typeValue = getTileType(x, y, z);
        return TileType.fromValue(typeValue);
    }

    /**
     * Loads a tile type map from resources.
     *
     * @return the tile type map
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if the class is not found
     */
    public static TileTypeMap load() throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(Walker.class.getResourceAsStream("tile_type.dat"))) {
            return new TileTypeMap((SparseBitSet[]) ois.readObject());
        }
    }
}