package com.tonic.services.streamer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

/**
 * Graphics that draw exactly as the graphics they wrap, after telling MirrorHiding which pixels each draw may cover, worked out generously from the shape, stroke, text or image and the current transform and clip. Can also log every call, so the drawing can be made again elsewhere. The transform and the clip are read from the wrapped graphics once and kept until a call through these graphics changes them, as nothing else draws with the wrapped graphics while these are in use.
 */
final class RecordingGraphics extends DelegatingGraphics
{
    private static final int PAD = 3;
    private static final double LIMIT = 1 << 24;

    private final DrawingLog log;
    private final int number;
    private final Rectangle area = new Rectangle();
    private AffineTransform transform;
    private Rectangle deviceClip;
    private boolean clipKnown;

    RecordingGraphics(Graphics2D graphics, boolean owned, DrawingLog log)
    {
        this(graphics, owned, log, 0);
    }

    private RecordingGraphics(Graphics2D graphics, boolean owned, DrawingLog log, int number)
    {
        super(graphics, owned);
        this.log = log;
        this.number = number;
    }

    @Override
    protected Graphics2D wrap(Graphics2D child)
    {
        return new RecordingGraphics(child, true, log, log == null ? 0 : log.create(number));
    }

    private AffineTransform transform()
    {
        if (transform == null)
        {
            transform = graphics.getTransform();
        }
        return transform;
    }

    private Rectangle deviceClip()
    {
        if (!clipKnown)
        {
            Shape clip = graphics.getClip();
            deviceClip = clip == null ? null : transform().createTransformedShape(clip).getBounds();
            if (deviceClip != null)
            {
                deviceClip.grow(PAD, PAD);
            }
            clipKnown = true;
        }
        return deviceClip;
    }

    private void transformChanged()
    {
        transform = null;
        clipKnown = false;
    }

    private void clipChanged()
    {
        clipKnown = false;
    }

    private void cover(Shape shape, boolean stroked)
    {
        AffineTransform current = transform();
        if (!stroked)
        {
            cover(current.createTransformedShape(shape).getBounds2D());
            return;
        }
        Stroke stroke = graphics.getStroke();
        if (!(stroke instanceof BasicStroke))
        {
            cover(current.createTransformedShape(stroke.createStrokedShape(shape)).getBounds2D());
            return;
        }
        BasicStroke basic = (BasicStroke) stroke;
        double reach = Math.max(basic.getLineWidth(), 1) / 2 * Math.max(basic.getLineJoin() == BasicStroke.JOIN_MITER ? basic.getMiterLimit() : 1, Math.sqrt(2));
        double grow = reach * Math.max(Math.hypot(current.getScaleX(), current.getShearY()), Math.hypot(current.getShearX(), current.getScaleY()));
        Rectangle2D bounds = current.createTransformedShape(shape).getBounds2D();
        cover(new Rectangle2D.Double(bounds.getX() - grow, bounds.getY() - grow, bounds.getWidth() + grow * 2, bounds.getHeight() + grow * 2));
    }

    private void coverPoints(int[] xPoints, int[] yPoints, int count, boolean stroked)
    {
        int points = Math.min(count, Math.min(xPoints.length, yPoints.length));
        if (points <= 0)
        {
            return;
        }
        if (stroked && !(graphics.getStroke() instanceof BasicStroke))
        {
            cover(new Polygon(xPoints, yPoints, points), true);
            return;
        }
        int minX = xPoints[0];
        int maxX = minX;
        int minY = yPoints[0];
        int maxY = minY;
        for (int i = 1; i < points; i++)
        {
            minX = Math.min(minX, xPoints[i]);
            maxX = Math.max(maxX, xPoints[i]);
            minY = Math.min(minY, yPoints[i]);
            maxY = Math.max(maxY, yPoints[i]);
        }
        cover(new Rectangle2D.Double(minX, minY, (double) maxX - minX, (double) maxY - minY), stroked);
    }

    private void coverUser(double x, double y, double width, double height)
    {
        cover(transform().createTransformedShape(new Rectangle2D.Double(x, y, width, height)).getBounds2D());
    }

    private void coverAll()
    {
        Rectangle clip = deviceClip();
        if (clip == null)
        {
            MirrorHiding.touch(new Rectangle(Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, Integer.MAX_VALUE, Integer.MAX_VALUE));
        }
        else
        {
            cover(new Rectangle2D.Double(clip.x, clip.y, clip.width, clip.height));
        }
    }

    private void cover(Rectangle2D device)
    {
        double left = limit(Math.floor(device.getMinX()) - PAD);
        double top = limit(Math.floor(device.getMinY()) - PAD);
        double right = limit(Math.ceil(device.getMaxX()) + PAD + 1);
        double bottom = limit(Math.ceil(device.getMaxY()) + PAD + 1);
        Rectangle clip = deviceClip();
        if (clip != null)
        {
            left = Math.max(left, clip.x);
            top = Math.max(top, clip.y);
            right = Math.min(right, (double) clip.x + clip.width);
            bottom = Math.min(bottom, (double) clip.y + clip.height);
        }
        if (!(left < right && top < bottom))
        {
            return;
        }
        area.setBounds((int) left, (int) top, (int) (right - left), (int) (bottom - top));
        MirrorHiding.touch(area);
    }

    private static double limit(double coordinate)
    {
        return Math.max(-LIMIT, Math.min(LIMIT, coordinate));
    }

    private void coverText(String text, double x, double y)
    {
        Rectangle2D bounds = graphics.getFont().getStringBounds(text, graphics.getFontRenderContext());
        coverUser(x + bounds.getX(), y + bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    private void coverImage(Image image, double x, double y)
    {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        if (width < 0 || height < 0)
        {
            coverAll();
        }
        else
        {
            coverUser(x, y, width, height);
        }
    }

    @Override
    public void draw(Shape shape)
    {
        if (log != null)
        {
            Shape logged = DrawingLog.copy(shape);
            log.add(number, g -> g.draw(logged));
        }
        cover(shape, true);
        graphics.draw(shape);
    }

    @Override
    public void fill(Shape shape)
    {
        if (log != null)
        {
            Shape logged = DrawingLog.copy(shape);
            log.add(number, g -> g.fill(logged));
        }
        cover(shape, false);
        graphics.fill(shape);
    }

    @Override
    public boolean drawImage(Image image, AffineTransform transform, ImageObserver observer)
    {
        if (log != null)
        {
            AffineTransform logged = new AffineTransform(transform);
            log.add(number, g -> g.drawImage(image, logged, observer));
        }
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        if (width < 0 || height < 0)
        {
            coverAll();
        }
        else
        {
            cover(transform.createTransformedShape(new Rectangle2D.Double(0, 0, width, height)), false);
        }
        return graphics.drawImage(image, transform, observer);
    }

    @Override
    public void drawImage(BufferedImage image, BufferedImageOp op, int x, int y)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawImage(image, op, x, y));
        }
        Rectangle2D bounds = op == null ? new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight()) : op.getBounds2D(image);
        coverUser(x + bounds.getX(), y + bounds.getY(), bounds.getWidth(), bounds.getHeight());
        graphics.drawImage(image, op, x, y);
    }

    @Override
    public void drawRenderedImage(RenderedImage image, AffineTransform transform)
    {
        if (log != null)
        {
            AffineTransform logged = new AffineTransform(transform);
            log.add(number, g -> g.drawRenderedImage(image, logged));
        }
        cover(transform.createTransformedShape(new Rectangle2D.Double(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight())), false);
        graphics.drawRenderedImage(image, transform);
    }

    @Override
    public void drawRenderableImage(RenderableImage image, AffineTransform transform)
    {
        if (log != null)
        {
            AffineTransform logged = new AffineTransform(transform);
            log.add(number, g -> g.drawRenderableImage(image, logged));
        }
        coverAll();
        graphics.drawRenderableImage(image, transform);
    }

    @Override
    public void drawString(String text, int x, int y)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawString(text, x, y));
        }
        coverText(text, x, y);
        graphics.drawString(text, x, y);
    }

    @Override
    public void drawString(String text, float x, float y)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawString(text, x, y));
        }
        coverText(text, x, y);
        graphics.drawString(text, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y)
    {
        if (log != null)
        {
            AttributedCharacterIterator logged = (AttributedCharacterIterator) iterator.clone();
            log.add(number, g -> g.drawString((AttributedCharacterIterator) logged.clone(), x, y));
        }
        coverAll();
        graphics.drawString(iterator, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y)
    {
        if (log != null)
        {
            AttributedCharacterIterator logged = (AttributedCharacterIterator) iterator.clone();
            log.add(number, g -> g.drawString((AttributedCharacterIterator) logged.clone(), x, y));
        }
        coverAll();
        graphics.drawString(iterator, x, y);
    }

    @Override
    public void drawGlyphVector(GlyphVector glyphs, float x, float y)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawGlyphVector(glyphs, x, y));
        }
        Rectangle bounds = glyphs.getPixelBounds(graphics.getFontRenderContext(), x, y);
        coverUser(bounds.x, bounds.y, bounds.width, bounds.height);
        graphics.drawGlyphVector(glyphs, x, y);
    }

    @Override
    public void setComposite(Composite composite)
    {
        if (log != null)
        {
            log.add(number, g -> g.setComposite(composite));
        }
        graphics.setComposite(composite);
    }

    @Override
    public void setPaint(Paint paint)
    {
        if (log != null)
        {
            log.add(number, g -> g.setPaint(paint));
        }
        graphics.setPaint(paint);
    }

    @Override
    public void setStroke(Stroke stroke)
    {
        if (log != null)
        {
            log.add(number, g -> g.setStroke(stroke));
        }
        graphics.setStroke(stroke);
    }

    @Override
    public void setRenderingHint(RenderingHints.Key key, Object value)
    {
        if (log != null)
        {
            log.add(number, g -> g.setRenderingHint(key, value));
        }
        graphics.setRenderingHint(key, value);
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints)
    {
        if (log != null)
        {
            Map<?, ?> logged = new HashMap<>(hints);
            log.add(number, g -> g.setRenderingHints(logged));
        }
        graphics.setRenderingHints(hints);
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints)
    {
        if (log != null)
        {
            Map<?, ?> logged = new HashMap<>(hints);
            log.add(number, g -> g.addRenderingHints(logged));
        }
        graphics.addRenderingHints(hints);
    }

    @Override
    public void translate(int x, int y)
    {
        if (log != null)
        {
            log.add(number, g -> g.translate(x, y));
        }
        graphics.translate(x, y);
        transformChanged();
    }

    @Override
    public void translate(double x, double y)
    {
        if (log != null)
        {
            log.add(number, g -> g.translate(x, y));
        }
        graphics.translate(x, y);
        transformChanged();
    }

    @Override
    public void rotate(double theta)
    {
        if (log != null)
        {
            log.add(number, g -> g.rotate(theta));
        }
        graphics.rotate(theta);
        transformChanged();
    }

    @Override
    public void rotate(double theta, double x, double y)
    {
        if (log != null)
        {
            log.add(number, g -> g.rotate(theta, x, y));
        }
        graphics.rotate(theta, x, y);
        transformChanged();
    }

    @Override
    public void scale(double x, double y)
    {
        if (log != null)
        {
            log.add(number, g -> g.scale(x, y));
        }
        graphics.scale(x, y);
        transformChanged();
    }

    @Override
    public void shear(double x, double y)
    {
        if (log != null)
        {
            log.add(number, g -> g.shear(x, y));
        }
        graphics.shear(x, y);
        transformChanged();
    }

    @Override
    public void transform(AffineTransform transform)
    {
        if (log != null)
        {
            AffineTransform logged = new AffineTransform(transform);
            log.add(number, g -> g.transform(logged));
        }
        graphics.transform(transform);
        transformChanged();
    }

    @Override
    public void setTransform(AffineTransform transform)
    {
        if (log != null)
        {
            log.setTransform(number, transform);
        }
        graphics.setTransform(transform);
        transformChanged();
    }

    @Override
    public void setBackground(Color color)
    {
        if (log != null)
        {
            log.add(number, g -> g.setBackground(color));
        }
        graphics.setBackground(color);
    }

    @Override
    public void clip(Shape shape)
    {
        if (log != null)
        {
            Shape logged = shape == null ? null : DrawingLog.copy(shape);
            log.add(number, g -> g.clip(logged));
        }
        graphics.clip(shape);
        clipChanged();
    }

    @Override
    public void setColor(Color color)
    {
        if (log != null)
        {
            log.add(number, g -> g.setColor(color));
        }
        graphics.setColor(color);
    }

    @Override
    public void setPaintMode()
    {
        if (log != null)
        {
            log.add(number, Graphics2D::setPaintMode);
        }
        graphics.setPaintMode();
    }

    @Override
    public void setXORMode(Color color)
    {
        if (log != null)
        {
            log.add(number, g -> g.setXORMode(color));
        }
        graphics.setXORMode(color);
    }

    @Override
    public void setFont(Font font)
    {
        if (log != null)
        {
            log.add(number, g -> g.setFont(font));
        }
        graphics.setFont(font);
    }

    @Override
    public void clipRect(int x, int y, int width, int height)
    {
        if (log != null)
        {
            log.add(number, g -> g.clipRect(x, y, width, height));
        }
        graphics.clipRect(x, y, width, height);
        clipChanged();
    }

    @Override
    public void setClip(int x, int y, int width, int height)
    {
        if (log != null)
        {
            log.add(number, g -> g.setClip(x, y, width, height));
        }
        graphics.setClip(x, y, width, height);
        clipChanged();
    }

    @Override
    public void setClip(Shape clip)
    {
        if (log != null)
        {
            Shape logged = clip == null ? null : DrawingLog.copy(clip);
            log.add(number, g -> g.setClip(logged));
        }
        graphics.setClip(clip);
        clipChanged();
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy)
    {
        if (log != null)
        {
            log.add(number, g -> g.copyArea(x, y, width, height, dx, dy));
        }
        coverUser(x + dx, y + dy, width, height);
        graphics.copyArea(x, y, width, height, dx, dy);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawLine(x1, y1, x2, y2));
        }
        cover(new Line2D.Double(x1, y1, x2, y2), true);
        graphics.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(int x, int y, int width, int height)
    {
        if (log != null)
        {
            log.add(number, g -> g.fillRect(x, y, width, height));
        }
        coverUser(x, y, width, height);
        graphics.fillRect(x, y, width, height);
    }

    @Override
    public void drawRect(int x, int y, int width, int height)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawRect(x, y, width, height));
        }
        cover(new Rectangle2D.Double(x, y, width, height), true);
        graphics.drawRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height)
    {
        if (log != null)
        {
            log.add(number, g -> g.clearRect(x, y, width, height));
        }
        coverUser(x, y, width, height);
        graphics.clearRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawRoundRect(x, y, width, height, arcWidth, arcHeight));
        }
        cover(new RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight), true);
        graphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
    {
        if (log != null)
        {
            log.add(number, g -> g.fillRoundRect(x, y, width, height, arcWidth, arcHeight));
        }
        coverUser(x, y, width, height);
        graphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawOval(int x, int y, int width, int height)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawOval(x, y, width, height));
        }
        cover(new Ellipse2D.Double(x, y, width, height), true);
        graphics.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height)
    {
        if (log != null)
        {
            log.add(number, g -> g.fillOval(x, y, width, height));
        }
        coverUser(x, y, width, height);
        graphics.fillOval(x, y, width, height);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawArc(x, y, width, height, startAngle, arcAngle));
        }
        cover(new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN), true);
        graphics.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle)
    {
        if (log != null)
        {
            log.add(number, g -> g.fillArc(x, y, width, height, startAngle, arcAngle));
        }
        coverUser(x, y, width, height);
        graphics.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int count)
    {
        if (log != null)
        {
            int[] loggedX = xPoints.clone();
            int[] loggedY = yPoints.clone();
            log.add(number, g -> g.drawPolyline(loggedX, loggedY, count));
        }
        coverPoints(xPoints, yPoints, count, true);
        graphics.drawPolyline(xPoints, yPoints, count);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int count)
    {
        if (log != null)
        {
            int[] loggedX = xPoints.clone();
            int[] loggedY = yPoints.clone();
            log.add(number, g -> g.drawPolygon(loggedX, loggedY, count));
        }
        coverPoints(xPoints, yPoints, count, true);
        graphics.drawPolygon(xPoints, yPoints, count);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int count)
    {
        if (log != null)
        {
            int[] loggedX = xPoints.clone();
            int[] loggedY = yPoints.clone();
            log.add(number, g -> g.fillPolygon(loggedX, loggedY, count));
        }
        coverPoints(xPoints, yPoints, count, false);
        graphics.fillPolygon(xPoints, yPoints, count);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, ImageObserver observer)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawImage(image, x, y, observer));
        }
        coverImage(image, x, y);
        return graphics.drawImage(image, x, y, observer);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height, ImageObserver observer)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawImage(image, x, y, width, height, observer));
        }
        coverUser(x, y, width, height);
        return graphics.drawImage(image, x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, Color background, ImageObserver observer)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawImage(image, x, y, background, observer));
        }
        coverImage(image, x, y);
        return graphics.drawImage(image, x, y, background, observer);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height, Color background, ImageObserver observer)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawImage(image, x, y, width, height, background, observer));
        }
        coverUser(x, y, width, height);
        return graphics.drawImage(image, x, y, width, height, background, observer);
    }

    @Override
    public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer));
        }
        coverUser(Math.min(dx1, dx2), Math.min(dy1, dy2), Math.abs(dx2 - dx1), Math.abs(dy2 - dy1));
        return graphics.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    @Override
    public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color background, ImageObserver observer)
    {
        if (log != null)
        {
            log.add(number, g -> g.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, background, observer));
        }
        coverUser(Math.min(dx1, dx2), Math.min(dy1, dy2), Math.abs(dx2 - dx1), Math.abs(dy2 - dy1));
        return graphics.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, background, observer);
    }

    @Override
    public void dispose()
    {
        if (owned() && log != null)
        {
            log.dispose(number);
        }
        super.dispose();
    }
}
