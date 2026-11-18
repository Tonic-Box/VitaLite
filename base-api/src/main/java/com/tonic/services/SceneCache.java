package com.tonic.services;

import com.tonic.Static;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reuses the CPU renderer's opaque static scene while the camera and scenery stay still. Once the scene has been
 * still for a few frames, one frame draws the opaque pass without what moves and keeps its pixels and depth; each
 * later frame restores them and fills only what moves, while the static scene is still projected and picked so
 * hover, menus and tile picking work as before.
 */
public final class SceneCache
{
    private static final int OFF = 0;
    private static final int BUILD = 1;
    private static final int REPLAY = 2;
    private static final int STILL_FRAMES = 2;
    private static final int MAX_REPLAYS = 150;

    private static final boolean[] marks = new boolean[64];

    private static final AtomicInteger revision = new AtomicInteger();
    private static int mode;
    private static int fullHeight;
    private static int staticHeight;
    private static int dynamicHeight;
    private static int dynamicDepth;
    private static int level;
    private static int overflow;

    private static boolean valid;
    private static int replays;
    private static int stillFrames;
    private static float keyX;
    private static float keyY;
    private static float keyZ;
    private static float keyPitch;
    private static float keyYaw;
    private static int keyZoom;
    private static int keyPlane;
    private static int keyLeft;
    private static int keyTop;
    private static int keyWidth;
    private static int keyHeight;
    private static int keyStride;
    private static int keyRevision;
    private static double keyBrightness;

    private static int[] pixels = new int[0];
    private static float[] depth = new float[0];

    private SceneCache()
    {
    }

    /**
     * Marks the cached scene out of date, as the scenery changed.
     */
    public static void invalidate()
    {
        revision.incrementAndGet();
    }

    /**
     * @return whether the opaque pass is being drawn for or from the cache
     */
    public static boolean active()
    {
        return mode != OFF;
    }

    /**
     * @return whether the opaque pass is being drawn without what moves, to be kept
     */
    public static boolean building()
    {
        return mode == BUILD;
    }

    /**
     * @return whether the model being drawn has nothing to fill, so its faces need not be drawn after picking
     */
    public static boolean skipsFaces()
    {
        return mode != OFF && height() == 0;
    }

    /**
     * Decides how this frame's opaque pass is drawn, restoring the cached scene when it can be reused.
     *
     * @param x the camera's x
     * @param y the camera's y
     * @param z the camera's z
     * @param pitch the camera's pitch
     * @param yaw the camera's yaw
     * @param zoom the projection's zoom
     * @param plane the plane drawn up to
     * @param brightness the textures' brightness
     * @param left the left of the area drawn
     * @param top the top of the area drawn
     * @param width the width of the area drawn
     * @param height the height of the area drawn
     * @param frame the frame's pixels
     * @param frameDepth the frame's depth
     * @param stride the frame's width
     * @return the fill height to start the opaque pass with
     */
    public static int begin(float x, float y, float z, float pitch, float yaw, int zoom, int plane, double brightness, int left, int top, int width, int height, int[] frame, float[] frameDepth, int stride)
    {
        mode = OFF;
        if (frame == null || frameDepth == null || width <= 0 || height <= 0 || !Static.getVitaConfig().shouldReuseStillScene())
        {
            valid = false;
            stillFrames = 0;
            return height;
        }

        int currentRevision = revision.get();
        boolean still = x == keyX && y == keyY && z == keyZ && pitch == keyPitch && yaw == keyYaw && zoom == keyZoom && plane == keyPlane && brightness == keyBrightness && left == keyLeft && top == keyTop && width == keyWidth && height == keyHeight && stride == keyStride && currentRevision == keyRevision;
        if (!still)
        {
            keyX = x;
            keyY = y;
            keyZ = z;
            keyPitch = pitch;
            keyYaw = yaw;
            keyZoom = zoom;
            keyPlane = plane;
            keyBrightness = brightness;
            keyLeft = left;
            keyTop = top;
            keyWidth = width;
            keyHeight = height;
            keyStride = stride;
            keyRevision = currentRevision;
            valid = false;
            stillFrames = 0;
            return height;
        }

        stillFrames++;
        if (valid && replays < MAX_REPLAYS)
        {
            copy(frame, frameDepth, false);
            replays++;
            mode = REPLAY;
        }
        else if (stillFrames >= STILL_FRAMES)
        {
            mode = BUILD;
        }
        else
        {
            return height;
        }

        fullHeight = height;
        staticHeight = mode == REPLAY ? 0 : height;
        dynamicHeight = mode == REPLAY ? height : 0;
        resetNesting();
        return staticHeight;
    }

    /**
     * Keeps the opaque pass just drawn without what moves and turns to filling only what moves.
     *
     * @param frame the frame's pixels
     * @param frameDepth the frame's depth
     * @return the fill height to draw the opaque pass again with
     */
    public static int keep(int[] frame, float[] frameDepth)
    {
        int size = keyWidth * keyHeight;
        if (pixels.length != size)
        {
            pixels = new int[size];
            depth = new float[size];
        }
        copy(frame, frameDepth, true);
        valid = true;
        replays = 0;
        mode = REPLAY;
        staticHeight = 0;
        dynamicHeight = fullHeight;
        resetNesting();
        return staticHeight;
    }

    /**
     * Ends the opaque pass.
     *
     * @return the fill height to put back
     */
    public static int end()
    {
        mode = OFF;
        return fullHeight;
    }

    /**
     * Enters the draw of a tile or renderable, with everything drawn inside it moving too if it moves.
     *
     * @param moving whether it can change while the camera is still
     * @return the fill height to draw it with
     */
    public static int enter(boolean moving)
    {
        if (level == marks.length)
        {
            overflow++;
            return height();
        }
        boolean dynamic = moving || dynamicDepth > 0;
        marks[level++] = dynamic;
        if (dynamic)
        {
            dynamicDepth++;
        }
        return height();
    }

    /**
     * Leaves the innermost tile or renderable draw.
     *
     * @return the fill height to carry on with
     */
    public static int exit()
    {
        if (overflow > 0)
        {
            overflow--;
        }
        else if (level > 0 && marks[--level])
        {
            dynamicDepth--;
        }
        return height();
    }

    private static int height()
    {
        return dynamicDepth > 0 ? dynamicHeight : staticHeight;
    }

    private static void resetNesting()
    {
        dynamicDepth = 0;
        level = 0;
        overflow = 0;
    }

    private static void copy(int[] frame, float[] frameDepth, boolean toCache)
    {
        for (int row = 0; row < keyHeight; row++)
        {
            int frameIndex = (keyTop + row) * keyStride + keyLeft;
            int cacheIndex = row * keyWidth;
            if (toCache)
            {
                System.arraycopy(frame, frameIndex, pixels, cacheIndex, keyWidth);
                System.arraycopy(frameDepth, frameIndex, depth, cacheIndex, keyWidth);
            }
            else
            {
                System.arraycopy(pixels, cacheIndex, frame, frameIndex, keyWidth);
                System.arraycopy(depth, cacheIndex, frameDepth, frameIndex, keyWidth);
            }
        }
    }
}
