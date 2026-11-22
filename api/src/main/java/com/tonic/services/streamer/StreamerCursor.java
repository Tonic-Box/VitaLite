package com.tonic.services.streamer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * The cursor the mirror draws: the image RuneLite last set as its cursor, such as the Custom Cursor plugin's, or a plain arrow.
 */
public final class StreamerCursor
{
    private static final BufferedImage ARROW = arrow();

    private static volatile BufferedImage image;

    private StreamerCursor()
    {
    }

    /**
     * Records the image RuneLite set as its cursor. Called by the client UI's cursor hooks.
     *
     * @param cursor the cursor image, or null when RuneLite went back to the system cursor
     */
    public static void set(BufferedImage cursor)
    {
        image = cursor;
    }

    /**
     * Records that RuneLite went back to the system cursor. Called by the client UI's cursor hooks.
     */
    public static void reset()
    {
        image = null;
    }

    /**
     * @return the image to draw at the pointer, with its hotspot at the top left
     */
    static BufferedImage current()
    {
        BufferedImage cursor = image;
        return cursor != null ? cursor : ARROW;
    }

    private static BufferedImage arrow()
    {
        BufferedImage arrow = new BufferedImage(13, 21, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = arrow.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Polygon shape = new Polygon(new int[]{1, 1, 5, 8, 10, 7, 12}, new int[]{1, 17, 13, 19, 18, 12, 12}, 7);
        g.setColor(Color.WHITE);
        g.fillPolygon(shape);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1f));
        g.drawPolygon(shape);
        g.dispose();
        return arrow;
    }
}
