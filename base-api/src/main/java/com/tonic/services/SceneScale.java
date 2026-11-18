package com.tonic.services;

import com.tonic.Static;

/**
 * Has the CPU renderer draw the 3D scene into the top left of the viewport at a smaller size and stretches it over
 * the whole viewport before anything is drawn on the scene, so fewer pixels are filled. The scale is taken once a
 * frame, after the scene, so a frame's picking and drawing always use the same one.
 */
public final class SceneScale
{
    private static int percent = 100;
    private static int[] columns = new int[0];
    private static int columnsWidth;
    private static int columnsScaled;
    private static boolean depthPending;
    private static int depthStride;
    private static int depthX;
    private static int depthY;
    private static int depthWidth;
    private static int depthHeight;
    private static int depthScaledWidth;
    private static int depthScaledHeight;

    private SceneScale()
    {
    }

    /**
     * @return the percent of the viewport's width and height this frame's scene is drawn at, 100 when not scaled
     */
    public static int percent()
    {
        return percent;
    }

    /**
     * @param size a viewport width or height
     * @return the size the scene is drawn at along it
     */
    public static int scaled(int size)
    {
        return Math.max(1, (size * percent + 50) / 100);
    }

    /**
     * Scales a viewport coordinate, or a length across it, to the scene's size along that axis.
     *
     * @param value the coordinate or length
     * @param size the viewport's size along the axis
     * @return the value at the scene's size
     */
    public static int scale(int value, int size)
    {
        return (int) ((long) value * scaled(size) / size);
    }

    /**
     * Takes the scale for the next frame from the settings, or the full size while the GPU draws the scene.
     *
     * @param gpu whether the GPU draws the scene
     */
    public static void latch(boolean gpu)
    {
        percent = gpu ? 100 : Static.getVitaConfig().getRenderScale().getPercent();
    }

    /**
     * Stretches the scene drawn into the viewport's top left over the whole viewport. The depth is stretched later,
     * only if something asks for it.
     *
     * @param pixels the frame's pixels
     * @param stride the frame's width
     * @param x the viewport's left
     * @param y the viewport's top
     * @param width the viewport's width
     * @param height the viewport's height
     */
    public static void stretch(int[] pixels, int stride, int x, int y, int width, int height)
    {
        int scaledWidth = scaled(width);
        int scaledHeight = scaled(height);
        int[] map = columns(width, scaledWidth);
        int previous = -1;
        for (int row = height - 1; row >= 0; row--)
        {
            int source = row * scaledHeight / height;
            int target = (y + row) * stride + x;
            if (source == previous)
            {
                System.arraycopy(pixels, target + stride, pixels, target, width);
                continue;
            }

            int from = (y + source) * stride + x;
            for (int column = width - 1; column >= 0; column--)
            {
                pixels[target + column] = pixels[from + map[column]];
            }
            previous = source;
        }

        depthPending = true;
        depthStride = stride;
        depthX = x;
        depthY = y;
        depthWidth = width;
        depthHeight = height;
        depthScaledWidth = scaledWidth;
        depthScaledHeight = scaledHeight;
    }

    /**
     * Stretches the last scaled scene's depth over the viewport, once, so it lines up with the stretched pixels.
     *
     * @param depth the frame's depth
     */
    public static void stretchDepth(float[] depth)
    {
        if (!depthPending || depth == null)
        {
            return;
        }
        depthPending = false;

        int[] map = columns(depthWidth, depthScaledWidth);
        int previous = -1;
        for (int row = depthHeight - 1; row >= 0; row--)
        {
            int source = row * depthScaledHeight / depthHeight;
            int target = (depthY + row) * depthStride + depthX;
            if (source == previous)
            {
                System.arraycopy(depth, target + depthStride, depth, target, depthWidth);
                continue;
            }

            int from = (depthY + source) * depthStride + depthX;
            for (int column = depthWidth - 1; column >= 0; column--)
            {
                depth[target + column] = depth[from + map[column]];
            }
            previous = source;
        }
    }

    /**
     * Drops a depth stretch nobody asked for, as a new scene is about to be drawn over it.
     */
    public static void discardDepth()
    {
        depthPending = false;
    }

    private static int[] columns(int width, int scaledWidth)
    {
        if (width != columnsWidth || scaledWidth != columnsScaled)
        {
            columns = new int[width];
            for (int column = 0; column < width; column++)
            {
                columns[column] = column * scaledWidth / width;
            }
            columnsWidth = width;
            columnsScaled = scaledWidth;
        }
        return columns;
    }
}
