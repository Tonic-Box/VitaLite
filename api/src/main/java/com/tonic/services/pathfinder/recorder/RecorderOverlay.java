package com.tonic.services.pathfinder.recorder;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.function.BooleanSupplier;

/**
 * What the transport recorder is doing and how much it has staged, shown while it records.
 */
public final class RecorderOverlay extends OverlayPanel
{
    private static final Color RECORDING = new Color(0xE0604C);
    private static final Color WAITING = new Color(0x9A9A9A);
    private static final Color ACTIVE = new Color(0x88C070);
    private static final Color MUTED = new Color(0x9AA0A6);
    private static final int PANEL_WIDTH = 240;

    private final TransportRecorder recorder;
    private final BooleanSupplier shown;

    /**
     * @param recorder what to report on
     * @param shown whether the overlay is switched on
     */
    public RecorderOverlay(TransportRecorder recorder, BooleanSupplier shown)
    {
        this.recorder = recorder;
        this.shown = shown;
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(PANEL_WIDTH, 0));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!recorder.recording() || !shown.getAsBoolean())
        {
            return null;
        }

        boolean active = recorder.active();
        panelComponent.getChildren().add(TitleComponent.builder().text("Transport Recorder").color(RECORDING).build());
        panelComponent.getChildren().add(LineComponent.builder().left("Staged").right(String.valueOf(recorder.store().size())).build());
        panelComponent.getChildren().add(LineComponent.builder().left("Status").right(active ? recorder.status() : "waiting for a transport").rightColor(active ? ACTIVE : WAITING).build());
        panelComponent.getChildren().add(LineComponent.builder().left("Tiles").right(recorder.showTiles() ? "shown" : "hidden").rightColor(MUTED).build());
        return super.render(graphics);
    }
}
