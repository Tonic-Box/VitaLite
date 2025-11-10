package com.tonic.services.mouse;

import com.tonic.services.mouserecorder.MovementVisualization;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import java.awt.*;

/**
 * Overlay class to render movement visualizations
 * Register this with your plugin's overlay manager
 */
public class MovementVisualizationOverlay extends Overlay
{
    public MovementVisualizationOverlay()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        MovementVisualization.render(graphics);
        return null;
    }
}