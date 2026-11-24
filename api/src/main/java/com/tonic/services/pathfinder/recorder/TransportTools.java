package com.tonic.services.pathfinder.recorder;

import com.tonic.Static;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.ui.TransportEditorFrame;
import com.tonic.services.pathfinder.ui.TransportEditorSettings;
import com.tonic.services.streamer.Streamer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * Installs the transport recorder and editor in dev runs: the recorder's event subscribers, its overlays, the Transport Recorder sidebar panel and the editor's shared staging store and settings.
 */
public final class TransportTools
{
    private static final int BUTTON_PRIORITY = 12;
    private static final int ICON_SIZE = 16;

    private static TransportRecordStore store;
    private static TransportEditorSettings settings;
    private static volatile TransportRecorder recorder;

    private TransportTools()
    {
    }

    /**
     * Creates the recorder and wires it, its overlays, its sidebar panel and the editor into the client; does nothing outside dev runs.
     */
    public static void install()
    {
        if (Static.isRunningFromShadedJar() || recorder != null)
        {
            return;
        }

        TransportRecorder created = new TransportRecorder(store());
        created.editor(TransportEditorFrame::open);
        created.fulfilmentWanted(TransportEditorFrame::showing);
        recorder = created;
        Static.getRuneLite().getEventBus().register(created);

        OverlayManager overlayManager = Static.getInjector().getInstance(OverlayManager.class);
        overlayManager.add(new RecorderOverlay(created, () -> settings().recorderOverlay()));
        overlayManager.add(new TransportTileOverlay(created));

        Streamer.whenUiReady(() -> SwingUtilities.invokeLater(() -> addPanel(created)));
    }

    /**
     * @return the staging store shared by the recorder and the editor
     */
    public static synchronized TransportRecordStore store()
    {
        if (store == null)
        {
            store = new TransportRecordStore();
        }
        return store;
    }

    /**
     * @return the editor's and recorder overlay's settings
     */
    public static synchronized TransportEditorSettings settings()
    {
        if (settings == null)
        {
            settings = new TransportEditorSettings(Static.getInjector().getInstance(ConfigManager.class));
        }
        return settings;
    }

    /**
     * @param transport a transport
     * @return whether its requirements held on the last tick the recorder worked them out, empty when it has none or there is no recorder
     */
    public static Optional<Boolean> fulfilled(Transport transport)
    {
        TransportRecorder current = recorder;
        return current == null ? Optional.empty() : current.fulfilled(transport);
    }

    private static void addPanel(TransportRecorder created)
    {
        TransportReviewPanel panel = new TransportReviewPanel(created, TransportEditorFrame::open, TransportEditorFrame::open, () -> settings().recorderOverlay(), shown -> settings().recorderOverlay(shown));
        NavigationButton button = NavigationButton.builder().tooltip("Transport Recorder").icon(icon()).priority(BUTTON_PRIORITY).panel(panel).build();
        Static.getInjector().getInstance(ClientToolbar.class).addNavigation(button);
    }

    private static BufferedImage icon()
    {
        BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(0xE0604C));
        graphics.fillOval(4, 4, 8, 8);
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.setColor(new Color(0xC8C8C8));
        graphics.drawOval(1, 1, 13, 13);
        graphics.dispose();
        return image;
    }
}
