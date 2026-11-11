package com.tonic.services.ground;

import java.util.List;

/**
 * The ground shapes taken out of RuneLite's overlay layers this frame, laid on the ground through a canvas per layer.
 */
final class CapturedShapes
{
    private static final double UNITS_PER_PIXEL = 2.5;

    private static final GroundLayer above = new GroundLayer();
    private static final GroundLayer under = new GroundLayer();
    private static final GroundLayer widgets = new GroundLayer();

    private static GroundCanvas canvas;
    private static GroundShape lastOutline;
    private static double lastOutlineInset;
    private static long widgetsFrame = Long.MIN_VALUE;

    private CapturedShapes()
    {
    }

    static void beginFrame(boolean capturing)
    {
        lastOutline = null;
        above.clear();
        if (!capturing)
        {
            under.clear();
            widgets.clear();
        }
    }

    static void addTo(List<GroundLayer> frame)
    {
        frame.add(above);
        frame.add(under);
        frame.add(widgets);
    }

    static void beginAbove()
    {
        begin(above);
    }

    static void beginUnder()
    {
        under.clear();
        begin(under);
    }

    static void beginWidgets(long frame)
    {
        if (widgetsFrame != frame)
        {
            widgets.clear();
            widgetsFrame = frame;
        }
        begin(widgets);
    }

    static void end()
    {
        canvas = null;
        lastOutline = null;
    }

    static boolean record(GroundShape shape)
    {
        GroundCanvas view = canvas == null ? null : canvas.onView(shape.points[0].worldView);
        if (view == null || !view.draws())
        {
            return false;
        }

        int count = shape.points.length;
        double[] xs = new double[count];
        double[] ys = new double[count];
        for (int i = 0; i < count; i++)
        {
            xs[i] = shape.points[i].x / GroundDrape.TILE;
            ys[i] = shape.points[i].y / GroundDrape.TILE;
        }
        int plane = shape.points[0].plane;
        int argb = shape.argb;
        boolean rectangle = count == 4 && onTileGrid(shape.points) && axisRectangle(xs, ys);

        if (shape.fill)
        {
            double inset = lastOutline != null && lastOutline.samePlace(shape) ? lastOutlineInset : 0;
            lastOutline = null;
            if (rectangle)
            {
                view.fillInside(min(xs), min(ys), max(xs), max(ys), inset, plane, argb);
                return true;
            }
            view.fillLocal(xs, ys, count, plane, argb);
            return true;
        }

        double units = Math.max(1, Math.round(shape.strokeWidth * UNITS_PER_PIXEL));
        double width = GroundCanvas.tiles(units);
        if (rectangle && shape.closed && shape.dash == null)
        {
            view.outlineRectangle(min(xs), min(ys), max(xs), max(ys), plane, argb, width);
            lastOutline = shape;
            lastOutlineInset = width;
            return true;
        }

        double[] dash = null;
        if (shape.dash != null)
        {
            dash = new double[shape.dash.length];
            for (int i = 0; i < dash.length; i++)
            {
                dash[i] = GroundCanvas.tiles(shape.dash[i] * UNITS_PER_PIXEL);
            }
        }
        view.polylineLocal(xs, ys, count, shape.closed, plane, argb, width / 2, dash, GroundCanvas.tiles(shape.dashPhase * UNITS_PER_PIXEL));
        lastOutline = null;
        return true;
    }

    private static void begin(GroundLayer layer)
    {
        canvas = GroundCanvas.over(layer);
        lastOutline = null;
    }

    private static boolean onTileGrid(GroundPoint[] points)
    {
        for (GroundPoint point : points)
        {
            if ((point.x & 127) != 0 || (point.y & 127) != 0)
            {
                return false;
            }
        }
        return true;
    }

    private static boolean axisRectangle(double[] xs, double[] ys)
    {
        double left = min(xs);
        double right = max(xs);
        double bottom = min(ys);
        double top = max(ys);
        if (left == right || bottom == top)
        {
            return false;
        }
        for (int i = 0; i < xs.length; i++)
        {
            if ((xs[i] != left && xs[i] != right) || (ys[i] != bottom && ys[i] != top))
            {
                return false;
            }
        }
        for (int i = 0; i < xs.length; i++)
        {
            int next = (i + 1) % xs.length;
            if (xs[i] != xs[next] && ys[i] != ys[next])
            {
                return false;
            }
        }
        return true;
    }

    private static double min(double[] values)
    {
        double min = Double.MAX_VALUE;
        for (double value : values)
        {
            min = Math.min(min, value);
        }
        return min;
    }

    private static double max(double[] values)
    {
        double max = -Double.MAX_VALUE;
        for (double value : values)
        {
            max = Math.max(max, value);
        }
        return max;
    }
}
