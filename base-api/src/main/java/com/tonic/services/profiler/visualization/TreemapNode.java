package com.tonic.services.profiler.visualization;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Node in a treemap representing memory allocation
 */
public class TreemapNode {
    private final String label;
    private final long size;
    private final Color color;
    private final List<TreemapNode> children;
    private Rectangle bounds;
    private TreemapNode parent;

    public TreemapNode(String label, long size, Color color) {
        this.label = label;
        this.size = size;
        this.color = color;
        this.children = new ArrayList<>();
        this.bounds = new Rectangle();
    }

    public void addChild(TreemapNode child) {
        children.add(child);
        child.parent = this;
    }

    public String getLabel() {
        return label;
    }

    public long getSize() {
        return size;
    }

    public Color getColor() {
        return color;
    }

    public List<TreemapNode> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    public TreemapNode getParent() {
        return parent;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * Get the full path from root to this node
     */
    public String getPath() {
        if (parent == null) {
            return label;
        }
        return parent.getPath() + " > " + label;
    }
}
