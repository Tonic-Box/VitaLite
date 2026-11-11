package com.tonic.services.ground;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Where a ground overlay draws: flat shapes laid on the terrain in world tiles, hidden by whatever stands in front of them. Widths are in game units, 128 to a tile, and colours are drawn with their alpha as opacity.
 */
public final class GroundCanvas
{
    /**
     * A fine line.
     */
    public static final int THIN = 6;
    /**
     * A line that reads at a distance.
     */
    public static final int REGULAR = 12;

    static final double LIFT = 3;

    private final GroundDrape drape;
    private final IntFunction<GroundCanvas> views;
    private final double[] cornersX = new double[4];
    private final double[] cornersY = new double[4];

    private GroundCanvas(GroundDrape drape, IntFunction<GroundCanvas> views)
    {
        this.drape = drape;
        this.views = views;
    }

    static GroundCanvas over(GroundLayer into)
    {
        return new Views(into).apply(WorldView.TOPLEVEL);
    }

    /**
     * Gets the canvas of another world view, such as a boat, drawing on its tiles in its own coordinates and moving with it.
     *
     * @param worldViewId the world view's id
     * @return its canvas, one that draws nothing when the world view is not loaded
     */
    public GroundCanvas onView(int worldViewId)
    {
        return views.apply(worldViewId);
    }

    /**
     * Fills a tile.
     *
     * @param x the tile's world x
     * @param y the tile's world y
     * @param plane its plane
     * @param colour the fill
     */
    public void fillTile(int x, int y, int plane, Color colour)
    {
        fillArea(x, y, 1, 1, plane, colour);
    }

    /**
     * Outlines a tile with a border inside it, so the outlines of neighbouring tiles never overlap.
     *
     * @param x the tile's world x
     * @param y the tile's world y
     * @param plane its plane
     * @param colour the border's colour
     * @param width the border's width in game units
     */
    public void outlineTile(int x, int y, int plane, Color colour, int width)
    {
        outlineArea(x, y, 1, 1, plane, colour, width);
    }

    /**
     * Marks a tile as RuneLite's tile markers do: a border inside it and a fill inside the border, so no part is drawn twice.
     *
     * @param x the tile's world x
     * @param y the tile's world y
     * @param plane its plane
     * @param outline the border's colour
     * @param fill the fill, or null for none
     * @param width the border's width in game units
     */
    public void markTile(int x, int y, int plane, Color outline, Color fill, int width)
    {
        requirePlane(plane);
        requireColour(outline);
        double inset = tiles(requireWidth(width));
        if (drape == null)
        {
            return;
        }

        double left = localX(x);
        double bottom = localY(y);
        if (!transparent(outline))
        {
            outlineRectangle(left, bottom, left + 1, bottom + 1, plane, outline.getRGB(), inset);
        }
        if (fill != null && !transparent(fill))
        {
            fillInside(left, bottom, left + 1, bottom + 1, inset, plane, fill.getRGB());
        }
    }

    /**
     * Draws a line along one side of a tile, inside it.
     *
     * @param x the tile's world x
     * @param y the tile's world y
     * @param plane its plane
     * @param side which side
     * @param colour the line's colour
     * @param width the line's width in game units
     */
    public void tileEdge(int x, int y, int plane, TileSide side, Color colour, int width)
    {
        requirePlane(plane);
        requireColour(colour);
        double inset = tiles(requireWidth(width));
        if (side == null)
        {
            throw new IllegalArgumentException("side cannot be null");
        }
        if (skips(colour))
        {
            return;
        }

        double left = localX(x);
        double bottom = localY(y);
        switch (side)
        {
            case SOUTH:
                rectangle(left, bottom, left + 1, bottom + inset, plane, colour.getRGB());
                break;
            case NORTH:
                rectangle(left, bottom + 1 - inset, left + 1, bottom + 1, plane, colour.getRGB());
                break;
            case WEST:
                rectangle(left, bottom, left + inset, bottom + 1, plane, colour.getRGB());
                break;
            default:
                rectangle(left + 1 - inset, bottom, left + 1, bottom + 1, plane, colour.getRGB());
                break;
        }
    }

    /**
     * Fills a rectangle of whole tiles.
     *
     * @param x the world x of its tiles lowest in x
     * @param y the world y of its tiles lowest in y
     * @param width how many tiles across
     * @param height how many tiles along
     * @param plane its plane
     * @param colour the fill
     */
    public void fillArea(int x, int y, int width, int height, int plane, Color colour)
    {
        requireSize(width, height);
        requirePlane(plane);
        requireColour(colour);
        if (skips(colour))
        {
            return;
        }
        rectangle(localX(x), localY(y), localX(x) + width, localY(y) + height, plane, colour.getRGB());
    }

    /**
     * Outlines a rectangle of whole tiles with a border inside it.
     *
     * @param x the world x of its tiles lowest in x
     * @param y the world y of its tiles lowest in y
     * @param width how many tiles across
     * @param height how many tiles along
     * @param plane its plane
     * @param colour the border's colour
     * @param lineWidth the border's width in game units
     */
    public void outlineArea(int x, int y, int width, int height, int plane, Color colour, int lineWidth)
    {
        requireSize(width, height);
        requirePlane(plane);
        requireColour(colour);
        double inset = tiles(requireWidth(lineWidth));
        if (skips(colour))
        {
            return;
        }
        outlineRectangle(localX(x), localY(y), localX(x) + width, localY(y) + height, plane, colour.getRGB(), inset);
    }

    /**
     * Draws a straight line between two points, squared off past each end by half its width.
     *
     * @param fromX where it starts, in world tiles, fractional: a tile's centre is its x + 0.5
     * @param fromY where it starts along
     * @param toX where it ends across
     * @param toY where it ends along
     * @param plane its plane
     * @param colour its colour
     * @param width its width in game units
     */
    public void line(double fromX, double fromY, double toX, double toY, int plane, Color colour, int width)
    {
        requirePlane(plane);
        requireColour(colour);
        double half = tiles(requireWidth(width)) / 2;
        if (skips(colour))
        {
            return;
        }
        strip(fromX - baseX(), fromY - baseY(), toX - baseX(), toY - baseY(), half, plane, colour.getRGB());
    }

    /**
     * Draws a line through the centres of a run of tiles; a step between tiles on different planes, or with a null tile, is left out. Steps going the same way are drawn as one straight line.
     *
     * @param tiles the tiles in order
     * @param colour its colour
     * @param width its width in game units
     */
    public void path(List<WorldPoint> tiles, Color colour, int width)
    {
        if (tiles == null)
        {
            throw new IllegalArgumentException("tiles cannot be null");
        }
        requireColour(colour);
        double half = tiles(requireWidth(width)) / 2;
        if (skips(colour))
        {
            return;
        }

        int start = 0;
        while (start + 1 < tiles.size())
        {
            WorldPoint from = tiles.get(start);
            WorldPoint next = tiles.get(start + 1);
            if (!joined(from, next))
            {
                start++;
                continue;
            }

            int stepX = next.getX() - from.getX();
            int stepY = next.getY() - from.getY();
            int end = start + 1;
            while (end + 1 < tiles.size() && joined(tiles.get(end), tiles.get(end + 1)) && tiles.get(end + 1).getX() - tiles.get(end).getX() == stepX && tiles.get(end + 1).getY() - tiles.get(end).getY() == stepY)
            {
                end++;
            }

            WorldPoint to = tiles.get(end);
            strip(from.getX() - baseX() + 0.5, from.getY() - baseY() + 0.5, to.getX() - baseX() + 0.5, to.getY() - baseY() + 0.5, half, from.getPlane(), colour.getRGB());
            start = end;
        }
    }

    /**
     * Fills a polygon, convex or not, as long as its sides do not cross.
     *
     * @param xs its corners across, in world tiles, fractional
     * @param ys its corners along
     * @param plane its plane
     * @param colour the fill
     */
    public void fillPolygon(double[] xs, double[] ys, int plane, Color colour)
    {
        requireCorners(xs, ys);
        requirePlane(plane);
        requireColour(colour);
        if (skips(colour))
        {
            return;
        }
        fillLocal(shifted(xs, baseX()), shifted(ys, baseY()), xs.length, plane, colour.getRGB());
    }

    /**
     * Outlines a polygon: a line along each side, the last corner joined back to the first.
     *
     * @param xs its corners across, in world tiles, fractional
     * @param ys its corners along
     * @param plane its plane
     * @param colour the line's colour
     * @param width the line's width in game units
     */
    public void outlinePolygon(double[] xs, double[] ys, int plane, Color colour, int width)
    {
        requireCorners(xs, ys);
        requirePlane(plane);
        requireColour(colour);
        double half = tiles(requireWidth(width)) / 2;
        if (skips(colour))
        {
            return;
        }
        polylineLocal(shifted(xs, baseX()), shifted(ys, baseY()), xs.length, true, plane, colour.getRGB(), half, null, 0);
    }

    boolean draws()
    {
        return drape != null;
    }

    void fillLocal(double[] xs, double[] ys, int count, int plane, int argb)
    {
        if (count == 3 || (count == 4 && convex(xs, ys, count)))
        {
            System.arraycopy(xs, 0, cornersX, 0, count);
            System.arraycopy(ys, 0, cornersY, 0, count);
            drape.fillConvex(cornersX, cornersY, count, plane, argb);
            return;
        }

        double[] cutX = Arrays.copyOf(xs, count);
        double[] cutY = Arrays.copyOf(ys, count);
        int[] triangles = triangulate(cutX, cutY);
        for (int at = 0; at + 2 < triangles.length; at += 3)
        {
            for (int corner = 0; corner < 3; corner++)
            {
                cornersX[corner] = cutX[triangles[at + corner]];
                cornersY[corner] = cutY[triangles[at + corner]];
            }
            drape.fillConvex(cornersX, cornersY, 3, plane, argb);
        }
    }

    void rectangle(double left, double bottom, double right, double top, int plane, int argb)
    {
        cornersX[0] = left;
        cornersY[0] = bottom;
        cornersX[1] = right;
        cornersY[1] = bottom;
        cornersX[2] = right;
        cornersY[2] = top;
        cornersX[3] = left;
        cornersY[3] = top;
        drape.fillConvex(cornersX, cornersY, 4, plane, argb);
    }

    void fillInside(double left, double bottom, double right, double top, double inset, int plane, int argb)
    {
        double insideLeft = left + inset;
        double insideRight = right - inset;
        double insideBottom = bottom + inset;
        double insideTop = top - inset;
        if (insideRight > insideLeft && insideTop > insideBottom)
        {
            rectangle(insideLeft, insideBottom, insideRight, insideTop, plane, argb);
        }
    }

    void outlineRectangle(double left, double bottom, double right, double top, int plane, int argb, double inset)
    {
        if (inset * 2 >= Math.min(right - left, top - bottom))
        {
            rectangle(left, bottom, right, top, plane, argb);
            return;
        }
        rectangle(left, bottom, right, bottom + inset, plane, argb);
        rectangle(left, top - inset, right, top, plane, argb);
        rectangle(left, bottom + inset, left + inset, top - inset, plane, argb);
        rectangle(right - inset, bottom + inset, right, top - inset, plane, argb);
    }

    void polylineLocal(double[] xs, double[] ys, int count, boolean closed, int plane, int argb, double half, double[] dash, double dashPhase)
    {
        int segments = closed ? count : count - 1;
        if (dash == null)
        {
            for (int at = 0; at < segments; at++)
            {
                int next = (at + 1) % count;
                strip(xs[at], ys[at], xs[next], ys[next], half, plane, argb);
            }
            return;
        }

        double total = 0;
        for (double length : dash)
        {
            total += length;
        }
        if (total <= 0)
        {
            return;
        }

        double into = ((dashPhase % total) + total) % total;
        int dashIndex = 0;
        while (into >= dash[dashIndex])
        {
            into -= dash[dashIndex];
            dashIndex = (dashIndex + 1) % dash.length;
        }

        for (int at = 0; at < segments; at++)
        {
            int next = (at + 1) % count;
            double fromX = xs[at];
            double fromY = ys[at];
            double length = Math.hypot(xs[next] - fromX, ys[next] - fromY);
            if (length == 0)
            {
                continue;
            }
            double unitX = (xs[next] - fromX) / length;
            double unitY = (ys[next] - fromY) / length;
            double walked = 0;
            while (walked < length)
            {
                double step = Math.min(dash[dashIndex] - into, length - walked);
                if (dashIndex % 2 == 0 && step > 0)
                {
                    strip(fromX + unitX * walked, fromY + unitY * walked, fromX + unitX * (walked + step), fromY + unitY * (walked + step), half, plane, argb);
                }
                walked += step;
                into += step;
                if (into >= dash[dashIndex])
                {
                    into = 0;
                    dashIndex = (dashIndex + 1) % dash.length;
                }
            }
        }
    }

    private void strip(double fromX, double fromY, double toX, double toY, double half, int plane, int argb)
    {
        double alongX = toX - fromX;
        double alongY = toY - fromY;
        double length = Math.hypot(alongX, alongY);
        if (length == 0)
        {
            rectangle(fromX - half, fromY - half, fromX + half, fromY + half, plane, argb);
            return;
        }

        alongX = alongX / length * half;
        alongY = alongY / length * half;
        double acrossX = -alongY;
        double acrossY = alongX;
        cornersX[0] = fromX - alongX - acrossX;
        cornersY[0] = fromY - alongY - acrossY;
        cornersX[1] = toX + alongX - acrossX;
        cornersY[1] = toY + alongY - acrossY;
        cornersX[2] = toX + alongX + acrossX;
        cornersY[2] = toY + alongY + acrossY;
        cornersX[3] = fromX - alongX + acrossX;
        cornersY[3] = fromY - alongY + acrossY;
        drape.fillConvex(cornersX, cornersY, 4, plane, argb);
    }

    static int[] triangulate(double[] xs, double[] ys)
    {
        int count = xs.length;
        List<Integer> left = new ArrayList<>(count);
        for (int at = 0; at < count; at++)
        {
            left.add(at);
        }

        double turn = Math.signum(signedArea(xs, ys));
        int[] triangles = new int[Math.max(0, (count - 2) * 3)];
        int made = 0;
        int guard = count * count;
        while (left.size() > 3 && guard-- > 0)
        {
            boolean clipped = false;
            for (int at = 0; at < left.size(); at++)
            {
                int previous = left.get((at + left.size() - 1) % left.size());
                int corner = left.get(at);
                int next = left.get((at + 1) % left.size());
                if (isEar(xs, ys, left, previous, corner, next, turn))
                {
                    triangles[made++] = previous;
                    triangles[made++] = corner;
                    triangles[made++] = next;
                    left.remove(at);
                    clipped = true;
                    break;
                }
            }
            if (!clipped)
            {
                break;
            }
        }

        if (left.size() == 3)
        {
            triangles[made++] = left.get(0);
            triangles[made++] = left.get(1);
            triangles[made++] = left.get(2);
        }
        return Arrays.copyOf(triangles, made);
    }

    private static boolean isEar(double[] xs, double[] ys, List<Integer> left, int previous, int corner, int next, double turn)
    {
        double cross = cross(xs[previous], ys[previous], xs[corner], ys[corner], xs[next], ys[next]);
        if (cross * turn <= 0)
        {
            return false;
        }
        for (int other : left)
        {
            if (other != previous && other != corner && other != next && inside(xs[other], ys[other], xs[previous], ys[previous], xs[corner], ys[corner], xs[next], ys[next]))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean convex(double[] xs, double[] ys, int count)
    {
        double sign = 0;
        for (int at = 0; at < count; at++)
        {
            double turn = cross(xs[at], ys[at], xs[(at + 1) % count], ys[(at + 1) % count], xs[(at + 2) % count], ys[(at + 2) % count]);
            if (turn != 0)
            {
                if (sign != 0 && Math.signum(turn) != sign)
                {
                    return false;
                }
                sign = Math.signum(turn);
            }
        }
        return sign != 0;
    }

    private static boolean inside(double x, double y, double ax, double ay, double bx, double by, double cx, double cy)
    {
        double first = cross(ax, ay, bx, by, x, y);
        double second = cross(bx, by, cx, cy, x, y);
        double third = cross(cx, cy, ax, ay, x, y);
        boolean anyNegative = first < 0 || second < 0 || third < 0;
        boolean anyPositive = first > 0 || second > 0 || third > 0;
        return !(anyNegative && anyPositive);
    }

    private static double cross(double ax, double ay, double bx, double by, double cx, double cy)
    {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static double signedArea(double[] xs, double[] ys)
    {
        double twice = 0;
        for (int at = 0; at < xs.length; at++)
        {
            int next = (at + 1) % xs.length;
            twice += xs[at] * ys[next] - xs[next] * ys[at];
        }
        return twice / 2;
    }

    private static boolean joined(WorldPoint from, WorldPoint to)
    {
        return from != null && to != null && from.getPlane() == to.getPlane() && from.getPlane() >= 0 && from.getPlane() < GroundSurface.PLANES;
    }

    private double localX(int worldX)
    {
        return worldX - baseX();
    }

    private double localY(int worldY)
    {
        return worldY - baseY();
    }

    private int baseX()
    {
        return drape == null ? 0 : drape.surface().baseX();
    }

    private int baseY()
    {
        return drape == null ? 0 : drape.surface().baseY();
    }

    private static double[] shifted(double[] values, int by)
    {
        double[] shifted = new double[values.length];
        for (int at = 0; at < values.length; at++)
        {
            shifted[at] = values[at] - by;
        }
        return shifted;
    }

    static double tiles(double units)
    {
        return units / GroundDrape.TILE;
    }

    private boolean skips(Color colour)
    {
        return drape == null || transparent(colour);
    }

    private static boolean transparent(Color colour)
    {
        return colour.getAlpha() == 0;
    }

    private static void requirePlane(int plane)
    {
        if (plane < 0 || plane >= GroundSurface.PLANES)
        {
            throw new IllegalArgumentException("plane must be 0 to 3, was " + plane);
        }
    }

    private static void requireColour(Color colour)
    {
        if (colour == null)
        {
            throw new IllegalArgumentException("colour cannot be null");
        }
    }

    private static int requireWidth(int width)
    {
        if (width <= 0)
        {
            throw new IllegalArgumentException("width must be positive, was " + width);
        }
        return width;
    }

    private static void requireSize(int width, int height)
    {
        if (width <= 0 || height <= 0)
        {
            throw new IllegalArgumentException("width and height must be positive, were " + width + " and " + height);
        }
    }

    private static void requireCorners(double[] xs, double[] ys)
    {
        if (xs == null || ys == null || xs.length != ys.length || xs.length < 3)
        {
            throw new IllegalArgumentException("a polygon needs as many xs as ys, three or more");
        }
    }

    private static final class Views implements IntFunction<GroundCanvas>
    {
        private final GroundLayer into;
        private final Map<Integer, GroundCanvas> made = new HashMap<>();

        Views(GroundLayer into)
        {
            this.into = into;
        }

        @Override
        public GroundCanvas apply(int worldViewId)
        {
            return made.computeIfAbsent(worldViewId, this::make);
        }

        private GroundCanvas make(int worldViewId)
        {
            Client client = Static.getClient();
            GroundSurface surface = client == null ? null : GroundSurface.of(client.getWorldView(worldViewId));
            if (surface == null)
            {
                return new GroundCanvas(null, this);
            }
            return new GroundCanvas(new GroundDrape(surface, LIFT, into.view(worldViewId)), this);
        }
    }
}
