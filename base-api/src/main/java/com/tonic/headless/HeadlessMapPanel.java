package com.tonic.headless;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * A JPanel that renders a simple collision map centered on the player.
 * Uses direct pixel manipulation for performance.
 */
public class HeadlessMapPanel extends JPanel {

    // Size matching ClientPanel (765x503) - required for RuneLite's custom Layout manager
    private static final Dimension GAME_FIXED_SIZE = new Dimension(765, 503);

    // Colors (ARGB format for BufferedImage TYPE_INT_ARGB)
    private static final int COLOR_BACKGROUND = 0xFFF0F0F0;  // Light gray (passable)
    private static final int COLOR_BLOCKED = 0xFFCC0000;     // Red (fully blocked)
    private static final int COLOR_WALL = 0xFF000000;        // Black (walls)
    private static final int COLOR_PLAYER = 0xFF00DD00;      // Green (player tile)

    // Info overlay colors
    private static final Color INFO_BG = new Color(30, 30, 35, 200);
    private static final Color INFO_TEXT = new Color(220, 220, 225);
    private static final Font INFO_FONT = new Font("Segoe UI", Font.PLAIN, 12);

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

    // Info overlay - extensible list of info lines
    private final java.util.List<String> infoLines = new java.util.ArrayList<>();

    // Collision map accessor (set via reflection from api module)
    private CollisionMapAccessor collisionAccessor;

    /**
     * Functional interface for collision map access.
     * This allows the api module to provide the collision lookup without direct dependency.
     */
    @FunctionalInterface
    public interface CollisionMapAccessor {
        byte getFlags(int x, int y, int plane);
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
    }

    /**
     * Set the collision map accessor for retrieving tile flags.
     */
    public void setCollisionAccessor(CollisionMapAccessor accessor) {
        this.collisionAccessor = accessor;
    }

    /**
     * Recreate the backing image when panel is resized.
     */
    private void recreateImage() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Calculate tile size to fill the panel nicely
        // Aim for ~4-6 pixels per tile, adjust based on panel size
        int minDimension = Math.min(w, h);
        tileSize = Math.max(2, Math.min(8, minDimension / 100));

        tilesX = w / tileSize;
        tilesY = h / tileSize;

        // Make sure we have odd tile counts so player is centered
        if (tilesX % 2 == 0) tilesX--;
        if (tilesY % 2 == 0) tilesY--;

        imageWidth = tilesX * tileSize;
        imageHeight = tilesY * tileSize;

        mapImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        pixels = ((DataBufferInt) mapImage.getRaster().getDataBuffer()).getData();
    }

    /**
     * Update the map display with the player's current position.
     */
    public void updateMap(int playerX, int playerY, int plane) {
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
                int worldX = playerX - halfTilesX + dx;
                int worldY = playerY - halfTilesY + dy;

                // Screen coordinates (Y inverted - north is up)
                int screenX = dx * tileSize;
                int screenY = (tilesY - 1 - dy) * tileSize;

                // Check if this is the player tile
                if (dx == halfTilesX && dy == halfTilesY) {
                    fillRect(screenX, screenY, tileSize, COLOR_PLAYER);
                    continue;
                }

                // Get collision flags
                byte flags = collisionAccessor.getFlags(worldX, worldY, plane);

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
    }
}
