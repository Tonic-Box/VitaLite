package com.tonic.services.streamer;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * Graphics that pass every call on to other graphics, for the streamer mirror's graphics that change a few calls. Graphics made from them wrap the graphics made from the ones they pass to, and own them. Graphics they were only lent, such as RuneLite's long-lived frame graphics, are never disposed, not even when the wrapper is collected, as java.awt.Graphics disposes itself on finalization.
 */
public abstract class DelegatingGraphics extends Graphics2D
{
    /**
     * The graphics every call is passed to.
     */
    protected final Graphics2D graphics;

    private final boolean owned;

    /**
     * @param graphics the graphics every call is passed to
     * @param owned whether these graphics own them, so disposing these disposes them too
     */
    protected DelegatingGraphics(Graphics2D graphics, boolean owned)
    {
        this.graphics = graphics;
        this.owned = owned;
    }

    /**
     * @param child graphics made from the wrapped graphics
     * @return the same kind of graphics around them, owning them
     */
    protected abstract Graphics2D wrap(Graphics2D child);

    /**
     * @return whether these graphics own the graphics they pass to, so disposing them disposes those too
     */
    protected boolean owned()
    {
        return owned;
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
    public Graphics create()
    {
        return wrap((Graphics2D) graphics.create());
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
    public void draw(Shape shape)
    {
        graphics.draw(shape);
    }

    @Override
    public void fill(Shape shape)
    {
        graphics.fill(shape);
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
    public void drawLine(int x1, int y1, int x2, int y2)
    {
        graphics.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void drawRect(int x, int y, int width, int height)
    {
        graphics.drawRect(x, y, width, height);
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
    public void drawPolyline(int[] xPoints, int[] yPoints, int count)
    {
        graphics.drawPolyline(xPoints, yPoints, count);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int count)
    {
        graphics.drawPolygon(xPoints, yPoints, count);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int count)
    {
        graphics.fillPolygon(xPoints, yPoints, count);
    }
}
