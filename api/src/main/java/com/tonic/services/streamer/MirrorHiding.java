package com.tonic.services.streamer;

import com.tonic.Static;
import com.tonic.services.profiler.frames.FrameProfiler;
import net.runelite.api.BufferProvider;
import net.runelite.api.Client;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Keeps the mirror's frame apart from the real one on the 32 pixel tiles where they differ. A hidden draw saves each tile before and after it draws; a mirror-only draw draws onto what the mirror shows and puts the real pixels back. When the frame is handed to the mirror, each saved pixel shows the mirror's value unless something drew over it since. Draws go to the client's frame, or to the image the scene layer's overlays are deferred into while the ground is drawn under objects. Runs on the client thread only.
 */
public final class MirrorHiding
{
    private static final Tiles frame = new Tiles();
    private static final Tiles deferred = new Tiles();
    private static final int TILE_SAVE = FrameProfiler.timer("mirror.tileSave");
    private static final int TILE_END = FrameProfiler.timer("mirror.tileEnd");
    private static final int TILES = FrameProfiler.counter("mirror.tilesPerFrame");

    private static Tiles current = frame;

    private MirrorHiding()
    {
    }

    /**
     * Starts drawing content that must stay out of the mirror.
     *
     * @param graphics the graphics it draws with
     * @return graphics that save each tile before it is drawn on
     */
    public static Graphics2D hide(Graphics2D graphics)
    {
        return hide(graphics, null);
    }

    /**
     * Starts drawing content that only the mirror shows.
     *
     * @param graphics the graphics it draws with
     * @return graphics that draw onto what the mirror shows, or null when there is nothing to draw onto
     */
    public static Graphics2D mirror(Graphics2D graphics)
    {
        return open(graphics, true, null, null);
    }

    /**
     * Draws content that must stay out of the mirror.
     *
     * @param graphics the graphics it draws with
     * @param drawing the drawing, given graphics that save each tile before it is drawn on
     */
    public static void drawHidden(Graphics2D graphics, Consumer<Graphics2D> drawing)
    {
        try
        {
            drawing.accept(hide(graphics));
        }
        finally
        {
            end();
        }
    }

    /**
     * Draws content that only the mirror shows, or nothing when there is nothing to draw onto.
     *
     * @param graphics the graphics it draws with
     * @param drawing the drawing, given graphics that draw onto what the mirror shows
     */
    public static void drawMirrorOnly(Graphics2D graphics, Consumer<Graphics2D> drawing)
    {
        Graphics2D onto = mirror(graphics);
        if (onto == null)
        {
            return;
        }
        try
        {
            drawing.accept(onto);
        }
        finally
        {
            end();
        }
    }

    /**
     * Starts a hidden draw over part of the client frame, for content drawn straight into its pixels.
     *
     * @param area the frame pixels the draw may cover, or null for the whole frame
     */
    public static void hideFrame(Rectangle area)
    {
        startFrame(area, false);
    }

    /**
     * Starts a hidden draw into the client frame that saves only the tiles passed to touch.
     */
    public static void hideFrame()
    {
        startFrame(false);
    }

    /**
     * Starts a mirror-only draw over part of the client frame, for content drawn straight into its pixels.
     *
     * @param area the frame pixels the draw may cover, or null for the whole frame
     */
    public static void mirrorFrame(Rectangle area)
    {
        startFrame(area, true);
    }

    /**
     * Starts a mirror-only draw into the client frame that draws only on the tiles passed to touch.
     */
    public static void mirrorFrame()
    {
        startFrame(true);
    }

    /**
     * Saves the tiles a draw open on the client frame is about to change, before it changes them.
     *
     * @param left the left of the pixels about to change
     * @param top their top
     * @param right one past their right
     * @param bottom one past their bottom
     */
    public static void touch(int left, int top, int right, int bottom)
    {
        long began = FrameProfiler.begin();
        current.touch(left, top, right, bottom);
        FrameProfiler.end(TILE_SAVE, began);
    }

    private static void startFrame(Rectangle area, boolean onlyMirror)
    {
        if (startFrame(onlyMirror))
        {
            if (area == null)
            {
                touch(0, 0, frame.width, frame.height);
            }
            else
            {
                touch(area);
            }
        }
    }

    private static boolean startFrame(boolean onlyMirror)
    {
        end();
        current = frame;
        if (!bindCurrent())
        {
            return false;
        }
        frame.start(null, onlyMirror, null);
        return true;
    }

    /**
     * Sends the draws that follow into the image the scene layer's overlays are deferred into, until the deferred layer is taken.
     *
     * @param image the deferred image, of type TYPE_INT_ARGB_PRE
     */
    public static void deferTo(BufferedImage image)
    {
        end();
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        deferred.bind(pixels, image.getWidth(), image.getHeight());
        deferred.forget();
        current = deferred;
    }

    /**
     * Ends drawing into the deferred image and writes what the mirror shows of it into another image, then sends draws to the client frame again.
     *
     * @param into an image of the same size and type as the deferred image
     * @param differing given each tile where what the mirror shows may differ from the deferred image, as an area of it
     * @return whether a deferred layer was taken
     */
    public static boolean takeDeferred(BufferedImage into, Areas differing)
    {
        end();
        boolean taken = current == deferred && into.getWidth() == deferred.width && into.getHeight() == deferred.height;
        if (taken)
        {
            int[] copy = ((DataBufferInt) into.getRaster().getDataBuffer()).getData();
            System.arraycopy(deferred.pixels, 0, copy, 0, deferred.width * deferred.height);
            deferred.apply(copy, deferred.width, deferred.height);
            deferred.report(differing);
        }
        deferred.forget();
        current = frame;
        return taken;
    }

    /**
     * @return how text with the player's name is drawn now: as it is while a hidden draw is open, replaced while a mirror-only draw is open, and split into a real and a mirror draw otherwise
     */
    static int openDraw()
    {
        if (!current.recording)
        {
            return StreamerName.SPLIT;
        }
        return current.mirrorOnly ? StreamerName.REPLACED : StreamerName.DRAW;
    }

    /**
     * Ends the draw started last. A hidden draw saves how it left each tile it drew on; a mirror-only draw keeps what it drew for the mirror and puts the real pixels back.
     */
    public static void end()
    {
        long began = FrameProfiler.begin();
        current.end();
        FrameProfiler.end(TILE_END, began);
    }

    static Graphics2D hide(Graphics2D graphics, DrawingLog log)
    {
        return open(graphics, false, log, graphics);
    }

    private static Graphics2D open(Graphics2D graphics, boolean onlyMirror, DrawingLog log, Graphics2D unbound)
    {
        end();
        return bindCurrent() ? current.start(graphics, onlyMirror, log) : unbound;
    }

    /**
     * Forgets every saved tile and any draw in progress, as when streamer mode starts.
     */
    static void reset()
    {
        frame.forget();
        deferred.forget();
        current = frame;
    }

    /**
     * Saves every tile the current draw is about to cover, unless the draw already saved it.
     *
     * @param area the device pixels about to be drawn
     */
    static void touch(Rectangle area)
    {
        touch(area.x, area.y, (int) Math.min(Integer.MAX_VALUE, (long) area.x + area.width), (int) Math.min(Integer.MAX_VALUE, (long) area.y + area.height));
    }

    /**
     * Writes into a copy of the client frame what the mirror shows on each saved tile, then forgets the tiles for the next frame.
     *
     * @param copy a copy of the frame's pixels, the same size as the frame
     * @param copyWidth the frame's width
     * @param copyHeight the frame's height
     */
    static void apply(int[] copy, int copyWidth, int copyHeight)
    {
        end();
        FrameProfiler.count(TILES, frame.slotsUsed);
        frame.apply(copy, copyWidth, copyHeight);
        frame.forget();
    }

    private static boolean bindCurrent()
    {
        if (current == deferred)
        {
            return true;
        }
        Client client = Static.getClient();
        BufferProvider provider = client == null ? null : client.getBufferProvider();
        return provider != null && frame.bind(provider.getPixels(), provider.getWidth(), provider.getHeight());
    }

    /**
     * Takes areas of an image, in pixels.
     */
    @FunctionalInterface
    public interface Areas
    {
        /**
         * @param left the area's left
         * @param top its top
         * @param right one past its right
         * @param bottom one past its bottom
         */
        void add(int left, int top, int right, int bottom);
    }

    /**
     * The saved tiles of one image.
     */
    private static final class Tiles
    {
        private static final int TILE = 32;
        private static final int TILE_AREA = TILE * TILE;

        private int[] pixels;
        private int width;
        private int height;
        private int tilesWide;
        private int[] tileSlots = new int[0];
        private int[] slotTiles = new int[0];
        private int[][] before = new int[0][];
        private int[][] after = new int[0][];
        private int[][] real = new int[0][];
        private boolean[] drawing = new boolean[0];
        private int[] drawingSlots = new int[0];
        private int slotsUsed;
        private int drawingCount;
        private boolean recording;
        private boolean mirrorOnly;

        private boolean bind(int[] imagePixels, int imageWidth, int imageHeight)
        {
            if (imagePixels == null || imageWidth <= 0 || imageHeight <= 0 || imagePixels.length < imageWidth * imageHeight)
            {
                return false;
            }
            if (imagePixels != pixels || imageWidth != width || imageHeight != height)
            {
                pixels = imagePixels;
                width = imageWidth;
                height = imageHeight;
                tilesWide = (width + TILE - 1) / TILE;
                int tiles = tilesWide * ((height + TILE - 1) / TILE);
                tileSlots = new int[tiles];
                Arrays.fill(tileSlots, -1);
                slotsUsed = 0;
                drawingCount = 0;
                if (drawing.length < tiles)
                {
                    slotTiles = Arrays.copyOf(slotTiles, tiles);
                    drawing = new boolean[tiles];
                    drawingSlots = new int[tiles];
                }
            }
            return true;
        }

        private Graphics2D start(Graphics2D graphics, boolean onlyMirror, DrawingLog log)
        {
            recording = true;
            mirrorOnly = onlyMirror;
            return graphics == null ? null : new RecordingGraphics(graphics, false, log);
        }

        private void end()
        {
            if (!recording)
            {
                return;
            }
            recording = false;
            for (int i = 0; i < drawingCount; i++)
            {
                int slot = drawingSlots[i];
                if (mirrorOnly)
                {
                    copyTile(slotTiles[slot], pixels, before[slot], false);
                    copyTile(slotTiles[slot], real[slot], pixels, true);
                    System.arraycopy(real[slot], 0, after[slot], 0, TILE_AREA);
                }
                else
                {
                    copyTile(slotTiles[slot], pixels, after[slot], false);
                }
                drawing[slot] = false;
            }
            drawingCount = 0;
            mirrorOnly = false;
        }

        private void forget()
        {
            for (int i = 0; i < drawingCount; i++)
            {
                drawing[drawingSlots[i]] = false;
            }
            drawingCount = 0;
            for (int slot = 0; slot < slotsUsed; slot++)
            {
                tileSlots[slotTiles[slot]] = -1;
            }
            slotsUsed = 0;
            recording = false;
            mirrorOnly = false;
        }

        private void touch(int areaLeft, int areaTop, int areaRight, int areaBottom)
        {
            if (!recording)
            {
                return;
            }
            int left = Math.max(0, areaLeft);
            int top = Math.max(0, areaTop);
            int right = Math.min(width, areaRight);
            int bottom = Math.min(height, areaBottom);
            if (left >= right || top >= bottom)
            {
                return;
            }
            for (int tileY = top / TILE; tileY <= (bottom - 1) / TILE; tileY++)
            {
                for (int tileX = left / TILE; tileX <= (right - 1) / TILE; tileX++)
                {
                    int tile = tileY * tilesWide + tileX;
                    int slot = tileSlots[tile];
                    boolean claimed = slot < 0;
                    if (claimed)
                    {
                        slot = claim(tile);
                        copyTile(tile, pixels, before[slot], false);
                    }
                    else if (!drawing[slot])
                    {
                        resolveInto(slot, null);
                    }
                    if (!drawing[slot])
                    {
                        drawing[slot] = true;
                        drawingSlots[drawingCount++] = slot;
                        if (mirrorOnly)
                        {
                            copyTile(tile, pixels, real[slot], false);
                            if (!claimed)
                            {
                                copyTile(tile, before[slot], pixels, true);
                            }
                        }
                    }
                }
            }
        }

        private void apply(int[] copy, int copyWidth, int copyHeight)
        {
            if (copyWidth != width || copyHeight != height)
            {
                return;
            }
            for (int slot = 0; slot < slotsUsed; slot++)
            {
                resolveInto(slot, copy);
            }
        }

        private void report(Areas areas)
        {
            for (int slot = 0; slot < slotsUsed; slot++)
            {
                int tile = slotTiles[slot];
                int x0 = (tile % tilesWide) * TILE;
                int y0 = (tile / tilesWide) * TILE;
                areas.add(x0, y0, Math.min(width, x0 + TILE), Math.min(height, y0 + TILE));
            }
        }

        private int claim(int tile)
        {
            int slot = slotsUsed++;
            if (slot == before.length)
            {
                int grown = Math.max(16, slot * 2);
                before = Arrays.copyOf(before, grown);
                after = Arrays.copyOf(after, grown);
                real = Arrays.copyOf(real, grown);
            }
            if (before[slot] == null)
            {
                before[slot] = new int[TILE_AREA];
                after[slot] = new int[TILE_AREA];
                real[slot] = new int[TILE_AREA];
            }
            slotTiles[slot] = tile;
            tileSlots[tile] = slot;
            return slot;
        }

        private void resolveInto(int slot, int[] copy)
        {
            int tile = slotTiles[slot];
            int x0 = (tile % tilesWide) * TILE;
            int y0 = (tile / tilesWide) * TILE;
            int columns = Math.min(TILE, width - x0);
            int rows = Math.min(TILE, height - y0);
            int[] saved = before[slot];
            int[] left = after[slot];
            for (int row = 0; row < rows; row++)
            {
                int imageIndex = (y0 + row) * width + x0;
                int tileIndex = row * TILE;
                for (int column = 0; column < columns; column++)
                {
                    int now = pixels[imageIndex + column];
                    int shown = now == left[tileIndex + column] ? saved[tileIndex + column] : now;
                    if (copy == null)
                    {
                        saved[tileIndex + column] = shown;
                    }
                    else
                    {
                        copy[imageIndex + column] = shown;
                    }
                }
            }
        }

        private void copyTile(int tile, int[] source, int[] into, boolean toImage)
        {
            int x0 = (tile % tilesWide) * TILE;
            int y0 = (tile / tilesWide) * TILE;
            int columns = Math.min(TILE, width - x0);
            int rows = Math.min(TILE, height - y0);
            for (int row = 0; row < rows; row++)
            {
                int image = (y0 + row) * width + x0;
                int saved = row * TILE;
                System.arraycopy(source, toImage ? saved : image, into, toImage ? image : saved, columns);
            }
        }
    }
}
