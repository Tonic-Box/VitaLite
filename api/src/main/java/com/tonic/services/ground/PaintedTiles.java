package com.tonic.services.ground;

import com.tonic.services.streamer.MirrorHiding;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * A set of the 32 pixel tiles of an image, found by scanning it for pixels other than 0 or marked from areas, and drawn as runs of neighbouring tiles along each row of tiles.
 */
final class PaintedTiles
{
    private static final int TILE = 32;

    private boolean[] tiles = new boolean[0];
    private int width;
    private int height;
    private int tilesWide;
    private int tilesHigh;
    private int[] runs = new int[64];
    private int count;
    private boolean stale;

    /**
     * Empties the set, sized for an image.
     *
     * @param imageWidth the image's width
     * @param imageHeight the image's height
     */
    void clear(int imageWidth, int imageHeight)
    {
        width = imageWidth;
        height = imageHeight;
        tilesWide = (width + TILE - 1) / TILE;
        tilesHigh = (height + TILE - 1) / TILE;
        int size = tilesWide * tilesHigh;
        if (tiles.length < size)
        {
            tiles = new boolean[size];
        }
        else
        {
            Arrays.fill(tiles, 0, size, false);
        }
        stale = true;
    }

    /**
     * Sets the set to the tiles of an image that hold any pixel other than 0.
     *
     * @param image an image backed by an int buffer
     */
    void scan(BufferedImage image)
    {
        clear(image.getWidth(), image.getHeight());
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int tileY = 0; tileY < tilesHigh; tileY++)
        {
            int top = tileY * TILE;
            int bottom = Math.min(height, top + TILE);
            for (int tileX = 0; tileX < tilesWide; tileX++)
            {
                int left = tileX * TILE;
                tiles[tileY * tilesWide + tileX] = painted(pixels, left, top, Math.min(width, left + TILE), bottom);
            }
        }
    }

    /**
     * Adds the tiles an area overlaps.
     *
     * @param left the area's left
     * @param top its top
     * @param right one past its right
     * @param bottom one past its bottom
     */
    void mark(int left, int top, int right, int bottom)
    {
        int fromX = Math.max(0, left) / TILE;
        int fromY = Math.max(0, top) / TILE;
        int toX = Math.min(width, right) - 1;
        int toY = Math.min(height, bottom) - 1;
        if (toX < 0 || toY < 0)
        {
            return;
        }
        for (int tileY = fromY; tileY <= toY / TILE; tileY++)
        {
            for (int tileX = fromX; tileX <= toX / TILE; tileX++)
            {
                tiles[tileY * tilesWide + tileX] = true;
            }
        }
        stale = true;
    }

    /**
     * Adds every tile of another set of the same size.
     *
     * @param other the other set
     */
    void include(PaintedTiles other)
    {
        int size = tilesWide * tilesHigh;
        for (int tile = 0; tile < size; tile++)
        {
            tiles[tile] |= other.tiles[tile];
        }
        stale = true;
    }

    /**
     * @return whether the set holds no tile
     */
    boolean isEmpty()
    {
        build();
        return count == 0;
    }

    /**
     * Has the draw open on the client frame save the set's tiles before they are drawn on.
     */
    void touch()
    {
        build();
        for (int run = 0; run < count; run += 4)
        {
            MirrorHiding.touch(runs[run], runs[run + 1], runs[run + 2], runs[run + 3]);
        }
    }

    /**
     * Draws the set's tiles of an image over graphics with no transform or clip.
     *
     * @param out the graphics
     * @param image the image, the size the set was made for
     */
    void draw(Graphics2D out, BufferedImage image)
    {
        build();
        for (int run = 0; run < count; run += 4)
        {
            int left = runs[run];
            int top = runs[run + 1];
            int right = runs[run + 2];
            int bottom = runs[run + 3];
            out.drawImage(image, left, top, right, bottom, left, top, right, bottom, null);
        }
    }

    private void build()
    {
        if (!stale)
        {
            return;
        }
        stale = false;
        count = 0;
        for (int tileY = 0; tileY < tilesHigh; tileY++)
        {
            int start = -1;
            for (int tileX = 0; tileX <= tilesWide; tileX++)
            {
                boolean in = tileX < tilesWide && tiles[tileY * tilesWide + tileX];
                if (in && start < 0)
                {
                    start = tileX;
                }
                else if (!in && start >= 0)
                {
                    add(start * TILE, tileY * TILE, Math.min(width, tileX * TILE), Math.min(height, (tileY + 1) * TILE));
                    start = -1;
                }
            }
        }
    }

    private void add(int left, int top, int right, int bottom)
    {
        if (count + 4 > runs.length)
        {
            runs = Arrays.copyOf(runs, runs.length * 2);
        }
        runs[count] = left;
        runs[count + 1] = top;
        runs[count + 2] = right;
        runs[count + 3] = bottom;
        count += 4;
    }

    private boolean painted(int[] pixels, int left, int top, int right, int bottom)
    {
        for (int y = top; y < bottom; y++)
        {
            int end = y * width + right;
            for (int index = y * width + left; index < end; index++)
            {
                if (pixels[index] != 0)
                {
                    return true;
                }
            }
        }
        return false;
    }
}
