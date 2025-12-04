package com.tonic.services;

import com.tonic.Logger;
import com.tonic.data.RegionInfo;
import com.tonic.headless.HeadlessMode;
import com.tonic.services.pathfinder.model.WalkerPath;
import com.tonic.util.ThreadPool;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Handles click interactions for the headless map panel.
 * Provides left-click and right-click context menu functionality.
 */
public class HeadlessMapInteraction {

    /**
     * -- GETTER --
     *  Check if handlers have been initialized.
     */
    @Getter
    private static boolean initialized = false;

    /**
     * Initialize the headless map click handlers.
     * Call this once when the map panel becomes available.
     */
    public static void initialize() {
        if (initialized || HeadlessMode.getMapPanel() == null) {
            return;
        }

        HeadlessMode.setMapLeftClickHandler(HeadlessMapInteraction::onLeftClick);
        HeadlessMode.setMapContextMenuProvider(HeadlessMapInteraction::createContextMenu);
        HeadlessMode.setMapInfoProvider(HeadlessMapInteraction::getInfoLines);

        initialized = true;
        Logger.info("Headless map click handlers initialized");
    }

    /**
     * Reset initialization state (useful if map panel is recreated).
     */
    public static void reset() {
        initialized = false;
    }

    /**
     * Handle left-click on the map - walks to the clicked tile.
     */
    private static void onLeftClick(int worldX, int worldY, int plane) {
        walkTo(worldX, worldY, plane);
    }

    /**
     * Create the right-click context menu for a map location.
     */
    private static JPopupMenu createContextMenu(int worldX, int worldY, int plane) {
        JPopupMenu menu = new JPopupMenu();

        // Walk here option
        JMenuItem walkItem = new JMenuItem("Walk here (" + worldX + ", " + worldY + ")");
        walkItem.addActionListener(e -> walkTo(worldX, worldY, plane));
        menu.add(walkItem);

        // Copy coordinates option
        JMenuItem copyItem = new JMenuItem("Copy coordinates");
        copyItem.addActionListener(e -> copyToClipboard(worldX + ", " + worldY + ", " + plane));
        menu.add(copyItem);

        return menu;
    }

    /**
     * Copy text to the system clipboard.
     */
    private static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(text), null);
    }

    /**
     * Generate info lines for the map overlay.
     * Can access all API classes here for rich data display.
     */
    private static java.util.List<String> getInfoLines(int playerX, int playerY, int plane) {
        int regionId = ((playerX >> 6) << 8) | (playerY >> 6);
        RegionInfo regionInfo = RegionInfo.fromRegion(regionId);
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(String.format("Position: %d, %d, %d", playerX, playerY, plane));
        lines.add(String.format("RegionId: %d", ((playerX >> 6) << 8) | (playerY >> 6)));
        lines.add(String.format("Location: %s", regionInfo != null ? regionInfo.getAreaName() : "Unknown"));

        return lines;
    }

    private static void walkTo(int worldX, int worldY, int plane) {
        if(GameManager.getWalkerPath() != null && !GameManager.getWalkerPath().isDone())
            return;
        ThreadPool.submit(() -> {
            WorldPoint worldPoint = new WorldPoint(worldX, worldY, plane);
            WalkerPath path = WalkerPath.get(worldPoint);
            if(path.getSteps().isEmpty()) {
                return;
            }
            GameManager.setWalkerPath(path);
            HeadlessMode.setMapDestination(worldX, worldY, plane);
        });
    }
}
