package com.tonic.services.streamer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;

/**
 * Graphics the streamer mirror prints Swing through, drawing each image VitaLite recoloured as stock RuneLite shows it and the streamer name in place of the player's.
 */
final class MirrorPrintGraphics extends DelegatingGraphics
{
    MirrorPrintGraphics(Graphics2D graphics)
    {
        super(graphics, true);
    }

    @Override
    protected Graphics2D wrap(Graphics2D child)
    {
        return new MirrorPrintGraphics(child);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, ImageObserver observer)
    {
        return graphics.drawImage(MirrorImages.forMirror(image), x, y, observer);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height, ImageObserver observer)
    {
        return graphics.drawImage(MirrorImages.forMirror(image), x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, Color background, ImageObserver observer)
    {
        return graphics.drawImage(MirrorImages.forMirror(image), x, y, background, observer);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height, Color background, ImageObserver observer)
    {
        return graphics.drawImage(MirrorImages.forMirror(image), x, y, width, height, background, observer);
    }

    @Override
    public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer)
    {
        return graphics.drawImage(MirrorImages.forMirror(image), dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    @Override
    public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color background, ImageObserver observer)
    {
        return graphics.drawImage(MirrorImages.forMirror(image), dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, background, observer);
    }

    @Override
    public boolean drawImage(Image image, AffineTransform transform, ImageObserver observer)
    {
        return graphics.drawImage(MirrorImages.forMirror(image), transform, observer);
    }

    @Override
    public void drawImage(BufferedImage image, BufferedImageOp op, int x, int y)
    {
        Image shown = MirrorImages.forMirror(image);
        if (shown instanceof BufferedImage)
        {
            graphics.drawImage((BufferedImage) shown, op, x, y);
        }
        else
        {
            graphics.drawImage(image, op, x, y);
        }
    }

    @Override
    public void drawString(String text, int x, int y)
    {
        String shown = StreamerName.replace(text);
        graphics.drawString(shown != null ? shown : text, x, y);
    }

    @Override
    public void drawString(String text, float x, float y)
    {
        String shown = StreamerName.replace(text);
        graphics.drawString(shown != null ? shown : text, x, y);
    }
}
