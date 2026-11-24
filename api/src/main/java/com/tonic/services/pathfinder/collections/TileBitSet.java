package com.tonic.services.pathfinder.collections;

import com.tonic.util.WorldPointUtil;

import java.util.Arrays;

/**
 * A set of tiles a search marks, a bit a tile, kept a 64x64 map square at a time; clearing it is a counter's increment, and a square is wiped when it is first written after.
 */
public final class TileBitSet
{
    private static final int SQUARE_SHIFT = 6;
    private static final int SQUARE_MASK = (1 << SQUARE_SHIFT) - 1;
    private static final int WORDS = (1 << (SQUARE_SHIFT * 2)) / Long.SIZE;
    private static final int SQUARES_Y = (0x7FFF >> SQUARE_SHIFT) + 1;
    private static final int SQUARES = ((0x3FFF >> SQUARE_SHIFT) + 1) * SQUARES_Y;
    private static final int PLANES = 8;

    private static final class Square
    {
        final long[] words = new long[WORDS];
        int generation;
    }

    private Square[][] planes = new Square[PLANES][];
    private int generation = 1;

    /**
     * Marks a tile.
     *
     * @param tile the tile's key
     */
    public void add(int tile)
    {
        int planeIndex = WorldPointUtil.getCompressedPlane(tile);
        Square[] plane = planes[planeIndex];
        if (plane == null)
        {
            plane = new Square[SQUARES];
            planes[planeIndex] = plane;
        }
        int squareIndex = square(tile);
        Square square = plane[squareIndex];
        if (square == null)
        {
            square = new Square();
            plane[squareIndex] = square;
        }
        if (square.generation != generation)
        {
            Arrays.fill(square.words, 0L);
            square.generation = generation;
        }
        int bit = bit(tile);
        square.words[bit >>> 6] |= 1L << bit;
    }

    /**
     * @param tile a tile's key
     * @return whether it is marked
     */
    public boolean contains(int tile)
    {
        Square[] plane = planes[WorldPointUtil.getCompressedPlane(tile)];
        if (plane == null)
        {
            return false;
        }
        Square square = plane[square(tile)];
        if (square == null || square.generation != generation)
        {
            return false;
        }
        int bit = bit(tile);
        return (square.words[bit >>> 6] & 1L << bit) != 0;
    }

    /**
     * Unmarks every tile, keeping the squares to be reused.
     */
    public void clear()
    {
        if (generation == Integer.MAX_VALUE)
        {
            planes = new Square[PLANES][];
            generation = 0;
        }
        generation++;
    }

    private static int square(int tile)
    {
        return (WorldPointUtil.getCompressedX(tile) >> SQUARE_SHIFT) * SQUARES_Y + (WorldPointUtil.getCompressedY(tile) >> SQUARE_SHIFT);
    }

    private static int bit(int tile)
    {
        return (WorldPointUtil.getCompressedX(tile) & SQUARE_MASK) << SQUARE_SHIFT | (WorldPointUtil.getCompressedY(tile) & SQUARE_MASK);
    }
}
