package com.tonic.services.profiler.visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Interactive flame graph visualization panel for CPU profiling
 */
public class FlameGraphPanel extends JPanel {
    private static final Color BG_COLOR = new Color(30, 31, 34);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);
    private static final Color BORDER_COLOR = new Color(60, 62, 66);
    private static final int FRAME_HEIGHT = 18;
    private static final int MIN_WIDTH_FOR_LABEL = 60;

    private FlameGraphNode root;
    private FlameGraphNode hoveredNode;
    private FlameGraphNode focusedNode;
    private Map<String, Color> packageColors;
    private int colorIndex = 0;

    // Zoom state
    private double zoomScale = 1.0;
    private double zoomOffsetX = 0;
    private static final double ZOOM_FACTOR = 1.2;
    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = 20.0;

    // Panning state (for drag-to-pan when zoomed)
    private boolean isPanning = false;
    private int panStartX;
    private double panStartOffsetX;

    private static final Color[] PALETTE = {
        new Color(229, 115, 115),
        new Color(255, 167, 38),
        new Color(255, 202, 40),
        new Color(102, 187, 106),
        new Color(77, 182, 172),
        new Color(79, 195, 247),
        new Color(100, 181, 246),
        new Color(159, 168, 218),
        new Color(186, 104, 200),
        new Color(244, 143, 177)
    };

    public FlameGraphPanel() {
        setBackground(BG_COLOR);
        packageColors = new HashMap<>();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getPoint());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Middle mouse button starts panning when zoomed
                if (e.getButton() == MouseEvent.BUTTON2 && zoomScale > 1.01) {
                    isPanning = true;
                    panStartX = e.getX();
                    panStartOffsetX = zoomOffsetX;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    isPanning = false;
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleHover(e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isPanning) {
                    handlePan(e.getX());
                }
            }
        });

        // Mouse wheel zoom - zoom centered on mouse position
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                handleMouseWheelZoom(e);
            }
        });
    }

    public void setRoot(FlameGraphNode root) {
        this.root = root;
        this.focusedNode = root;
        this.packageColors.clear();
        this.colorIndex = 0;
        this.zoomScale = 1.0;
        this.zoomOffsetX = 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (root == null || focusedNode == null) {
            drawEmptyState(g);
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Calculate layout
        int depth = calculateDepth(focusedNode);
        int totalHeight = depth * FRAME_HEIGHT;

        // Draw focus info at top (includes zoom level)
        drawFocusInfo(g2d);

        // Apply zoom transform for layout
        int baseWidth = getWidth() - 10;
        int zoomedWidth = (int) (baseWidth * zoomScale);
        int zoomedX = (int) (5 - zoomOffsetX);

        // Layout and draw flame graph with zoom
        Rectangle bounds = new Rectangle(zoomedX, 35, zoomedWidth, totalHeight);
        layoutNode(focusedNode, bounds, 0);
        drawFlameGraph(g2d, focusedNode, 0);

        // Draw tooltip
        if (hoveredNode != null) {
            drawTooltip(g2d, hoveredNode);
        }
    }

    private void drawEmptyState(Graphics g) {
        g.setColor(TEXT_COLOR);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        String msg = "No profiling data available";
        FontMetrics fm = g.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(msg)) / 2;
        int y = getHeight() / 2;
        g.drawString(msg, x, y);
    }

    private void drawFocusInfo(Graphics2D g2d) {
        g2d.setColor(new Color(50, 52, 56));
        g2d.fillRect(0, 0, getWidth(), 30);

        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        String focusInfo;
        if (focusedNode == root) {
            focusInfo = "All Stacks (" + root.getSamples() + " samples) - Click frame to zoom, scroll to magnify";
        } else {
            focusInfo = "Focused: " + focusedNode.getDisplayName() +
                       " (" + focusedNode.getSamples() + " samples) - Click background to reset";
        }

        // Show zoom level if zoomed
        String zoomInfo = "";
        if (zoomScale > 1.01) {
            zoomInfo = String.format(" [%.0f%% zoom]", zoomScale * 100);
        }

        g2d.drawString(focusInfo + zoomInfo, 10, 20);
    }

    private int calculateDepth(FlameGraphNode node) {
        if (node.getChildren().isEmpty()) {
            return 1;
        }

        int maxChildDepth = 0;
        for (FlameGraphNode child : node.getChildren()) {
            maxChildDepth = Math.max(maxChildDepth, calculateDepth(child));
        }

        return maxChildDepth + 1;
    }

    private void layoutNode(FlameGraphNode node, Rectangle bounds, int depth) {
        node.setBounds(new Rectangle(bounds.x, bounds.y + depth * FRAME_HEIGHT, bounds.width, FRAME_HEIGHT));

        if (node.getChildren().isEmpty()) {
            return;
        }

        long totalSamples = node.getSamples();
        int xOffset = bounds.x;

        for (FlameGraphNode child : node.getChildren()) {
            double widthRatio = (double) child.getSamples() / totalSamples;
            int childWidth = (int) (bounds.width * widthRatio);

            if (childWidth > 0) {
                Rectangle childBounds = new Rectangle(xOffset, bounds.y, childWidth, bounds.height);
                layoutNode(child, childBounds, depth + 1);
                xOffset += childWidth;
            }
        }
    }

    private void drawFlameGraph(Graphics2D g2d, FlameGraphNode node, int depth) {
        drawFrame(g2d, node);

        for (FlameGraphNode child : node.getChildren()) {
            drawFlameGraph(g2d, child, depth + 1);
        }
    }

    private void drawFrame(Graphics2D g2d, FlameGraphNode node) {
        Rectangle bounds = node.getBounds();

        if (bounds.width < 2) {
            return;
        }

        // Get color
        Color color = getColorForPackage(node.getPackageName());
        if (node == hoveredNode) {
            color = brighten(color, 0.3f);
        }

        // Fill
        g2d.setColor(color);
        g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        // Border
        g2d.setColor(BORDER_COLOR);
        g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

        // Label
        if (bounds.width >= MIN_WIDTH_FOR_LABEL) {
            drawLabel(g2d, node, bounds);
        }
    }

    private void drawLabel(Graphics2D g2d, FlameGraphNode node, Rectangle bounds) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));

        String label = node.getDisplayName();
        FontMetrics fm = g2d.getFontMetrics();

        // Truncate if needed
        if (fm.stringWidth(label) > bounds.width - 10) {
            label = truncateLabel(label, fm, bounds.width - 10);
        }

        int x = bounds.x + 5;
        int y = bounds.y + fm.getAscent() + 2;

        g2d.drawString(label, x, y);
    }

    private String truncateLabel(String label, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(label) <= maxWidth) {
            return label;
        }

        String ellipsis = "...";
        int ellipsisWidth = fm.stringWidth(ellipsis);

        for (int i = label.length() - 1; i > 0; i--) {
            String truncated = label.substring(0, i) + ellipsis;
            if (fm.stringWidth(truncated) <= maxWidth) {
                return truncated;
            }
        }

        return ellipsis;
    }

    private void drawTooltip(Graphics2D g2d, FlameGraphNode node) {
        long totalSamples = root.getSamples();
        double percentage = (node.getSamples() * 100.0) / totalSamples;

        String text = String.format("%s: %d samples (%.2f%%)",
            node.getFullName(), node.getSamples(), percentage);

        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics fm = g2d.getFontMetrics();

        int padding = 8;
        int width = fm.stringWidth(text) + padding * 2;
        int height = fm.getHeight() + padding;

        Point mouse = getMousePosition();
        if (mouse == null) {
            return;
        }

        int x = mouse.x + 15;
        int y = mouse.y - height - 5;

        // Keep tooltip on screen
        if (x + width > getWidth()) {
            x = mouse.x - width - 5;
        }
        if (y < 0) {
            y = mouse.y + 15;
        }

        g2d.setColor(new Color(50, 52, 56, 230));
        g2d.fillRoundRect(x, y, width, height, 5, 5);

        g2d.setColor(BORDER_COLOR);
        g2d.drawRoundRect(x, y, width, height, 5, 5);

        g2d.setColor(TEXT_COLOR);
        g2d.drawString(text, x + padding, y + fm.getAscent() + padding / 2);
    }

    private void handleClick(Point point) {
        FlameGraphNode clicked = findNodeAt(focusedNode, point);
        if (clicked != null) {
            focusedNode = clicked;
            repaint();
        } else if (point.y < 30 && focusedNode != root) {
            // Click on header to reset
            focusedNode = root;
            repaint();
        }
    }

    private void handleHover(Point point) {
        FlameGraphNode hovered = findNodeAt(focusedNode, point);
        if (hovered != hoveredNode) {
            hoveredNode = hovered;
            repaint();
        }
    }

    private FlameGraphNode findNodeAt(FlameGraphNode node, Point point) {
        if (node.getBounds().contains(point)) {
            // Check children first (they're on top)
            for (FlameGraphNode child : node.getChildren()) {
                FlameGraphNode found = findNodeAt(child, point);
                if (found != null) {
                    return found;
                }
            }
            return node;
        }
        return null;
    }

    private Color getColorForPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return PALETTE[0];
        }

        return packageColors.computeIfAbsent(packageName, k -> {
            Color color = PALETTE[colorIndex % PALETTE.length];
            colorIndex++;
            return color;
        });
    }

    private Color brighten(Color color, float factor) {
        int r = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor));
        int g = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor));
        int b = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(r, g, b);
    }

    /**
     * Handles panning when middle mouse button is dragged
     */
    private void handlePan(int currentX) {
        int deltaX = panStartX - currentX;
        double newOffset = panStartOffsetX + deltaX;

        // Clamp offset
        int baseWidth = getWidth() - 10;
        double maxOffset = baseWidth * zoomScale - baseWidth;
        zoomOffsetX = Math.max(0, Math.min(maxOffset, newOffset));

        repaint();
    }

    /**
     * Handles mouse wheel zoom - zooms in/out centered on mouse position
     */
    private void handleMouseWheelZoom(MouseWheelEvent e) {
        if (root == null || focusedNode == null) {
            return;
        }

        // Get mouse position relative to content area
        int mouseX = e.getX();

        // Determine zoom direction
        int rotation = e.getWheelRotation();
        double oldZoom = zoomScale;
        double newZoom;

        if (rotation < 0) {
            // Scroll up = zoom in
            newZoom = oldZoom * ZOOM_FACTOR;
        } else {
            // Scroll down = zoom out
            newZoom = oldZoom / ZOOM_FACTOR;
        }

        // Clamp zoom
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));

        if (Math.abs(newZoom - oldZoom) < 0.001) {
            return; // No change
        }

        // Calculate new offset to keep mouse position fixed
        // Formula: mouseX = zoomedX + (contentX * zoomScale)
        // We want: mouseX_old_content == mouseX_new_content
        // So: (mouseX + oldOffset) / oldZoom == (mouseX + newOffset) / newZoom
        // newOffset = (mouseX + oldOffset) * newZoom / oldZoom - mouseX

        double contentX = (mouseX + zoomOffsetX - 5) / oldZoom;
        zoomOffsetX = contentX * newZoom - (mouseX - 5);
        zoomScale = newZoom;

        // Clamp offset to prevent panning past content
        int baseWidth = getWidth() - 10;
        double maxOffset = baseWidth * zoomScale - baseWidth;
        zoomOffsetX = Math.max(0, Math.min(maxOffset, zoomOffsetX));

        repaint();
    }

    public void resetZoom() {
        if (root != null) {
            focusedNode = root;
            zoomScale = 1.0;
            zoomOffsetX = 0;
            repaint();
        }
    }
}
