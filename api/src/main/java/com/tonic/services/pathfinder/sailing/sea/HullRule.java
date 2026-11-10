package com.tonic.services.pathfinder.sailing.sea;

import java.util.Objects;

/**
 * Which of the sea map's obstacles stop a hull
 */
public final class HullRule
{
    private final boolean walkBlockedBlocks;
    private final boolean subCells;
    private final boolean wholeRectangle;
    private final int inset;

    /**
     * The strictest reading
     */
    public static final HullRule STRICT = new HullRule(true, true, true, 0);
    /**
     * What the coast-hugging voyage west of Port Sarim showed the server does
     */
    public static final HullRule CALIBRATED = new HullRule(false, true, true, BoatHull.CELL);

    public HullRule(boolean walkBlockedBlocks, boolean subCells, boolean wholeRectangle, int inset)
    {
        if (inset < 0)
        {
            throw new IllegalArgumentException("inset cannot be negative");
        }
        this.walkBlockedBlocks = walkBlockedBlocks;
        this.subCells = subCells;
        this.wholeRectangle = wholeRectangle;
        this.inset = inset;
    }

    public boolean walkBlockedBlocks()
    {
        return walkBlockedBlocks;
    }

    public boolean subCells()
    {
        return subCells;
    }

    public boolean wholeRectangle()
    {
        return wholeRectangle;
    }

    public int inset()
    {
        return inset;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof HullRule))
        {
            return false;
        }
        HullRule other = (HullRule) o;
        return walkBlockedBlocks == other.walkBlockedBlocks && subCells == other.subCells && wholeRectangle == other.wholeRectangle && inset == other.inset;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(walkBlockedBlocks, subCells, wholeRectangle, inset);
    }

    @Override
    public String toString()
    {
        return "HullRule[" + "walkBlockedBlocks=" + walkBlockedBlocks + ", " + "subCells=" + subCells + ", " + "wholeRectangle=" + wholeRectangle + ", " + "inset=" + inset + "]";
    }
}
