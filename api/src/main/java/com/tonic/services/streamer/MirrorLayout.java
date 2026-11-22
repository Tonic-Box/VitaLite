package com.tonic.services.streamer;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Lays out the overlays stacked in RuneLite's snap corners as the mirror shows them: each corner is followed a second time, shifted only by the overlays the mirror shows, at the sizes it shows them. Runs on the client thread only.
 */
final class MirrorLayout
{
    private static final int ALIGNMENT_RIGHT = 0x1;
    private static final int ALIGNMENT_BOTTOM = 0x2;
    private static final int ALIGNMENT_CENTER_HORIZONTAL = 0x4;
    private static final int EXPAND_LEFT = 0x10;
    private static final int EXPAND_RIGHT = 0x20;
    private static final int EXPAND_UP = 0x40;
    private static final int EXPAND_DOWN = 0x80;
    private static final int PADDING = 2;

    private static final Map<Object, Corner> corners = new IdentityHashMap<>();

    private static MethodHandle snapCorners;
    private static MethodHandle correctedPosition;
    private static MethodHandle clamp;
    private static MethodHandle forPosition;
    private static MethodHandle cornerX;
    private static MethodHandle cornerY;
    private static MethodHandle cornerMode;
    private static boolean broken;
    private static long frame;
    private static Corner placing;

    private MirrorLayout()
    {
    }

    /**
     * Starts a new frame, after which each corner is followed again from where RuneLite starts it.
     */
    static void nextFrame()
    {
        frame++;
    }

    /**
     * Works out where the mirror shows an overlay RuneLite is about to draw.
     *
     * @param renderer RuneLite's overlay renderer
     * @param overlay the overlay
     * @param real where RuneLite draws it
     * @param size the size the mirror showed it at last
     * @param realSize whether that is the size RuneLite showed it at last
     * @return where the mirror shows it
     */
    static Point place(Object renderer, Overlay overlay, Point real, Dimension size, boolean realSize)
    {
        placing = null;
        Object snapCorner = snapCorner(renderer, overlay);
        if (snapCorner == null)
        {
            return new Point(real);
        }
        try
        {
            Corner corner = corners.computeIfAbsent(snapCorner, key -> new Corner());
            int realX = (int) cornerX.invokeExact(snapCorner);
            int realY = (int) cornerY.invokeExact(snapCorner);
            if (corner.frame != frame)
            {
                corner.frame = frame;
                corner.x = realX;
                corner.y = realY;
                corner.mode = (int) cornerMode.invokeExact(snapCorner);
            }
            placing = corner;
            if (realSize && corner.x == realX && corner.y == realY)
            {
                return new Point(real);
            }
            int x = corner.x;
            int y = corner.y;
            if ((corner.mode & ALIGNMENT_CENTER_HORIZONTAL) != 0)
            {
                x -= size.width / 2;
            }
            else if ((corner.mode & ALIGNMENT_RIGHT) != 0)
            {
                x -= size.width;
            }
            if ((corner.mode & ALIGNMENT_BOTTOM) != 0)
            {
                y -= size.height;
            }
            Point placed = new Point();
            clamp.invokeExact(renderer, x, y, size.width, size.height, overlay.getParentBounds(), placed);
            return placed;
        }
        catch (Throwable e)
        {
            broken = true;
            return new Point(real);
        }
    }

    /**
     * Moves the corner of the overlay placed last past it, as RuneLite does once an overlay is drawn. Not called for overlays the mirror leaves out.
     *
     * @param bounds where the mirror shows the overlay and how big
     */
    static void placed(Rectangle bounds)
    {
        Corner corner = placing;
        placing = null;
        if (corner == null || bounds.width + bounds.height <= 0)
        {
            return;
        }
        switch (corner.mode & 0xf0)
        {
            case EXPAND_LEFT:
                corner.x = Math.min(corner.x, bounds.x - PADDING);
                break;
            case EXPAND_RIGHT:
                corner.x = Math.max(corner.x, bounds.x + bounds.width + PADDING);
                break;
            case EXPAND_UP:
                corner.y = Math.min(corner.y, bounds.y - PADDING);
                break;
            case EXPAND_DOWN:
                corner.y = Math.max(corner.y, bounds.y + bounds.height + PADDING);
                break;
            default:
                break;
        }
    }

    /**
     * Forgets the overlay placed last without moving its corner.
     */
    static void skipped()
    {
        placing = null;
    }

    private static Object snapCorner(Object renderer, Overlay overlay)
    {
        if (broken || overlay.getPreferredLocation() != null || !resolve(renderer))
        {
            return null;
        }
        try
        {
            OverlayPosition position = (OverlayPosition) correctedPosition.invokeExact(renderer, overlay);
            if (position == OverlayPosition.DYNAMIC || position == OverlayPosition.TOOLTIP || position == OverlayPosition.DETACHED)
            {
                return null;
            }
            return (Object) forPosition.invokeExact((Object) snapCorners.invokeExact(renderer), position);
        }
        catch (Throwable e)
        {
            broken = true;
            return null;
        }
    }

    private static boolean resolve(Object renderer)
    {
        if (clamp != null)
        {
            return true;
        }
        try
        {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> rendererType = renderer.getClass();
            Field snapCornersField = Members.field(rendererType, "snapCorners");
            Method forPositionMethod = Members.method(snapCornersField.getType(), "forPosition", OverlayPosition.class);
            Class<?> snapCorner = forPositionMethod.getReturnType();
            snapCorners = lookup.unreflectGetter(snapCornersField).asType(MethodType.methodType(Object.class, Object.class));
            correctedPosition = lookup.unreflect(Members.method(rendererType, "getCorrectedOverlayPosition", Overlay.class)).asType(MethodType.methodType(OverlayPosition.class, Object.class, Overlay.class));
            forPosition = lookup.unreflect(forPositionMethod).asType(MethodType.methodType(Object.class, Object.class, OverlayPosition.class));
            cornerX = intGetter(lookup, snapCorner, "px");
            cornerY = intGetter(lookup, snapCorner, "py");
            cornerMode = intGetter(lookup, snapCorner, "mode");
            clamp = lookup.unreflect(Members.method(rendererType, "clampOverlayLocation", int.class, int.class, int.class, int.class, Rectangle.class, Point.class)).asType(MethodType.methodType(void.class, Object.class, int.class, int.class, int.class, int.class, Rectangle.class, Point.class));
            return true;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            broken = true;
            return false;
        }
    }

    private static MethodHandle intGetter(MethodHandles.Lookup lookup, Class<?> owner, String name) throws ReflectiveOperationException
    {
        return lookup.unreflectGetter(Members.field(owner, name)).asType(MethodType.methodType(int.class, Object.class));
    }

    private static final class Corner
    {
        private long frame = -1;
        private int x;
        private int y;
        private int mode;
    }
}
