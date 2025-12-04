package com.tonic.headless;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.util.ReflectBuilder;
import javax.swing.*;
import java.awt.*;

public class HeadlessMode {
    private static final Object clientUI;
    private static final JPanel clientPanel;
    private static final JFrame frame;
    private static final JTabbedPane sidebar;
    private static RestoreSize clientPanelSize;

    // Map panel for headless mode visualization - overlay approach
    private static HeadlessMapPanel mapPanel;
    private static boolean mapPanelActive = false;

    // Wrapper panel that holds both clientPanel and mapPanel as overlays
    private static JLayeredPane wrapperPane;
    private static boolean wrapperInstalled = false;

    static
    {
        clientUI = ReflectBuilder.runelite()
                .staticField("rlInstance")
                .field("clientUI")
                .get();

        clientPanel = ReflectBuilder.of(clientUI)
                .field("clientPanel")
                .get();

        frame = ReflectBuilder.of(clientUI)
                .field("frame")
                .get();

        sidebar = ReflectBuilder.of(clientUI)
                .field("sidebar")
                .get();
    }

    public static void toggleHeadless(boolean headless) {
        if (clientUI == null || clientPanel == null || frame == null || sidebar == null) {
            return;
        }

        boolean showMap = Static.getVitaConfig().shouldShowHeadlessMap();

        Logger.info("[HeadlessDebug] === Toggle headless=" + headless + " showMap=" + showMap + " ===");

        if (headless && showMap) {
            // OVERLAY APPROACH: Don't remove clientPanel from hierarchy
            // Instead, overlay mapPanel on top using JLayeredPane

            if (mapPanel == null) {
                mapPanel = new HeadlessMapPanel();
                mapPanel.setOpaque(true);
                Logger.info("[HeadlessDebug] Created new HeadlessMapPanel");
            }

            if (!mapPanelActive) {
                Container content = clientPanel.getParent();
                if (content == null) {
                    Logger.info("[HeadlessDebug] ERROR: clientPanel has no parent");
                    return;
                }

                // Ensure sidebar is visible
                if (!sidebar.isVisible() || sidebar.getSelectedIndex() < 0) {
                    ReflectBuilder.of(clientUI)
                            .method("togglePluginPanel", null, null);
                }

                if (!wrapperInstalled) {
                    // First time: Install the wrapper JLayeredPane
                    // This replaces clientPanel at index 0 with a layered pane containing both
                    int clientIndex = -1;
                    for (int i = 0; i < content.getComponentCount(); i++) {
                        if (content.getComponent(i) == clientPanel) {
                            clientIndex = i;
                            break;
                        }
                    }

                    if (clientIndex < 0) {
                        Logger.info("[HeadlessDebug] ERROR: clientPanel not found in content");
                        return;
                    }

                    Logger.info("[HeadlessDebug] Installing wrapper at index " + clientIndex);

                    // Create layered pane
                    wrapperPane = new JLayeredPane();
                    wrapperPane.setLayout(new OverlayLayout(wrapperPane));

                    // Remove clientPanel, add wrapper, add clientPanel to wrapper
                    Dimension clientSize = clientPanel.getSize();
                    content.remove(clientPanel);

                    // Add clientPanel to bottom layer
                    clientPanel.setAlignmentX(0.5f);
                    clientPanel.setAlignmentY(0.5f);
                    wrapperPane.add(clientPanel, JLayeredPane.DEFAULT_LAYER);

                    // Set wrapper size to match
                    wrapperPane.setPreferredSize(clientSize);
                    wrapperPane.setMinimumSize(clientSize);
                    wrapperPane.setSize(clientSize);

                    // Add wrapper to content at original position
                    content.add(wrapperPane, clientIndex);

                    wrapperInstalled = true;
                    content.revalidate();
                    content.repaint();

                    Logger.info("[HeadlessDebug] Wrapper installed, clientPanel in DEFAULT_LAYER");
                }

                // Add mapPanel to top layer (above clientPanel)
                mapPanel.setAlignmentX(0.5f);
                mapPanel.setAlignmentY(0.5f);
                mapPanel.setSize(clientPanel.getSize());
                mapPanel.setPreferredSize(clientPanel.getSize());
                wrapperPane.add(mapPanel, JLayeredPane.PALETTE_LAYER);

                mapPanelActive = true;
                wrapperPane.revalidate();
                wrapperPane.repaint();

                Logger.info("[HeadlessDebug] MapPanel added to PALETTE_LAYER (overlay on top)");
            }
        } else if (headless && !showMap) {
            // Original headless behavior - just hide/shrink client panel
            clientPanel.setVisible(false);
            if (!sidebar.isVisible() || sidebar.getSelectedIndex() < 0) {
                ReflectBuilder.of(clientUI)
                        .method("togglePluginPanel", null, null);
            }
            clientPanelSize = new RestoreSize(clientPanel);
            clientPanelSize.hide(clientPanel);
        } else {
            // Restoring from headless mode
            Logger.info("[HeadlessDebug] RESTORE: mapPanelActive=" + mapPanelActive);

            if (mapPanelActive && wrapperPane != null) {
                // Simply remove the overlay - clientPanel never left the hierarchy!
                mapPanel.clearMap();
                wrapperPane.remove(mapPanel);
                mapPanelActive = false;

                wrapperPane.revalidate();
                wrapperPane.repaint();

                Logger.info("[HeadlessDebug] MapPanel removed from overlay, clientPanel was never removed");
            } else if (clientPanelSize != null) {
                // Restore non-map headless mode size (was hidden, not swapped)
                clientPanelSize.restore(clientPanel);
            }

            // Restore client panel visibility
            clientPanel.setVisible(true);
            Logger.info("[HeadlessDebug] Set clientPanel.visible=true");
        }

        Logger.info("[HeadlessDebug] === Toggle complete ===");
    }

    /**
     * Update the headless map with current player position.
     * Called from GameManager on each game tick.
     *
     * @param x Player world X coordinate
     * @param y Player world Y coordinate
     * @param plane Player plane/level
     * @param collisionAccessor Accessor for collision flags
     */
    public static void updateMap(int x, int y, int plane, HeadlessMapPanel.CollisionMapAccessor collisionAccessor) {
        if (mapPanel != null && mapPanelActive && Static.isHeadless() && Static.getVitaConfig().shouldShowHeadlessMap()) {
            if (mapPanel.getWidth() > 0 && mapPanel.getHeight() > 0) {
                mapPanel.setCollisionAccessor(collisionAccessor);
                mapPanel.setInfoText(String.format("Position: %d, %d, %d", x, y, plane));
                mapPanel.updateMap(x, y, plane);
            }
        }
    }

    /**
     * Get the map panel for direct access if needed.
     */
    public static HeadlessMapPanel getMapPanel() {
        return mapPanel;
    }

    /**
     * Check if the map panel is currently active.
     */
    public static boolean isMapPanelActive() {
        return mapPanelActive;
    }
}
