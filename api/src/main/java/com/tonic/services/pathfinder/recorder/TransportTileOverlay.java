package com.tonic.services.pathfinder.recorder;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Map;

/**
 * The tiles transports leave from, filled and labelled with how many leave each, while the recorder shows its tiles; inside an instance every copy of a tile is drawn.
 */
public final class TransportTileOverlay extends Overlay
{
    private static final Color FILL = new Color(0x33, 0x99, 0xFF, 45);
    private static final Color OUTLINE = new Color(0x33, 0x99, 0xFF, 190);
    private static final Color TEXT = Color.WHITE;
    private static final Color TEXT_SHADOW = new Color(0, 0, 0, 190);
    private static final Font FONT = new Font("SansSerif", Font.BOLD, 12);

    private final TransportRecorder recorder;

    /**
     * @param recorder the recorder whose tiles are drawn
     */
    public TransportTileOverlay(TransportRecorder recorder)
    {
        this.recorder = recorder;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(PRIORITY_LOW);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!recorder.showTiles())
        {
            return null;
        }

        Client client = Static.getClient();
        WorldView view = client.getTopLevelWorldView();
        graphics.setFont(FONT);
        FontMetrics metrics = graphics.getFontMetrics();
        for (Map.Entry<WorldPoint, Integer> count : recorder.tiles().entrySet())
        {
            LocalPoint local = LocalPoint.fromWorld(view, count.getKey());
            Polygon polygon = local == null ? null : Perspective.getCanvasTilePoly(client, local);
            if (polygon == null)
            {
                continue;
            }

            graphics.setColor(FILL);
            graphics.fill(polygon);
            graphics.setColor(OUTLINE);
            graphics.draw(polygon);

            String label = String.valueOf(count.getValue());
            Rectangle bounds = polygon.getBounds();
            int x = (int) bounds.getCenterX() - metrics.stringWidth(label) / 2;
            int y = (int) bounds.getCenterY() + metrics.getAscent() / 2;
            graphics.setColor(TEXT_SHADOW);
            graphics.drawString(label, x + 1, y + 1);
            graphics.setColor(TEXT);
            graphics.drawString(label, x, y);
        }
        return null;
    }
}
