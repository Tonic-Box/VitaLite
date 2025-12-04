package com.tonic.headless;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * A JPanel that renders a simple collision map centered on the player.
 * Uses direct pixel manipulation for performance.
 * Supports dragging to pan view and plane selection.
 */
public class HeadlessMapPanel extends JPanel {

    // Size matching ClientPanel (765x503) - required for RuneLite's custom Layout manager
    private static final Dimension GAME_FIXED_SIZE = new Dimension(765, 503);

    // Colors (ARGB format for BufferedImage TYPE_INT_ARGB)
    private static final int COLOR_BACKGROUND = 0xFFF0F0F0;  // Light gray (passable)
    private static final int COLOR_BLOCKED = 0xFFCC0000;     // Red (fully blocked)
    private static final int COLOR_WALL = 0xFF000000;        // Black (walls)
    private static final int COLOR_PLAYER = 0xFF00DD00;      // Green (player tile)
    private static final int COLOR_DESTINATION = 0xFF0088FF;  // Blue (walk target)

    // Info overlay colors
    private static final Color INFO_BG = new Color(30, 30, 35, 200);
    private static final Color INFO_TEXT = new Color(220, 220, 225);
    private static final Font INFO_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    // Button styling
    private static final int BUTTON_SIZE = 24;
    private static final int BUTTON_MARGIN = 4;
    private static final int BUTTON_PADDING = 8;
    private static final Color BUTTON_ACTIVE = new Color(0, 180, 0, 200);
    private static final Color BUTTON_INACTIVE = new Color(60, 60, 65, 200);
    private static final Color BUTTON_SELECTED = new Color(0, 120, 200, 200);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 12);

    // Collision flag constants (matching Flags class in api module)
    private static final byte FLAG_NORTH = 0x2;
    private static final byte FLAG_EAST = 0x10;
    private static final byte FLAG_SOUTH = 0x40;
    private static final byte FLAG_WEST = 0x8;
    private static final byte FLAG_ALL = (byte) 0xFF;
    private static final byte FLAG_NONE = 0x0;

    // Rendering state
    private BufferedImage mapImage;
    private int[] pixels;
    private int imageWidth;
    private int imageHeight;
    private int tileSize = 4;  // Pixels per tile
    private int tilesX;        // Number of tiles visible horizontally
    private int tilesY;        // Number of tiles visible vertically

    // Zoom control
    private static final int MIN_TILE_SIZE = 1;
    private static final int MAX_TILE_SIZE = 16;
    private int zoomTileSize = 4;  // User-controlled tile size (pixels per tile)

    // Last known player position
    private int lastPlayerX, lastPlayerY, lastPlane;

    // View state - separate from player position for drag support
    private int viewCenterX, viewCenterY;  // Current view center (world coords)
    private int viewPlane;                  // Currently displayed plane
    private boolean lockedToPlayer = true;  // Whether view follows player

    // Drag state
    private int dragStartX, dragStartY;           // Screen coords where drag started
    private int dragStartViewX, dragStartViewY;   // View center when drag started
    private boolean isDragging = false;

    // Button bounds for click detection
    private Rectangle lockButtonBounds;
    private final Rectangle[] planeButtonBounds = new Rectangle[4];

    // Destination marker (blue tile showing walk target)
    private int destX = -1, destY = -1, destPlane = -1;  // -1 = no destination

    // Info overlay - extensible list of info lines
    private final java.util.List<String> infoLines = new java.util.ArrayList<>();

    // Collision map accessor (set via reflection from api module)
    private CollisionMapAccessor collisionAccessor;

    // Click handlers
    private MapClickHandler leftClickHandler;
    private MapContextMenuProvider contextMenuProvider;
    private MapInfoProvider infoProvider;

    /**
     * Functional interface for collision map access.
     * This allows the api module to provide the collision lookup without direct dependency.
     */
    @FunctionalInterface
    public interface CollisionMapAccessor {
        byte getFlags(int x, int y, int plane);
    }

    /**
     * Functional interface for handling left-click on the map.
     */
    @FunctionalInterface
    public interface MapClickHandler {
        void onClick(int worldX, int worldY, int plane);
    }

    /**
     * Functional interface for providing right-click context menu.
     */
    @FunctionalInterface
    public interface MapContextMenuProvider {
        JPopupMenu getContextMenu(int worldX, int worldY, int plane);
    }

    /**
     * Functional interface for providing info text to display on the map.
     */
    @FunctionalInterface
    public interface MapInfoProvider {
        java.util.List<String> getInfoLines(int playerX, int playerY, int plane);
    }

    public HeadlessMapPanel() {
        // Match ClientPanel's size contract for RuneLite's custom Layout manager
        setSize(GAME_FIXED_SIZE);
        setMinimumSize(GAME_FIXED_SIZE);
        setPreferredSize(GAME_FIXED_SIZE);

        setBackground(new Color(0xF0F0F0));
        setDoubleBuffered(true);

        // Recalculate image size on resize
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                recreateImage();
            }
        });

        // Mouse wheel zoom
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int rotation = e.getWheelRotation();
                if (rotation < 0) {
                    // Scroll up = zoom in (larger tiles)
                    zoomTileSize = Math.min(MAX_TILE_SIZE, zoomTileSize + 1);
                } else {
                    // Scroll down = zoom out (smaller tiles)
                    zoomTileSize = Math.max(MIN_TILE_SIZE, zoomTileSize - 1);
                }
                recreateImage();
                // Immediately redraw with current view position
                if (collisionAccessor != null) {
                    redrawMap();
                }
            }
        });

        // Mouse drag handling
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    // Calculate drag delta in tiles
                    int deltaX = (e.getX() - dragStartX) / tileSize;
                    int deltaY = (e.getY() - dragStartY) / tileSize;

                    // Update view center (invert Y since screen Y is opposite world Y)
                    viewCenterX = dragStartViewX - deltaX;
                    viewCenterY = dragStartViewY + deltaY;

                    // Redraw at new position
                    redrawMap();
                }
            }
        });

        // Mouse click handlers for map interaction
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Check if clicking on a button first
                if (handleButtonClick(e.getX(), e.getY())) {
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
                    // Start drag
                    isDragging = true;
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                    dragStartViewX = viewCenterX;
                    dragStartViewY = viewCenterY;

                    // Detach from player tracking
                    lockedToPlayer = false;
                }
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Check if this was a click (not a drag) for left-click handler
                if (isDragging && SwingUtilities.isLeftMouseButton(e)) {
                    int dragDistX = Math.abs(e.getX() - dragStartX);
                    int dragDistY = Math.abs(e.getY() - dragStartY);

                    // If minimal movement, treat as click
                    if (dragDistX < 5 && dragDistY < 5 && leftClickHandler != null) {
                        int[] worldCoords = screenToWorld(e.getX(), e.getY());
                        if (worldCoords != null) {
                            leftClickHandler.onClick(worldCoords[0], worldCoords[1], worldCoords[2]);
                        }
                    }
                }
                isDragging = false;
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger() && contextMenuProvider != null) {
                    int[] worldCoords = screenToWorld(e.getX(), e.getY());
                    if (worldCoords == null) return;

                    JPopupMenu menu = contextMenuProvider.getContextMenu(
                        worldCoords[0], worldCoords[1], worldCoords[2]);
                    if (menu != null) {
                        // Close menu when mouse exits
                        menu.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseExited(MouseEvent evt) {
                                // Only close if mouse actually left the menu bounds
                                if (!menu.getBounds().contains(evt.getPoint())) {
                                    menu.setVisible(false);
                                }
                            }
                        });
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }

    /**
     * Handle button clicks. Returns true if a button was clicked.
     */
    private boolean handleButtonClick(int x, int y) {
        // Check lock button
        if (lockButtonBounds != null && lockButtonBounds.contains(x, y)) {
            lockToPlayer();
            return true;
        }

        // Check plane buttons
        for (int i = 0; i < 4; i++) {
            if (planeButtonBounds[i] != null && planeButtonBounds[i].contains(x, y)) {
                setViewPlane(i);
                return true;
            }
        }

        return false;
    }

    /**
     * Lock the view back to player position and plane.
     */
    public void lockToPlayer() {
        lockedToPlayer = true;
        viewCenterX = lastPlayerX;
        viewCenterY = lastPlayerY;
        viewPlane = lastPlane;
        redrawMap();
    }

    /**
     * Set the displayed plane (0-3).
     * Also unlocks from player tracking.
     */
    public void setViewPlane(int plane) {
        if (plane >= 0 && plane <= 3) {
            viewPlane = plane;
            lockedToPlayer = false;
            redrawMap();
        }
    }

    /**
     * Set the collision map accessor for retrieving tile flags.
     */
    public void setCollisionAccessor(CollisionMapAccessor accessor) {
        this.collisionAccessor = accessor;
    }

    /**
     * Set the handler for left-click events on the map.
     */
    public void setLeftClickHandler(MapClickHandler handler) {
        this.leftClickHandler = handler;
    }

    /**
     * Set the provider for right-click context menus.
     */
    public void setContextMenuProvider(MapContextMenuProvider provider) {
        this.contextMenuProvider = provider;
    }

    /**
     * Set the provider for info text displayed on the map.
     */
    public void setInfoProvider(MapInfoProvider provider) {
        this.infoProvider = provider;
    }

    /**
     * Set the destination marker position.
     * Pass -1 for any coordinate to clear the marker.
     */
    public void setDestination(int x, int y, int plane) {
        this.destX = x;
        this.destY = y;
        this.destPlane = plane;
    }

    /**
     * Clear the destination marker.
     */
    public void clearDestination() {
        this.destX = -1;
        this.destY = -1;
        this.destPlane = -1;
    }

    /**
     * Convert screen coordinates to world coordinates.
     * @param screenX Screen X coordinate (from mouse event)
     * @param screenY Screen Y coordinate (from mouse event)
     * @return int[3] = {worldX, worldY, plane} or null if click is outside map
     */
    private int[] screenToWorld(int screenX, int screenY) {
        if (mapImage == null || tileSize <= 0) return null;

        // Account for centering offset
        int offsetX = (getWidth() - imageWidth) / 2;
        int offsetY = (getHeight() - imageHeight) / 2;

        // Convert to image-relative coordinates
        int imgX = screenX - offsetX;
        int imgY = screenY - offsetY;

        // Check bounds
        if (imgX < 0 || imgX >= imageWidth || imgY < 0 || imgY >= imageHeight) {
            return null;
        }

        // Convert to tile coordinates
        int tileX = imgX / tileSize;
        int tileY = (tilesY - 1) - (imgY / tileSize);  // Invert Y

        // Convert to world coordinates using view center
        int halfTilesX = tilesX / 2;
        int halfTilesY = tilesY / 2;

        int worldX = viewCenterX - halfTilesX + tileX;
        int worldY = viewCenterY - halfTilesY + tileY;

        return new int[] { worldX, worldY, viewPlane };
    }

    /**
     * Recreate the backing image when panel is resized or zoom changes.
     */
    private void recreateImage() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Use user-controlled zoom tile size
        tileSize = zoomTileSize;

        tilesX = w / tileSize;
        tilesY = h / tileSize;

        // Make sure we have odd tile counts so player is centered
        if (tilesX % 2 == 0) tilesX--;
        if (tilesY % 2 == 0) tilesY--;

        // Ensure minimum tile counts
        tilesX = Math.max(3, tilesX);
        tilesY = Math.max(3, tilesY);

        imageWidth = tilesX * tileSize;
        imageHeight = tilesY * tileSize;

        mapImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        pixels = ((DataBufferInt) mapImage.getRaster().getDataBuffer()).getData();
    }

    /**
     * Update the map display with the player's current position.
     * Only updates view center if locked to player.
     */
    public void updateMap(int playerX, int playerY, int plane) {
        // Always save player position
        this.lastPlayerX = playerX;
        this.lastPlayerY = playerY;
        this.lastPlane = plane;

        // Clear destination if player reached it
        if (playerX == destX && playerY == destY && plane == destPlane) {
            clearDestination();
        }

        // Only update view center if locked to player
        if (lockedToPlayer) {
            viewCenterX = playerX;
            viewCenterY = playerY;
            viewPlane = plane;
        }

        // Update info text from provider
        if (infoProvider != null) {
            setInfoLines(infoProvider.getInfoLines(playerX, playerY, plane));
        }

        redrawMap();
    }

    /**
     * Redraw the map at current view center.
     */
    private void redrawMap() {
        if (mapImage == null || collisionAccessor == null) {
            recreateImage();
            if (mapImage == null) return;
        }

        // Clear to background
        Arrays.fill(pixels, COLOR_BACKGROUND);

        int halfTilesX = tilesX / 2;
        int halfTilesY = tilesY / 2;

        // Render tiles
        for (int dx = 0; dx < tilesX; dx++) {
            for (int dy = 0; dy < tilesY; dy++) {
                int worldX = viewCenterX - halfTilesX + dx;
                int worldY = viewCenterY - halfTilesY + dy;

                // Screen coordinates (Y inverted - north is up)
                int screenX = dx * tileSize;
                int screenY = (tilesY - 1 - dy) * tileSize;

                // Check if this is the player tile (only if on same plane as view)
                boolean isPlayerTile = (worldX == lastPlayerX && worldY == lastPlayerY && viewPlane == lastPlane);
                if (isPlayerTile) {
                    fillRect(screenX, screenY, tileSize, COLOR_PLAYER);
                    continue;
                }

                // Check if this is the destination tile (only if on same plane as view)
                boolean isDestTile = (destX >= 0 && worldX == destX && worldY == destY && viewPlane == destPlane);
                if (isDestTile) {
                    fillRect(screenX, screenY, tileSize, COLOR_DESTINATION);
                    continue;
                }

                // Get collision flags for VIEW plane (not player plane)
                byte flags = collisionAccessor.getFlags(worldX, worldY, viewPlane);

                if (flags == FLAG_NONE) {
                    // Fully blocked - red fill
                    fillRect(screenX, screenY, tileSize, COLOR_BLOCKED);
                } else if (flags != FLAG_ALL) {
                    // Partial blocking - draw walls on blocked edges
                    int wallThickness = Math.max(1, tileSize / 4);

                    // North wall (top of screen tile)
                    if ((flags & FLAG_NORTH) == 0) {
                        fillRect(screenX, screenY, tileSize, wallThickness, COLOR_WALL);
                    }
                    // South wall (bottom of screen tile)
                    if ((flags & FLAG_SOUTH) == 0) {
                        fillRect(screenX, screenY + tileSize - wallThickness, tileSize, wallThickness, COLOR_WALL);
                    }
                    // East wall (right of screen tile)
                    if ((flags & FLAG_EAST) == 0) {
                        fillRect(screenX + tileSize - wallThickness, screenY, wallThickness, tileSize, COLOR_WALL);
                    }
                    // West wall (left of screen tile)
                    if ((flags & FLAG_WEST) == 0) {
                        fillRect(screenX, screenY, wallThickness, tileSize, COLOR_WALL);
                    }
                }
            }
        }

        repaint();
    }

    /**
     * Fill a rectangle in the pixel array.
     */
    private void fillRect(int x, int y, int size, int color) {
        fillRect(x, y, size, size, color);
    }

    /**
     * Fill a rectangle in the pixel array with separate width/height.
     */
    private void fillRect(int x, int y, int width, int height, int color) {
        int x1 = Math.max(0, x);
        int y1 = Math.max(0, y);
        int x2 = Math.min(imageWidth, x + width);
        int y2 = Math.min(imageHeight, y + height);

        if (x1 >= x2 || y1 >= y2) return;

        int fillWidth = x2 - x1;
        for (int py = y1; py < y2; py++) {
            int rowStart = py * imageWidth + x1;
            Arrays.fill(pixels, rowStart, rowStart + fillWidth, color);
        }
    }

    /**
     * Set a single info line (convenience method for simple use).
     */
    public void setInfoText(String text) {
        synchronized (infoLines) {
            infoLines.clear();
            if (text != null && !text.isEmpty()) {
                infoLines.add(text);
            }
        }
    }

    /**
     * Set multiple info lines for the overlay.
     */
    public void setInfoLines(java.util.List<String> lines) {
        synchronized (infoLines) {
            infoLines.clear();
            if (lines != null) {
                infoLines.addAll(lines);
            }
        }
    }

    /**
     * Add an info line to the overlay.
     */
    public void addInfoLine(String line) {
        synchronized (infoLines) {
            infoLines.add(line);
        }
    }

    /**
     * Clear all info lines.
     */
    public void clearInfoLines() {
        synchronized (infoLines) {
            infoLines.clear();
        }
    }

    /**
     * Clear the map image to prevent ghost rendering after deactivation.
     */
    public void clearMap() {
        if (pixels != null) {
            Arrays.fill(pixels, 0);  // Clear to transparent
        }
        mapImage = null;  // Release the image
        pixels = null;
        lockedToPlayer = true;  // Reset lock state
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw the map image centered in the panel
        if (mapImage != null) {
            int offsetX = (getWidth() - imageWidth) / 2;
            int offsetY = (getHeight() - imageHeight) / 2;
            g2d.drawImage(mapImage, offsetX, offsetY, null);
        }

        // Draw info overlay in top-left
        synchronized (infoLines) {
            if (!infoLines.isEmpty()) {
                g2d.setFont(INFO_FONT);
                FontMetrics fm = g2d.getFontMetrics();

                int lineHeight = fm.getHeight();
                int padding = 8;
                int maxWidth = 0;

                for (String line : infoLines) {
                    maxWidth = Math.max(maxWidth, fm.stringWidth(line));
                }

                int boxWidth = maxWidth + padding * 2;
                int boxHeight = lineHeight * infoLines.size() + padding * 2;

                // Draw background
                g2d.setColor(INFO_BG);
                g2d.fillRoundRect(8, 8, boxWidth, boxHeight, 6, 6);

                // Draw text
                g2d.setColor(INFO_TEXT);
                int textY = 8 + padding + fm.getAscent();
                for (String line : infoLines) {
                    g2d.drawString(line, 8 + padding, textY);
                    textY += lineHeight;
                }
            }
        }

        // Draw control buttons in top-right
        drawControlButtons(g2d);
    }

    /**
     * Draw the control buttons (lock to player, plane selectors) in the top-right corner.
     */
    private void drawControlButtons(Graphics2D g2d) {
        int rightEdge = getWidth() - BUTTON_PADDING;
        int y = BUTTON_PADDING;

        g2d.setFont(BUTTON_FONT);
        FontMetrics fm = g2d.getFontMetrics();

        // Lock to player button
        int lockX = rightEdge - BUTTON_SIZE;
        lockButtonBounds = new Rectangle(lockX, y, BUTTON_SIZE, BUTTON_SIZE);

        g2d.setColor(lockedToPlayer ? BUTTON_ACTIVE : BUTTON_INACTIVE);
        g2d.fillRoundRect(lockX, y, BUTTON_SIZE, BUTTON_SIZE, 4, 4);
        g2d.setColor(Color.WHITE);
        // Draw crosshair symbol (centered)
        String lockSymbol = "\u2316";  // Position indicator symbol
        int symbolWidth = fm.stringWidth(lockSymbol);
        g2d.drawString(lockSymbol, lockX + (BUTTON_SIZE - symbolWidth) / 2, y + BUTTON_SIZE - 7);

        // Plane buttons (0, 1, 2, 3)
        y += BUTTON_SIZE + BUTTON_MARGIN;
        for (int i = 0; i < 4; i++) {
            int btnX = rightEdge - BUTTON_SIZE;
            int btnY = y + i * (BUTTON_SIZE + BUTTON_MARGIN);
            planeButtonBounds[i] = new Rectangle(btnX, btnY, BUTTON_SIZE, BUTTON_SIZE);

            boolean isSelected = (viewPlane == i);
            g2d.setColor(isSelected ? BUTTON_SELECTED : BUTTON_INACTIVE);
            g2d.fillRoundRect(btnX, btnY, BUTTON_SIZE, BUTTON_SIZE, 4, 4);
            g2d.setColor(Color.WHITE);
            String planeStr = String.valueOf(i);
            int textWidth = fm.stringWidth(planeStr);
            g2d.drawString(planeStr, btnX + (BUTTON_SIZE - textWidth) / 2, btnY + BUTTON_SIZE - 7);
        }
    }
}
