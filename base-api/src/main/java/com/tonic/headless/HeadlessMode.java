package com.tonic.headless;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.util.ReflectBuilder;
import lombok.Getter;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class HeadlessMode {
    private static final Object clientUI;
    private static final JPanel clientPanel;
    private static final JFrame frame;
    private static final JTabbedPane sidebar;
    private static RestoreSize clientPanelSize;
    private static RestoreSize wrapperPaneSize;
    @Getter
    private static volatile HeadlessMapPanel mapPanel;
    @Getter
    private static volatile boolean mapPanelActive = false;
    private static JLayeredPane wrapperPane;
    private static boolean wrapperInstalled = false;

    private static Object shouldRestoreGpu = null;

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
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> toggleHeadless(headless));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logger.error("Interrupted while toggling headless mode", e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                Logger.error("Failed while toggling headless mode", cause);
            }
            return;
        }

        if (clientUI == null || clientPanel == null || frame == null || sidebar == null) {
            return;
        }

        boolean showMap = Static.getVitaConfig().shouldShowHeadlessMap();

        if (headless && showMap) {
            if (shouldRestoreGpu == null) {
                Object stoppedGpuPlugin = Static.getRuneLite().getPluginManager()
                        .stopPlugin("net.runelite.client.plugins.gpu.GpuPlugin");
                if (stoppedGpuPlugin != null) {
                    shouldRestoreGpu = stoppedGpuPlugin;
                }
            }

            if (clientPanelSize != null) {
                clientPanelSize.restore(clientPanel);
                clientPanelSize = null;
            }
            if (wrapperPane != null && wrapperPaneSize != null) {
                wrapperPaneSize.restore(wrapperPane);
                wrapperPaneSize = null;
            }
            clientPanel.setVisible(true);

            if (mapPanel == null) {
                mapPanel = new HeadlessMapPanel();
                mapPanel.setOpaque(true);
            }

            if (!mapPanelActive) {
                Container content = clientPanel.getParent();
                if (content == null) {
                    Logger.info("[HeadlessDebug] ERROR: clientPanel has no parent");
                    return;
                }

                if (!sidebar.isVisible() || sidebar.getSelectedIndex() < 0) {
                    ReflectBuilder.of(clientUI)
                            .method("togglePluginPanel", null, null);
                }

                if (!wrapperInstalled) {
                    int clientIndex = -1;
                    for (int i = 0; i < content.getComponentCount(); i++) {
                        if (content.getComponent(i) == clientPanel) {
                            clientIndex = i;
                            break;
                        }
                    }

                    if (clientIndex < 0) {
                        Logger.error("[HeadlessDebug] ERROR: clientPanel not found in content");
                        return;
                    }

                    wrapperPane = new JLayeredPane();
                    wrapperPane.setLayout(new OverlayLayout(wrapperPane));

                    Dimension clientSize = clientPanel.getSize();
                    content.remove(clientPanel);

                    clientPanel.setAlignmentX(0.5f);
                    clientPanel.setAlignmentY(0.5f);
                    wrapperPane.add(clientPanel, JLayeredPane.DEFAULT_LAYER);

                    wrapperPane.setPreferredSize(clientSize);
                    wrapperPane.setMinimumSize(clientSize);
                    wrapperPane.setSize(clientSize);

                    content.add(wrapperPane, clientIndex);

                    wrapperInstalled = true;
                    content.revalidate();
                    content.repaint();
                }

                mapPanel.setAlignmentX(0.5f);
                mapPanel.setAlignmentY(0.5f);
                mapPanel.setSize(clientPanel.getSize());
                mapPanel.setPreferredSize(clientPanel.getSize());
                wrapperPane.setSize(clientPanel.getSize());
                wrapperPane.setPreferredSize(clientPanel.getSize());
                wrapperPane.setMinimumSize(clientPanel.getSize());
                wrapperPane.add(mapPanel, JLayeredPane.PALETTE_LAYER);

                mapPanelActive = true;
                wrapperPane.revalidate();
                wrapperPane.repaint();
            }
        } else if (headless) {
            if (mapPanelActive && wrapperPane != null) {
                mapPanel.clearMap();
                wrapperPane.remove(mapPanel);
                mapPanelActive = false;
                wrapperPane.revalidate();
                wrapperPane.repaint();
            }

            clientPanel.setVisible(false);
            if (!sidebar.isVisible() || sidebar.getSelectedIndex() < 0) {
                ReflectBuilder.of(clientUI)
                        .method("togglePluginPanel", null, null);
            }
            if (clientPanelSize == null) {
                clientPanelSize = new RestoreSize(clientPanel);
            }
            clientPanelSize.hide(clientPanel);
            if (wrapperPane != null) {
                if (wrapperPaneSize == null) {
                    wrapperPaneSize = new RestoreSize(wrapperPane);
                }
                wrapperPaneSize.hide(wrapperPane);
                wrapperPane.revalidate();
                wrapperPane.repaint();
            }
        } else {
            if (mapPanelActive && wrapperPane != null) {
                mapPanel.clearMap();
                wrapperPane.remove(mapPanel);
                mapPanelActive = false;

                wrapperPane.revalidate();
                wrapperPane.repaint();
            }
            if (clientPanelSize != null) {
                clientPanelSize.restore(clientPanel);
                clientPanelSize = null;
            }
            if (wrapperPane != null && wrapperPaneSize != null) {
                wrapperPaneSize.restore(wrapperPane);
                wrapperPaneSize = null;
            }

            clientPanel.setVisible(true);
            if (shouldRestoreGpu != null) {
                try {
                    Static.getRuneLite().getPluginManager().startPlugin(shouldRestoreGpu);
                } catch (Exception e) {
                    Logger.error("Failed to restore GPU plugin after leaving headless mode", e);
                } finally {
                    shouldRestoreGpu = null;
                }
            }
        }
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
                mapPanel.updateMap(x, y, plane);
            }
        }
    }

    /**
     * Set the left-click handler for the headless map.
     * Call this from api module to set up click actions with access to MovementAPI etc.
     */
    public static void setMapLeftClickHandler(HeadlessMapPanel.MapClickHandler handler) {
        if (mapPanel != null) {
            mapPanel.setLeftClickHandler(handler);
        }
    }

    /**
     * Set the context menu provider for the headless map.
     * Call this from api module to set up right-click menu with access to MovementAPI etc.
     */
    public static void setMapContextMenuProvider(HeadlessMapPanel.MapContextMenuProvider provider) {
        if (mapPanel != null) {
            mapPanel.setContextMenuProvider(provider);
        }
    }

    /**
     * Set the destination marker on the headless map.
     * The marker will auto-clear when the player reaches it.
     */
    public static void setMapDestination(int x, int y, int plane) {
        if (mapPanel != null) {
            mapPanel.setDestination(x, y, plane);
        }
    }

    /**
     * Clear the destination marker on the headless map.
     */
    public static void clearMapDestination() {
        if (mapPanel != null) {
            mapPanel.clearDestination();
        }
    }

    /**
     * Set the info text provider for the headless map.
     * Call this from api module to provide rich info using API classes.
     */
    public static void setMapInfoProvider(HeadlessMapPanel.MapInfoProvider provider) {
        if (mapPanel != null) {
            mapPanel.setInfoProvider(provider);
        }
    }
}
