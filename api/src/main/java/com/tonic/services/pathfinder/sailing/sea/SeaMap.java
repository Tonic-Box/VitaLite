package com.tonic.services.pathfinder.sailing.sea;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
import net.runelite.api.coords.WorldPoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * What the world's water is like for a boat, tile by tile on the ground plane, dumped from the cache
 */
public final class SeaMap implements SeaView
{
    /**
     * Tiles along a map region's side
     */
    public static final int REGION_SIZE = 64;
    /**
     * The most clearance the map records
     */
    public static final int MOST_CLEARANCE = 7;
    static final int FAMILY_MASK = 0xF;
    static final int WALK_BLOCKED = 0x10;
    static final int CLEARANCE_SHIFT = 5;
    private static final int FINE_SHIFT = 7;
    private static final int CELL_SHIFT = 5;
    /**
     * Sub-cells along a tile's side
     */
    public static final int CELLS = 4;
    /**
     * The cell mask of a tile that is all water
     */
    public static final int ALL_CELLS = 0xFFFF;
    private static final int CELL_MASK = CELLS - 1;
    private static final int OLDEST_FALLBACK = 20;

    private static final int REGIONS = 1 << 16;
    private static final int TILES = REGION_SIZE * REGION_SIZE;

    private final byte[][] info = new byte[REGIONS][];
    private final Int2ObjectOpenHashMap<Short2ShortOpenHashMap> masks;
    private final AtomicReferenceArray<short[]> cells = new AtomicReferenceArray<>(REGIONS);
    private final int regionCount;
    private final List<Dock> docks;

    SeaMap(Int2ObjectOpenHashMap<byte[]> info, Int2ObjectOpenHashMap<Short2ShortOpenHashMap> masks, List<Dock> docks)
    {
        info.int2ObjectEntrySet().forEach(entry -> this.info[entry.getIntKey()] = entry.getValue());
        this.regionCount = info.size();
        this.masks = masks;
        this.docks = Collections.unmodifiableList(docks);
    }

    /**
     * Finds the newest revision at or below the given one that a bundled sea map exists for
     * @param revision Game revision
     * @return the revision found, or -1
     */
    public static int available(int revision)
    {
        for (int candidate = revision; candidate > revision - OLDEST_FALLBACK; candidate--)
        {
            if (SeaMap.class.getResource(resource(candidate)) != null)
            {
                return candidate;
            }
        }
        return -1;
    }

    /**
     * Reads the bundled sea map of a revision
     * @param revision Game revision
     * @return the sea map
     */
    public static SeaMap load(int revision) throws IOException
    {
        InputStream stream = SeaMap.class.getResourceAsStream(resource(revision));
        if (stream == null)
        {
            throw new IOException(resource(revision) + " not found on classpath");
        }
        try (stream)
        {
            return SeaCodec.read(stream);
        }
    }

    private static String resource(int revision)
    {
        return "/com/tonic/services/pathfinder/sea-" + revision + ".dat";
    }

    /**
     * Reads a sea map
     * @param stream Stream holding the map in VSEA form
     * @return the sea map
     */
    public static SeaMap load(InputStream stream) throws IOException
    {
        return SeaCodec.read(stream);
    }

    @Override
    public WaterFamily family(int x, int y)
    {
        return WaterFamily.of(infoAt(x, y) & FAMILY_MASK);
    }

    @Override
    public boolean isWater(int x, int y)
    {
        return (infoAt(x, y) & FAMILY_MASK) != 0;
    }

    @Override
    public boolean walkBlocked(int x, int y)
    {
        return (infoAt(x, y) & WALK_BLOCKED) != 0;
    }

    @Override
    public int clearance(int x, int y)
    {
        return (infoAt(x, y) & 0xFF) >>> CLEARANCE_SHIFT;
    }

    @Override
    public int cells(int x, int y)
    {
        if (!isWater(x, y))
        {
            return 0;
        }
        Short2ShortOpenHashMap partial = masks.get(region(x, y));
        if (partial == null)
        {
            return SeaMap.ALL_CELLS;
        }
        short index = (short) index(x, y);
        return partial.containsKey(index) ? partial.get(index) & SeaMap.ALL_CELLS : SeaMap.ALL_CELLS;
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
     * Gets every dock the map found a gangplank or mooring for
     * @return the docks
     */
    public List<Dock> docks()
    {
        return docks;
    }

    /**
     * Gets how many regions carry water
     * @return the region count
     */
    public int regionCount()
    {
        return regionCount;
    }

    private int infoAt(int x, int y)
    {
        byte[] tiles = regionInfo(region(x, y));
        return tiles == null ? 0 : tiles[index(x, y)];
    }

    /**
     * Gets a region's tile info bytes
     * @param region Region id
     * @return the info bytes, null for a region without water
     */
    byte[] regionInfo(int region)
    {
        return region >= 0 && region < REGIONS ? info[region] : null;
    }

    /**
     * Gets a region's cell masks by tile index, worked out once and shared
     * @param region Region id
     * @return the cell masks, null for a region without water
     */
    short[] regionCells(int region)
    {
        byte[] tiles = regionInfo(region);
        if (tiles == null)
        {
            return null;
        }
        short[] known = cells.get(region);
        if (known == null)
        {
            known = densify(tiles, masks.get(region));
            cells.set(region, known);
        }
        return known;
    }

    private static short[] densify(byte[] info, Short2ShortOpenHashMap partial)
    {
        short[] cells = new short[TILES];
        for (int index = 0; index < TILES; index++)
        {
            if ((info[index] & FAMILY_MASK) == 0)
            {
                continue;
            }
            short tile = (short) index;
            cells[index] = partial != null && partial.containsKey(tile) ? (short) (partial.get(tile) & ALL_CELLS) : (short) ALL_CELLS;
        }
        return cells;
    }

    static int region(int x, int y)
    {
        return (x >> 6) << 8 | (y >> 6);
    }

    static int index(int x, int y)
    {
        return (x & (REGION_SIZE - 1)) * REGION_SIZE + (y & (REGION_SIZE - 1));
    }

    /**
     * A dock's place in the world
     */
    public static final class Dock
    {
        private final int dockId;
        private final int locId;
        private final WorldPoint land;
        private final WorldPoint approach;

        public Dock(int dockId, int locId, WorldPoint land, WorldPoint approach)
        {
            this.dockId = dockId;
            this.locId = locId;
            this.land = land;
            this.approach = approach;
        }

        public int dockId()
        {
            return dockId;
        }

        public int locId()
        {
            return locId;
        }

        public WorldPoint land()
        {
            return land;
        }

        public WorldPoint approach()
        {
            return approach;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Dock))
            {
                return false;
            }
            Dock other = (Dock) o;
            return dockId == other.dockId && locId == other.locId && Objects.equals(land, other.land) && Objects.equals(approach, other.approach);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(dockId, locId, land, approach);
        }

        @Override
        public String toString()
        {
            return "Dock[" + "dockId=" + dockId + ", " + "locId=" + locId + ", " + "land=" + land + ", " + "approach=" + approach + "]";
        }
    }
}
