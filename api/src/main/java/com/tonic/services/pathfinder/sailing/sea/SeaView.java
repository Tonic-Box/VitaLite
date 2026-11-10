package com.tonic.services.pathfinder.sailing.sea;

/**
 * What the sea is like tile by tile, as the sea map holds it or as a search reads it through its own grid
 */
public interface SeaView
{
    /**
     * Checks whether the tile carries water of any family
     * @param x Tile x
     * @param y Tile y
     * @return true if the tile is water
     */
    boolean isWater(int x, int y);

    /**
     * Gets the water family of a tile
     * @param x Tile x
     * @param y Tile y
     * @return the tile's family, NONE where there is no water
     */
    WaterFamily family(int x, int y);

    /**
     * Checks whether walking is blocked on the tile, by its flags or by something standing on it
     * @param x Tile x
     * @param y Tile y
     * @return true if walking is blocked
     */
    boolean walkBlocked(int x, int y);

    /**
     * Gets how many tiles of open water lie between the tile and the nearest obstacle
     * @param x Tile x
     * @param y Tile y
     * @return the clearance, up to SeaMap.MOST_CLEARANCE
     */
    int clearance(int x, int y);

    /**
     * Gets the tile's sub-cells that are water
     * @param x Tile x
     * @param y Tile y
     * @return the water cells, a bit per cell
     */
    int cells(int x, int y);

    /**
     * Checks whether the tile is water right across, not crossed by a coast
     * @param x Tile x
     * @param y Tile y
     * @return true if every sub-cell is water
     */
    boolean fullWater(int x, int y);

    /**
     * Checks whether the sub-cell under the position is water
     * @param fineX Fine x position
     * @param fineY Fine y position
     * @return true if the sub-cell is water
     */
    boolean waterAt(int fineX, int fineY);
}
