package com.tonic.services.profiler.visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive treemap visualization panel
 */
public class TreemapPanel extends JPanel {
    private static final Color BG_COLOR = new Color(30, 31, 34);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);
    private static final Color BORDER_COLOR = new Color(60, 62, 66);
    private static final int MIN_LABEL_WIDTH = 80;
    private static final int MIN_LABEL_HEIGHT = 20;

    private TreemapNode root;
    private TreemapNode currentView;
    private TreemapNode hoveredNode;
    private List<TreemapNode> breadcrumb;

    public TreemapPanel() {
        setBackground(BG_COLOR);
        breadcrumb = new ArrayList<>();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getPoint());
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleHover(e.getPoint());
            }
        });
    }

    public void setRoot(TreemapNode root) {
        this.root = root;
        this.currentView = root;
        this.breadcrumb.clear();
        if (root != null) {
            breadcrumb.add(root);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (currentView == null) {
            drawEmptyState(g);
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw breadcrumb
        drawBreadcrumb(g2d);

        // Calculate layout bounds (leave space for breadcrumb)
        Rectangle layoutBounds = new Rectangle(5, 35, getWidth() - 10, getHeight() - 40);

        // Layout and draw treemap
        if (currentView.hasChildren()) {
            layoutTreemap(currentView, layoutBounds);
            drawTreemap(g2d, currentView);
        } else {
            drawSingleNode(g2d, currentView, layoutBounds);
        }

        // Draw hover tooltip
        if (hoveredNode != null) {
            drawTooltip(g2d, hoveredNode);
        }
    }

    private void drawEmptyState(Graphics g) {
        g.setColor(TEXT_COLOR);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        String msg = "No data to display";
        FontMetrics fm = g.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(msg)) / 2;
        int y = getHeight() / 2;
        g.drawString(msg, x, y);
    }

    private void drawBreadcrumb(Graphics2D g2d) {
        g2d.setColor(new Color(50, 52, 56));
        g2d.fillRect(0, 0, getWidth(), 30);

        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        StringBuilder path = new StringBuilder();
        for (int i = 0; i < breadcrumb.size(); i++) {
            if (i > 0) {
                path.append(" > ");
            }
            path.append(breadcrumb.get(i).getLabel());
        }

        g2d.drawString(path.toString(), 10, 20);
    }

    private void layoutTreemap(TreemapNode node, Rectangle bounds) {
        if (!node.hasChildren()) {
            node.setBounds(bounds);
            return;
        }

        List<TreemapNode> children = new ArrayList<>(node.getChildren());
        children.sort((a, b) -> Long.compare(b.getSize(), a.getSize()));

        squarify(children, new ArrayList<>(), bounds);
    }

    private void squarify(List<TreemapNode> children, List<TreemapNode> row, Rectangle bounds) {
        if (children.isEmpty()) {
            layoutRow(row, bounds);
            return;
        }

        TreemapNode child = children.get(0);

        if (row.isEmpty() || improveAspectRatio(row, child, bounds)) {
            row.add(child);
            squarify(children.subList(1, children.size()), row, bounds);
        } else {
            Rectangle remainingBounds = layoutRow(row, bounds);
            squarify(children, new ArrayList<>(), remainingBounds);
        }
    }

    private boolean improveAspectRatio(List<TreemapNode> row, TreemapNode child, Rectangle bounds) {
        if (row.isEmpty()) {
            return true;
        }

        double currentWorst = worstAspectRatio(row, bounds);

        List<TreemapNode> withChild = new ArrayList<>(row);
        withChild.add(child);
        double newWorst = worstAspectRatio(withChild, bounds);

        return newWorst <= currentWorst;
    }

    private double worstAspectRatio(List<TreemapNode> row, Rectangle bounds) {
        long total = row.stream().mapToLong(TreemapNode::getSize).sum();
        if (total == 0) {
            return Double.MAX_VALUE;
        }

        double width = bounds.width;
        double height = bounds.height;
        double area = width * height;

        if (area == 0) {
            return Double.MAX_VALUE;
        }

        double shortSide = Math.min(width, height);
        if (shortSide == 0) {
            return Double.MAX_VALUE;
        }

        double worst = 0;
        for (TreemapNode node : row) {
            double nodeArea = (node.getSize() / (double) total) * area;
            double aspectRatio = Math.max(
                (shortSide * shortSide) / nodeArea,
                nodeArea / (shortSide * shortSide)
            );
            worst = Math.max(worst, aspectRatio);
        }

        return worst;
    }

    private Rectangle layoutRow(List<TreemapNode> row, Rectangle bounds) {
        if (row.isEmpty()) {
            return bounds;
        }

        long total = row.stream().mapToLong(TreemapNode::getSize).sum();

        boolean horizontal = bounds.width >= bounds.height;
        double size = horizontal
            ? (total / (double) currentView.getSize()) * bounds.height
            : (total / (double) currentView.getSize()) * bounds.width;

        size = Math.max(1, size);

        int offset = horizontal ? bounds.y : bounds.x;

        for (TreemapNode node : row) {
            double nodeSize = horizontal
                ? (node.getSize() / (double) total) * bounds.width
                : (node.getSize() / (double) total) * bounds.height;

            nodeSize = Math.max(1, nodeSize);

            Rectangle nodeBounds = horizontal
                ? new Rectangle(
                    bounds.x + (int) (offset - bounds.y),
                    bounds.y,
                    (int) nodeSize,
                    (int) size
                )
                : new Rectangle(
                    bounds.x,
                    bounds.y + (int) (offset - bounds.x),
                    (int) size,
                    (int) nodeSize
                );

            node.setBounds(nodeBounds);

            if (node.hasChildren()) {
                Rectangle childBounds = new Rectangle(
                    nodeBounds.x + 2,
                    nodeBounds.y + 2,
                    nodeBounds.width - 4,
                    nodeBounds.height - 4
                );
                layoutTreemap(node, childBounds);
            }

            offset += (int) nodeSize;
        }

        return horizontal
            ? new Rectangle(bounds.x, bounds.y + (int) size, bounds.width, bounds.height - (int) size)
            : new Rectangle(bounds.x + (int) size, bounds.y, bounds.width - (int) size, bounds.height);
    }

    private void drawTreemap(Graphics2D g2d, TreemapNode node) {
        for (TreemapNode child : node.getChildren()) {
            drawNode(g2d, child);
        }
    }

    private void drawNode(Graphics2D g2d, TreemapNode node) {
        Rectangle bounds = node.getBounds();

        if (bounds.width < 2 || bounds.height < 2) {
            return;
        }

        // Fill
        Color fillColor = node.getColor();
        if (node == hoveredNode) {
            fillColor = brighten(fillColor, 0.2f);
        }
        g2d.setColor(fillColor);
        g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        // Border
        g2d.setColor(BORDER_COLOR);
        g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

        // Label
        if (bounds.width >= MIN_LABEL_WIDTH && bounds.height >= MIN_LABEL_HEIGHT) {
            drawLabel(g2d, node, bounds);
        }

        // Recurse for children
        if (node.hasChildren()) {
            for (TreemapNode child : node.getChildren()) {
                drawNode(g2d, child);
            }
        }
    }

    private void drawSingleNode(Graphics2D g2d, TreemapNode node, Rectangle bounds) {
        g2d.setColor(node.getColor());
        g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        g2d.setColor(BORDER_COLOR);
        g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

        drawLabel(g2d, node, bounds);
    }

    private void drawLabel(Graphics2D g2d, TreemapNode node, Rectangle bounds) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));

        String label = node.getLabel();
        FontMetrics fm = g2d.getFontMetrics();

        if (fm.stringWidth(label) > bounds.width - 10) {
            label = truncateLabel(label, fm, bounds.width - 10);
        }

        int x = bounds.x + 5;
        int y = bounds.y + fm.getAscent() + 3;

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

    private void drawTooltip(Graphics2D g2d, TreemapNode node) {
        String text = String.format("%s: %s bytes", node.getLabel(), formatSize(node.getSize()));

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
        TreemapNode clicked = findNodeAt(currentView, point);
        if (clicked != null && clicked.hasChildren()) {
            // Drill down
            currentView = clicked;
            breadcrumb.add(clicked);
            repaint();
        } else if (point.y < 30 && breadcrumb.size() > 1) {
            // Click on breadcrumb - go up
            breadcrumb.remove(breadcrumb.size() - 1);
            currentView = breadcrumb.get(breadcrumb.size() - 1);
            repaint();
        }
    }

    private void handleHover(Point point) {
        TreemapNode hovered = findNodeAt(currentView, point);
        if (hovered != hoveredNode) {
            hoveredNode = hovered;
            repaint();
        }
    }

    private TreemapNode findNodeAt(TreemapNode node, Point point) {
        if (node.getBounds().contains(point)) {
            if (node.hasChildren()) {
                for (TreemapNode child : node.getChildren()) {
                    TreemapNode found = findNodeAt(child, point);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return node;
        }
        return null;
    }

    private Color brighten(Color color, float factor) {
        int r = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor));
        int g = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor));
        int b = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(r, g, b);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    public void zoomOut() {
        if (breadcrumb.size() > 1) {
            breadcrumb.remove(breadcrumb.size() - 1);
            currentView = breadcrumb.get(breadcrumb.size() - 1);
            repaint();
        }
    }

    public void zoomToRoot() {
        if (root != null) {
            currentView = root;
            breadcrumb.clear();
            breadcrumb.add(root);
            repaint();
        }
    }
}
