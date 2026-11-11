package com.tonic.services.ground;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Arrays;
import java.util.Map;

/**
 * Graphics an overlay layer draws through: lines and polygons whose every corner is a ground point go to the ground pass, everything else is drawn as usual.
 */
final class GroundCapture extends Graphics2D
{
    private static final double EPSILON = 1e-3;
    private static final int MAX_VERTICES = 4096;

    private final Graphics2D graphics;
    private final AffineTransform canvas;
    private final boolean owned;

    GroundCapture(Graphics2D graphics)
    {
        this(graphics, inverse(graphics.getTransform()), false);
    }

    private GroundCapture(Graphics2D graphics, AffineTransform canvas, boolean owned)
    {
        this.graphics = graphics;
        this.canvas = canvas;
        this.owned = owned;
    }

    @Override
    public void draw(Shape shape)
    {
        if (!capture(shape, false))
        {
            graphics.draw(shape);
        }
    }

    @Override
    public void fill(Shape shape)
    {
        if (!capture(shape, true))
        {
            graphics.fill(shape);
        }
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int count)
    {
        if (!capture(xPoints, yPoints, count, true, false))
        {
            graphics.drawPolygon(xPoints, yPoints, count);
        }
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int count)
    {
        if (!capture(xPoints, yPoints, count, true, true))
        {
            graphics.fillPolygon(xPoints, yPoints, count);
        }
    }

    @Override
    public void drawPolygon(Polygon polygon)
    {
        drawPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
    }

    @Override
    public void fillPolygon(Polygon polygon)
    {
        fillPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int count)
    {
        if (!capture(xPoints, yPoints, count, false, false))
        {
            graphics.drawPolyline(xPoints, yPoints, count);
        }
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2)
    {
        if (!capture(new int[]{x1, x2}, new int[]{y1, y2}, 2, false, false))
        {
            graphics.drawLine(x1, y1, x2, y2);
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

    @Override
    public Graphics create()
    {
        return new GroundCapture((Graphics2D) graphics.create(), canvas, true);
    }

    @Override
    public void dispose()
    {
        if (owned)
        {
            graphics.dispose();
        }
    }

    @Override
    public void drawRect(int x, int y, int width, int height)
    {
        graphics.drawRect(x, y, width, height);
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
    public boolean drawImage(Image image, AffineTransform transform, ImageObserver observer)
    {
        return graphics.drawImage(image, transform, observer);
    }

    @Override
    public void drawImage(BufferedImage image, BufferedImageOp op, int x, int y)
    {
        graphics.drawImage(image, op, x, y);
    }

    @Override
    public void drawRenderedImage(RenderedImage image, AffineTransform transform)
    {
        graphics.drawRenderedImage(image, transform);
    }

    @Override
    public void drawRenderableImage(RenderableImage image, AffineTransform transform)
    {
        graphics.drawRenderableImage(image, transform);
    }

    @Override
    public void drawString(String text, int x, int y)
    {
        graphics.drawString(text, x, y);
    }

    @Override
    public void drawString(String text, float x, float y)
    {
        graphics.drawString(text, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y)
    {
        graphics.drawString(iterator, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y)
    {
        graphics.drawString(iterator, x, y);
    }

    @Override
    public void drawGlyphVector(GlyphVector glyphs, float x, float y)
    {
        graphics.drawGlyphVector(glyphs, x, y);
    }

    @Override
    public boolean hit(Rectangle rectangle, Shape shape, boolean onStroke)
    {
        return graphics.hit(rectangle, shape, onStroke);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration()
    {
        return graphics.getDeviceConfiguration();
    }

    @Override
    public void setComposite(Composite composite)
    {
        graphics.setComposite(composite);
    }

    @Override
    public void setPaint(Paint paint)
    {
        graphics.setPaint(paint);
    }

    @Override
    public void setStroke(Stroke stroke)
    {
        graphics.setStroke(stroke);
    }

    @Override
    public void setRenderingHint(RenderingHints.Key key, Object value)
    {
        graphics.setRenderingHint(key, value);
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key key)
    {
        return graphics.getRenderingHint(key);
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints)
    {
        graphics.setRenderingHints(hints);
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints)
    {
        graphics.addRenderingHints(hints);
    }

    @Override
    public RenderingHints getRenderingHints()
    {
        return graphics.getRenderingHints();
    }

    @Override
    public void translate(int x, int y)
    {
        graphics.translate(x, y);
    }

    @Override
    public void translate(double x, double y)
    {
        graphics.translate(x, y);
    }

    @Override
    public void rotate(double theta)
    {
        graphics.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y)
    {
        graphics.rotate(theta, x, y);
    }

    @Override
    public void scale(double x, double y)
    {
        graphics.scale(x, y);
    }

    @Override
    public void shear(double x, double y)
    {
        graphics.shear(x, y);
    }

    @Override
    public void transform(AffineTransform transform)
    {
        graphics.transform(transform);
    }

    @Override
    public void setTransform(AffineTransform transform)
    {
        graphics.setTransform(transform);
    }

    @Override
    public AffineTransform getTransform()
    {
        return graphics.getTransform();
    }

    @Override
    public Paint getPaint()
    {
        return graphics.getPaint();
    }

    @Override
    public Composite getComposite()
    {
        return graphics.getComposite();
    }

    @Override
    public void setBackground(Color color)
    {
        graphics.setBackground(color);
    }

    @Override
    public Color getBackground()
    {
        return graphics.getBackground();
    }

    @Override
    public Stroke getStroke()
    {
        return graphics.getStroke();
    }

    @Override
    public void clip(Shape shape)
    {
        graphics.clip(shape);
    }

    @Override
    public FontRenderContext getFontRenderContext()
    {
        return graphics.getFontRenderContext();
    }

    @Override
    public Color getColor()
    {
        return graphics.getColor();
    }

    @Override
    public void setColor(Color color)
    {
        graphics.setColor(color);
    }

    @Override
    public void setPaintMode()
    {
        graphics.setPaintMode();
    }

    @Override
    public void setXORMode(Color color)
    {
        graphics.setXORMode(color);
    }

    @Override
    public Font getFont()
    {
        return graphics.getFont();
    }

    @Override
    public void setFont(Font font)
    {
        graphics.setFont(font);
    }

    @Override
    public FontMetrics getFontMetrics()
    {
        return graphics.getFontMetrics();
    }

    @Override
    public FontMetrics getFontMetrics(Font font)
    {
        return graphics.getFontMetrics(font);
    }

    @Override
    public Rectangle getClipBounds()
    {
        return graphics.getClipBounds();
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

    @Override
    public void clipRect(int x, int y, int width, int height)
    {
        graphics.clipRect(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height)
    {
        graphics.setClip(x, y, width, height);
    }

    @Override
    public Shape getClip()
    {
        return graphics.getClip();
    }

    @Override
    public void setClip(Shape clip)
    {
        graphics.setClip(clip);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy)
    {
        graphics.copyArea(x, y, width, height, dx, dy);
    }

    @Override
    public void fillRect(int x, int y, int width, int height)
    {
        graphics.fillRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height)
    {
        graphics.clearRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
    {
        graphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
    {
        graphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawOval(int x, int y, int width, int height)
    {
        graphics.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height)
    {
        graphics.fillOval(x, y, width, height);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
    {
        graphics.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle)
    {
        graphics.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, ImageObserver observer)
    {
        return graphics.drawImage(image, x, y, observer);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height, ImageObserver observer)
    {
        return graphics.drawImage(image, x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, Color background, ImageObserver observer)
    {
        return graphics.drawImage(image, x, y, background, observer);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height, Color background, ImageObserver observer)
    {
        return graphics.drawImage(image, x, y, width, height, background, observer);
    }

    @Override
    public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer)
    {
        return graphics.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    @Override
    public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color background, ImageObserver observer)
    {
        return graphics.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, background, observer);
    }
}
