package com.tonic.services.pathfinder.sailing.sea;

import java.util.Arrays;

/**
 * A search's view of the sea map
 */
final class SeaGrid implements SeaView
{
    /**
     * A tile whose cost the search has not worked out yet
     */
    static final int UNKNOWN_COST = Integer.MIN_VALUE;
    /**
     * A tile the search has given no slot of its own yet
     */
    static final int NO_SLOT = -1;
    /**
     * The most objectives a search keeps a field for
     */
    static final int MOST_OBJECTIVES = 4;
    private static final int REGION_SHIFT = 6;
    private static final int REGIONS_X = 256;
    private static final int REGIONS_Y = 1024;
    private static final int TILES = SeaMap.REGION_SIZE * SeaMap.REGION_SIZE;
    private static final int FINE_SHIFT = 7;
    private static final int CELL_SHIFT = 5;
    private static final int CELL_MASK = SeaMap.CELLS - 1;
    private static final byte[] NO_WATER = new byte[TILES];
    private static final short[] NO_CELLS = new short[TILES];

    private final SeaMap sea;
    private final Region[][] regions = new Region[REGIONS_X][];

    SeaGrid(SeaMap sea)
    {
        this.sea = sea;
    }

    @Override
    public boolean isWater(int x, int y)
    {
        return (info(x, y) & SeaMap.FAMILY_MASK) != 0;
    }

    @Override
    public WaterFamily family(int x, int y)
    {
        return WaterFamily.of(info(x, y) & SeaMap.FAMILY_MASK);
    }

    @Override
    public boolean walkBlocked(int x, int y)
    {
        return (info(x, y) & SeaMap.WALK_BLOCKED) != 0;
    }

    @Override
    public int clearance(int x, int y)
    {
        return info(x, y) >>> SeaMap.CLEARANCE_SHIFT;
    }

    @Override
    public int cells(int x, int y)
    {
        Region region = region(x, y);
        if (region == null)
        {
            return 0;
        }
        if (region.cells == null)
        {
            short[] shared = sea.regionCells(SeaMap.region(x, y));
            region.cells = shared == null ? NO_CELLS : shared;
        }
        return region.cells[SeaMap.index(x, y)] & SeaMap.ALL_CELLS;
    }

    @Override
    public boolean fullWater(int x, int y)
    {
        return cells(x, y) == SeaMap.ALL_CELLS;
    }

    @Override
    public boolean waterAt(int fineX, int fineY)
    {
        int column = (fineX >> CELL_SHIFT) & CELL_MASK;
        int row = (fineY >> CELL_SHIFT) & CELL_MASK;
        return (cells(fineX >> FINE_SHIFT, fineY >> FINE_SHIFT) & 1 << (row * SeaMap.CELLS + column)) != 0;
    }

    /**
     * Gets the cost the search filed for a tile
     * @param x Tile x
     * @param y Tile y
     * @return the cost, UNKNOWN_COST until it is filed
     */
    int tileCost(int x, int y)
    {
        Region region = region(x, y);
        return region == null || region.costs == null ? UNKNOWN_COST : region.costs[SeaMap.index(x, y)];
    }

    /**
     * Files a tile's cost
     * @param x Tile x
     * @param y Tile y
     * @param cost Cost to file
     */
    void tileCost(int x, int y, int cost)
    {
        Region region = region(x, y);
        if (region == null)
        {
            return;
        }
        if (region.costs == null)
        {
            region.costs = filled();
        }
        region.costs[SeaMap.index(x, y)] = cost;
    }

    /**
     * Gets the least cost the search reckons from a tile to an objective
     * @param objective Objective index
     * @param x Tile x
     * @param y Tile y
     * @return the cost, UNKNOWN_COST until it is worked out
     */
    int field(int objective, int x, int y)
    {
        Region region = region(x, y);
        return region == null || region.fields == null || region.fields[objective] == null ? UNKNOWN_COST : region.fields[objective][SeaMap.index(x, y)];
    }

    /**
     * Files the least cost reckoned from a tile to one of the search's objectives
     * @param objective Objective index
     * @param x Tile x
     * @param y Tile y
     * @param cost Cost to file
     */
    void field(int objective, int x, int y, int cost)
    {
        Region region = region(x, y);
        if (region == null)
        {
            return;
        }
        if (region.fields == null)
        {
            region.fields = new int[MOST_OBJECTIVES][];
        }
        if (region.fields[objective] == null)
        {
            region.fields[objective] = filled();
        }
        region.fields[objective][SeaMap.index(x, y)] = cost;
    }

    /**
     * Gets the slot the search gave a tile for its own per-tile tables
     * @param x Tile x
     * @param y Tile y
     * @return the slot, NO_SLOT until one is given
     */
    int slot(int x, int y)
    {
        Region region = region(x, y);
        return region == null || region.slots == null ? NO_SLOT : region.slots[SeaMap.index(x, y)];
    }

    /**
     * Gives a tile a slot
     * @param x Tile x
     * @param y Tile y
     * @param slot Slot to give
     */
    void slot(int x, int y, int slot)
    {
        Region region = region(x, y);
        if (region == null)
        {
            return;
        }
        if (region.slots == null)
        {
            region.slots = new int[TILES];
            Arrays.fill(region.slots, NO_SLOT);
        }
        region.slots[SeaMap.index(x, y)] = slot;
    }

    private static int[] filled()
    {
        int[] values = new int[TILES];
        Arrays.fill(values, UNKNOWN_COST);
        return values;
    }

    private int info(int x, int y)
    {
        Region region = region(x, y);
        return region == null ? 0 : region.info[SeaMap.index(x, y)] & 0xFF;
    }

    private Region region(int x, int y)
    {
        int regionX = x >> REGION_SHIFT;
        int regionY = y >> REGION_SHIFT;
        if (regionX < 0 || regionX >= REGIONS_X || regionY < 0 || regionY >= REGIONS_Y)
        {
            return null;
        }
        Region[] column = regions[regionX];
        if (column == null)
        {
            column = new Region[REGIONS_Y];
            regions[regionX] = column;
        }
        Region region = column[regionY];
        if (region == null)
        {
            byte[] info = sea.regionInfo(SeaMap.region(x, y));
            region = new Region(info == null ? NO_WATER : info);
            column[regionY] = region;
        }
        return region;
    }

    /**
     * One map region as the grid reads it
     */
    private static final class Region
    {
        private final byte[] info;
        private short[] cells;
        private int[] costs;
        private int[][] fields;
        private int[] slots;

        Region(byte[] info)
        {
            this.info = info;
        }
    }
}
