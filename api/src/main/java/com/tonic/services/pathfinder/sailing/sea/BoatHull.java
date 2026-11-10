package com.tonic.services.pathfinder.sailing.sea;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A boat's footprint on the water
 */
public final class BoatHull
{
    /**
     * Fine units along a sub-cell's side
     */
    public static final int CELL = 32;
    private static final int CELL_SHIFT = 5;
    private static final int TILE_SHIFT = Compass.FINE_SHIFT;
    static final int HALF_CELL = CELL / 2;
    private static final int HALF_TILE = 64;
    private static final int SAMPLE = 64;
    private static final int BOW_DEPTH = 128;
    private static final double[] SIN = new double[Compass.TURN];
    private static final double[] COS = new double[Compass.TURN];
    private static final int FOOTPRINT_CELLS = 4;

    static
    {
        for (int angle = 0; angle < Compass.TURN; angle++)
        {
            SIN[angle] = Math.sin(angle * (2 * Math.PI / Compass.TURN));
            COS[angle] = Math.cos(angle * (2 * Math.PI / Compass.TURN));
        }
    }

    private final int halfAcross;
    private final int halfAlong;
    private volatile Footprints footprints;

    public BoatHull(int halfAcross, int halfAlong)
    {
        this.halfAcross = requirePositive(halfAcross, "halfAcross");
        this.halfAlong = requirePositive(halfAlong, "halfAlong");
    }

    /**
     * Whether a deck tile is blocked
     */
    @FunctionalInterface
    public interface Deck
    {
        boolean blocked(int x, int y);
    }

    /**
     * Gets the hull the blocked tiles of a deck span about a pivot, symmetric about it; the deck's outer ring is ignored
     * @param sizeX Deck width in tiles
     * @param sizeY Deck length in tiles
     * @param pivotX Pivot x in fine units
     * @param pivotY Pivot y in fine units
     * @param deck Which deck tiles are blocked
     * @return the hull
     */
    public static BoatHull of(int sizeX, int sizeY, int pivotX, int pivotY, Deck deck)
    {
        if (deck == null)
        {
            throw new IllegalArgumentException("deck cannot be null");
        }
        int halfAcross = 0;
        int halfAlong = 0;
        for (int x = 1; x < sizeX - 1; x++)
        {
            for (int y = 1; y < sizeY - 1; y++)
            {
                if (deck.blocked(x, y))
                {
                    halfAcross = Math.max(halfAcross, Math.abs((x << TILE_SHIFT) + HALF_TILE - pivotX) + HALF_TILE);
                    halfAlong = Math.max(halfAlong, Math.abs((y << TILE_SHIFT) + HALF_TILE - pivotY) + HALF_TILE);
                }
            }
        }
        if (halfAcross == 0 || halfAlong == 0)
        {
            return new BoatHull((sizeX - 2) << (TILE_SHIFT - 1), (sizeY - 2) << (TILE_SHIFT - 1));
        }
        return new BoatHull(halfAcross, halfAlong);
    }

    public int halfAcross()
    {
        return halfAcross;
    }

    public int halfAlong()
    {
        return halfAlong;
    }

    /**
     * Gets the least clearance a pivot's tile can have for the hull to be sure to fit at any pose on it
     * @return the clearance needed
     */
    public int clearanceToFit()
    {
        int reach = (int) Math.ceil(Math.hypot(halfAcross, halfAlong));
        return ((reach + HALF_TILE) >> TILE_SHIFT) + 1;
    }

    /**
     * Checks whether every cell of the hull the rule tests is water the rule allows
     * @param sea Sea map
     * @param pose Boat pose
     * @param rule Hull rule
     * @return true if the hull fits
     */
    public boolean fits(SeaMap sea, BoatPose pose, HullRule rule)
    {
        return fits(sea, pose.fineX(), pose.fineY(), pose.angle(), rule);
    }

    /**
     * Checks whether every cell of the hull the rule tests is water the rule allows
     * @param sea Sea view
     * @param fineX Pivot fine x
     * @param fineY Pivot fine y
     * @param angle Angle the boat faces
     * @param rule Hull rule
     * @return true if the hull fits
     */
    public boolean fits(SeaView sea, int fineX, int fineY, int angle, HullRule rule)
    {
        angle &= Compass.ANGLE_MASK;
        int offsetX = fineX & (CELL - 1);
        int offsetY = fineY & (CELL - 1);
        int[] footprint = (offsetX | offsetY) == 0 ? alignedFootprint(angle, rule) : footprint(offsetX, offsetY, angle, rule);
        int cellX = fineX >> CELL_SHIFT;
        int cellY = fineY >> CELL_SHIFT;
        int cellsPerTile = TILE_SHIFT - CELL_SHIFT;
        if (openWater(sea, (cellX + footprint[0]) >> cellsPerTile, (cellY + footprint[2]) >> cellsPerTile, (cellX + footprint[1]) >> cellsPerTile, (cellY + footprint[3]) >> cellsPerTile, rule))
        {
            return true;
        }
        for (int at = FOOTPRINT_CELLS; at < footprint.length; at += 2)
        {
            if (!clear(sea, ((cellX + footprint[at]) << CELL_SHIFT) + HALF_CELL, ((cellY + footprint[at + 1]) << CELL_SHIFT) + HALF_CELL, rule))
            {
                return false;
            }
        }
        return true;
    }

    private int[] alignedFootprint(int angle, HullRule rule)
    {
        Footprints known = footprints;
        if (known == null || !known.rule.equals(rule))
        {
            known = new Footprints(rule);
            footprints = known;
        }
        int[] footprint = known.byAngle.get(angle);
        if (footprint == null)
        {
            footprint = footprint(0, 0, angle, rule);
            known.byAngle.set(angle, footprint);
        }
        return footprint;
    }

    /**
     * Gets the cells the rule tests for a pivot at an offset within its cell, as cell steps from the pivot's cell after the bounding box's first and last steps along x and y
     * @param offsetX Pivot x offset within its cell
     * @param offsetY Pivot y offset within its cell
     * @param angle Angle the boat faces
     * @param rule Hull rule
     * @return the bounding box steps followed by the cell steps
     */
    private int[] footprint(int offsetX, int offsetY, int angle, HullRule rule)
    {
        int across = Math.max(1, halfAcross - rule.inset());
        int along = Math.max(1, halfAlong - rule.inset());
        double sin = SIN[angle];
        double cos = COS[angle];
        int reachX = (int) Math.ceil(Math.abs(sin) * along + Math.abs(cos) * across);
        int reachY = (int) Math.ceil(Math.abs(cos) * along + Math.abs(sin) * across);
        int firstCellX = (offsetX - reachX) >> CELL_SHIFT;
        int lastCellX = (offsetX + reachX) >> CELL_SHIFT;
        int firstCellY = (offsetY - reachY) >> CELL_SHIFT;
        int lastCellY = (offsetY + reachY) >> CELL_SHIFT;
        IntArrayList cells = new IntArrayList();
        cells.add(firstCellX);
        cells.add(lastCellX);
        cells.add(firstCellY);
        cells.add(lastCellY);
        int nearestAlong = rule.wholeRectangle() ? -along : along - BOW_DEPTH;
        for (int cellY = firstCellY; cellY <= lastCellY; cellY++)
        {
            for (int cellX = firstCellX; cellX <= lastCellX; cellX++)
            {
                double rx = (cellX << CELL_SHIFT) + HALF_CELL - offsetX;
                double ry = (cellY << CELL_SHIFT) + HALF_CELL - offsetY;
                double cellAlong = -rx * sin - ry * cos;
                double cellAcross = rx * cos - ry * sin;
                if (cellAlong < nearestAlong || cellAlong > along || Math.abs(cellAcross) > across)
                {
                    continue;
                }
                cells.add(cellX);
                cells.add(cellY);
            }
        }
        return cells.toIntArray();
    }

    /**
     * Gets the cells the rule tests that are not water it allows
     * @param sea Sea map
     * @param pose Boat pose
     * @param rule Hull rule
     * @return the offending cells
     */
    public List<Cell> offendingCells(SeaMap sea, BoatPose pose, HullRule rule)
    {
        return offendingCells(sea, pose.fineX(), pose.fineY(), pose.angle(), rule);
    }

    /**
     * Gets the offending cells, in cell units of 32 fine, nearest the stern first
     * @param sea Sea view
     * @param fineX Pivot fine x
     * @param fineY Pivot fine y
     * @param angle Angle the boat faces
     * @param rule Hull rule
     * @return the offending cells
     */
    public List<Cell> offendingCells(SeaView sea, int fineX, int fineY, int angle, HullRule rule)
    {
        List<Cell> offenders = new ArrayList<>();
        offenders(sea, fineX, fineY, angle, rule, offenders);
        return offenders;
    }

    /**
     * Counts how many cells the rule tests are not water it allows
     * @param sea Sea view
     * @param fineX Pivot fine x
     * @param fineY Pivot fine y
     * @param angle Angle the boat faces
     * @param rule Hull rule
     * @return the offending cell count, 0 when the hull fits
     */
    public int offences(SeaView sea, int fineX, int fineY, int angle, HullRule rule)
    {
        return offenders(sea, fineX, fineY, angle, rule, null);
    }

    private int offenders(SeaView sea, int fineX, int fineY, int angle, HullRule rule, List<Cell> into)
    {
        angle &= Compass.ANGLE_MASK;
        int across = Math.max(1, halfAcross - rule.inset());
        int along = Math.max(1, halfAlong - rule.inset());
        double sin = SIN[angle];
        double cos = COS[angle];
        int reachX = (int) Math.ceil(Math.abs(sin) * along + Math.abs(cos) * across);
        int reachY = (int) Math.ceil(Math.abs(cos) * along + Math.abs(sin) * across);
        int nearestAlong = rule.wholeRectangle() ? -along : along - BOW_DEPTH;
        int count = 0;
        for (int cellY = (fineY - reachY) >> CELL_SHIFT; cellY <= (fineY + reachY) >> CELL_SHIFT; cellY++)
        {
            for (int cellX = (fineX - reachX) >> CELL_SHIFT; cellX <= (fineX + reachX) >> CELL_SHIFT; cellX++)
            {
                int centreX = (cellX << CELL_SHIFT) + HALF_CELL;
                int centreY = (cellY << CELL_SHIFT) + HALF_CELL;
                double rx = centreX - fineX;
                double ry = centreY - fineY;
                double cellAlong = -rx * sin - ry * cos;
                double cellAcross = rx * cos - ry * sin;
                if (cellAlong >= nearestAlong && cellAlong <= along && Math.abs(cellAcross) <= across && !clear(sea, centreX, centreY, rule))
                {
                    count++;
                    if (into != null)
                    {
                        into.add(new Cell(cellX, cellY));
                    }
                }
            }
        }
        return count;
    }

    /**
     * Checks whether the hull fits at every sample of the move, 64 fine units apart, turning along the shortest arc
     * @param sea Sea map
     * @param from Pose moved from
     * @param to Pose moved to
     * @param rule Hull rule
     * @return true if the hull fits along the whole move
     */
    public boolean sweptFits(SeaMap sea, BoatPose from, BoatPose to, HullRule rule)
    {
        int dx = to.fineX() - from.fineX();
        int dy = to.fineY() - from.fineY();
        int arc = Compass.shortestArc(from.angle(), to.angle());
        int samples = Math.max(1, Math.max((Math.max(Math.abs(dx), Math.abs(dy)) + SAMPLE - 1) / SAMPLE, (Math.abs(arc) + SAMPLE - 1) / SAMPLE));
        for (int i = 1; i <= samples; i++)
        {
            int x = from.fineX() + dx * i / samples;
            int y = from.fineY() + dy * i / samples;
            int angle = from.angle() + arc * i / samples;
            if (!fits(sea, x, y, angle, rule))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the ground tiles any cell of the hull lies on, nearest the stern first
     * @param pose Boat pose
     * @return the tiles under the hull
     */
    public List<WorldPoint> tiles(BoatPose pose)
    {
        int angle = pose.angle();
        double sin = SIN[angle];
        double cos = COS[angle];
        int reachX = (int) Math.ceil(Math.abs(sin) * halfAlong + Math.abs(cos) * halfAcross);
        int reachY = (int) Math.ceil(Math.abs(cos) * halfAlong + Math.abs(sin) * halfAcross);
        Set<WorldPoint> tiles = new LinkedHashSet<>();
        for (int cellY = (pose.fineY() - reachY) >> CELL_SHIFT; cellY <= (pose.fineY() + reachY) >> CELL_SHIFT; cellY++)
        {
            for (int cellX = (pose.fineX() - reachX) >> CELL_SHIFT; cellX <= (pose.fineX() + reachX) >> CELL_SHIFT; cellX++)
            {
                double rx = (cellX << CELL_SHIFT) + HALF_CELL - pose.fineX();
                double ry = (cellY << CELL_SHIFT) + HALF_CELL - pose.fineY();
                double along = -rx * sin - ry * cos;
                double across = rx * cos - ry * sin;
                if (Math.abs(along) <= halfAlong && Math.abs(across) <= halfAcross)
                {
                    tiles.add(new WorldPoint(cellX >> (TILE_SHIFT - CELL_SHIFT), cellY >> (TILE_SHIFT - CELL_SHIFT), 0));
                }
            }
        }
        return new ArrayList<>(tiles);
    }

    /**
     * Checks whether every tile the hull's bounding box touches is water right across and nothing the rule stops at
     * @param sea Sea view
     * @param firstTileX First tile x of the box
     * @param firstTileY First tile y of the box
     * @param lastTileX Last tile x of the box
     * @param lastTileY Last tile y of the box
     * @param rule Hull rule
     * @return true if the whole box is open water
     */
    private static boolean openWater(SeaView sea, int firstTileX, int firstTileY, int lastTileX, int lastTileY, HullRule rule)
    {
        for (int tileY = firstTileY; tileY <= lastTileY; tileY++)
        {
            for (int tileX = firstTileX; tileX <= lastTileX; tileX++)
            {
                if (!sea.fullWater(tileX, tileY) || rule.walkBlockedBlocks() && sea.walkBlocked(tileX, tileY))
                {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean clear(SeaView sea, int fineX, int fineY, HullRule rule)
    {
        int tileX = fineX >> TILE_SHIFT;
        int tileY = fineY >> TILE_SHIFT;
        if (rule.subCells() ? !sea.waterAt(fineX, fineY) : !sea.fullWater(tileX, tileY))
        {
            return false;
        }
        return !rule.walkBlockedBlocks() || !sea.walkBlocked(tileX, tileY);
    }

    private static int requirePositive(int value, String name)
    {
        if (value <= 0)
        {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    @Override
    public String toString()
    {
        return "hull " + 2 * halfAcross + "x" + 2 * halfAlong;
    }

    /**
     * The footprints worked out for a rule, by angle, for pivots on a cell's corner
     */
    private static final class Footprints
    {
        private final HullRule rule;
        private final AtomicReferenceArray<int[]> byAngle = new AtomicReferenceArray<>(Compass.TURN);

        Footprints(HullRule rule)
        {
            this.rule = rule;
        }
    }

    /**
     * A sub-cell of the sea, a quarter tile square
     */
    public static final class Cell
    {
        private final int cellX;
        private final int cellY;

        public Cell(int cellX, int cellY)
        {
            this.cellX = cellX;
            this.cellY = cellY;
        }

        /**
         * Gets the cell's west edge in world tiles
         * @return the west edge
         */
        public double tileX()
        {
            return cellX / 4.0;
        }

        /**
         * Gets the cell's south edge in world tiles
         * @return the south edge
         */
        public double tileY()
        {
            return cellY / 4.0;
        }

        public int cellX()
        {
            return cellX;
        }

        public int cellY()
        {
            return cellY;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Cell))
            {
                return false;
            }
            Cell other = (Cell) o;
            return cellX == other.cellX && cellY == other.cellY;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(cellX, cellY);
        }

        @Override
        public String toString()
        {
            return "Cell[" + "cellX=" + cellX + ", " + "cellY=" + cellY + "]";
        }
    }
}
