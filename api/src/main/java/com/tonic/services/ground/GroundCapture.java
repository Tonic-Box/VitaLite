package com.tonic.services.ground;

import com.tonic.services.streamer.DelegatingGraphics;
import com.tonic.services.streamer.MirrorOverlays;
import com.tonic.services.streamer.Streamer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.util.Arrays;

/**
 * Graphics an overlay layer draws through: lines and polygons whose every corner is a ground point go to the ground pass, everything else is drawn as usual. While streamer mode is on, what goes to the ground pass is also drawn flat for the mirror, where stock RuneLite would draw it.
 */
final class GroundCapture extends DelegatingGraphics
{
    private static final double EPSILON = 1e-3;
    private static final int MAX_VERTICES = 4096;

    private final AffineTransform canvas;

    GroundCapture(Graphics2D graphics)
    {
        this(graphics, inverse(graphics.getTransform()), false);
    }

    private GroundCapture(Graphics2D graphics, AffineTransform canvas, boolean owned)
    {
        super(graphics, owned);
        this.canvas = canvas;
    }

    @Override
    protected Graphics2D wrap(Graphics2D child)
    {
        return new GroundCapture(child, canvas, true);
    }

    @Override
    public void draw(Shape shape)
    {
        if (!capture(shape, false))
        {
            graphics.draw(shape);
        }
        else if (Streamer.mirroring)
        {
            MirrorOverlays.captured(graphics, mirror -> mirror.draw(shape));
        }
    }

    @Override
    public void fill(Shape shape)
    {
        if (!capture(shape, true))
        {
            graphics.fill(shape);
        }
        else if (Streamer.mirroring)
        {
            MirrorOverlays.captured(graphics, mirror -> mirror.fill(shape));
        }
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int count)
    {
        if (!capture(xPoints, yPoints, count, true, false))
        {
            graphics.drawPolygon(xPoints, yPoints, count);
        }
        else if (Streamer.mirroring)
        {
            MirrorOverlays.captured(graphics, mirror -> mirror.drawPolygon(xPoints, yPoints, count));
        }
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int count)
    {
        if (!capture(xPoints, yPoints, count, true, true))
        {
            graphics.fillPolygon(xPoints, yPoints, count);
        }
        else if (Streamer.mirroring)
        {
            MirrorOverlays.captured(graphics, mirror -> mirror.fillPolygon(xPoints, yPoints, count));
        }
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int count)
    {
        if (!capture(xPoints, yPoints, count, false, false))
        {
            graphics.drawPolyline(xPoints, yPoints, count);
        }
        else if (Streamer.mirroring)
        {
            MirrorOverlays.captured(graphics, mirror -> mirror.drawPolyline(xPoints, yPoints, count));
        }
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2)
    {
        if (!capture(new int[]{x1, x2}, new int[]{y1, y2}, 2, false, false))
        {
            graphics.drawLine(x1, y1, x2, y2);
        }
        else if (Streamer.mirroring)
        {
            MirrorOverlays.captured(graphics, mirror -> mirror.drawLine(x1, y1, x2, y2));
        }
    }

    private boolean capture(Shape shape, boolean fill)
    {
        if (!paintable(fill))
        {
            return false;
        }

        PathIterator path = shape.getPathIterator(null);
        int[] xs = new int[8];
        int[] ys = new int[8];
        int count = 0;
        boolean closed = false;
        boolean moved = false;
        double[] coords = new double[6];
        while (!path.isDone())
        {
            int segment = path.currentSegment(coords);
            if (segment == PathIterator.SEG_CLOSE)
            {
                closed = true;
            }
            else if (closed || (segment != PathIterator.SEG_LINETO && segment != PathIterator.SEG_MOVETO) || (segment == PathIterator.SEG_MOVETO && moved))
            {
                return false;
            }
            else
            {
                moved = true;
                long x = Math.round(coords[0]);
                long y = Math.round(coords[1]);
                if (Math.abs(coords[0] - x) > EPSILON || Math.abs(coords[1] - y) > EPSILON || count == MAX_VERTICES)
                {
                    return false;
                }
                if (count == xs.length)
                {
                    xs = Arrays.copyOf(xs, count * 2);
                    ys = Arrays.copyOf(ys, count * 2);
                }
                xs[count] = (int) x;
                ys[count] = (int) y;
                count++;
            }
            path.next();
        }

        if (count > 1 && xs[0] == xs[count - 1] && ys[0] == ys[count - 1])
        {
            count--;
            closed = true;
        }
        return record(xs, ys, count, closed || fill, fill);
    }

    private boolean capture(int[] xPoints, int[] yPoints, int count, boolean closed, boolean fill)
    {
        if (count < 2 || count > MAX_VERTICES || xPoints.length < count || yPoints.length < count || !paintable(fill))
        {
            return false;
        }
        return record(xPoints.clone(), yPoints.clone(), count, closed, fill);
    }

    private boolean record(int[] xs, int[] ys, int count, boolean closed, boolean fill)
    {
        if (count < 2 || (fill && count < 3))
        {
            return false;
        }

        double[] offset = canvasOffset();
        if (offset == null)
        {
            return false;
        }
        for (int i = 0; i < count; i++)
        {
            double x = xs[i] + offset[0];
            double y = ys[i] + offset[1];
            long rx = Math.round(x);
            long ry = Math.round(y);
            if (Math.abs(x - rx) > EPSILON || Math.abs(y - ry) > EPSILON)
            {
                return false;
            }
            xs[i] = (int) rx;
            ys[i] = (int) ry;
        }

        GroundPoint[] points = GroundShape.match(xs, ys, count);
        if (points == null)
        {
            return false;
        }

        float width = 0;
        float[] dash = null;
        float dashPhase = 0;
        if (!fill)
        {
            BasicStroke stroke = (BasicStroke) graphics.getStroke();
            width = stroke.getLineWidth();
            dash = stroke.getDashArray();
            dashPhase = stroke.getDashPhase();
        }

        GroundShape shape = new GroundShape(points, closed, fill, argb(), width, dash, dashPhase);
        return CapturedShapes.record(shape);
    }

    private boolean paintable(boolean fill)
    {
        if (!(graphics.getPaint() instanceof Color))
        {
            return false;
        }
        Composite composite = graphics.getComposite();
        if (!(composite instanceof AlphaComposite) || ((AlphaComposite) composite).getRule() != AlphaComposite.SRC_OVER)
        {
            return false;
        }
        return fill || graphics.getStroke() instanceof BasicStroke;
    }

    private int argb()
    {
        Color color = (Color) graphics.getPaint();
        float extra = ((AlphaComposite) graphics.getComposite()).getAlpha();
        int alpha = Math.round(color.getAlpha() * extra);
        return (alpha << 24) | (color.getRGB() & 0xFFFFFF);
    }

    private double[] canvasOffset()
    {
        if (canvas == null)
        {
            return null;
        }
        AffineTransform toCanvas = new AffineTransform(canvas);
        toCanvas.concatenate(graphics.getTransform());
        if (Math.abs(toCanvas.getScaleX() - 1) > EPSILON || Math.abs(toCanvas.getScaleY() - 1) > EPSILON || Math.abs(toCanvas.getShearX()) > EPSILON || Math.abs(toCanvas.getShearY()) > EPSILON)
        {
            return null;
        }
        return new double[]{toCanvas.getTranslateX(), toCanvas.getTranslateY()};
    }

    private static AffineTransform inverse(AffineTransform transform)
    {
        try
        {
            return transform.createInverse();
        }
        catch (NoninvertibleTransformException e)
        {
            return null;
        }
    }

    /**
     * @return the graphics this draws through
     */
    Graphics2D target()
    {
        return graphics;
    }

    @Override
    public void draw3DRect(int x, int y, int width, int height, boolean raised)
    {
        graphics.draw3DRect(x, y, width, height, raised);
    }

    @Override
    public void fill3DRect(int x, int y, int width, int height, boolean raised)
    {
        graphics.fill3DRect(x, y, width, height, raised);
    }

    @Override
    public FontMetrics getFontMetrics()
    {
        return graphics.getFontMetrics();
    }

    @Override
    public Rectangle getClipBounds(Rectangle rectangle)
    {
        return graphics.getClipBounds(rectangle);
    }

    @Override
    public boolean hitClip(int x, int y, int width, int height)
    {
        return graphics.hitClip(x, y, width, height);
    }
}
