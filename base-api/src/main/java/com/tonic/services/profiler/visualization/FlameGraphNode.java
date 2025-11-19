package com.tonic.services.profiler.visualization;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Node in a flame graph representing a method call
 */
public class FlameGraphNode {
    private final String methodName;
    private final String className;
    private final String packageName;
    private long samples;
    private final List<FlameGraphNode> children;
    private FlameGraphNode parent;
    private Rectangle bounds;

    public FlameGraphNode(String methodName, String className, String packageName) {
        this.methodName = methodName;
        this.className = className;
        this.packageName = packageName;
        this.samples = 0;
        this.children = new ArrayList<>();
        this.bounds = new Rectangle();
    }

    public void addSample() {
        this.samples++;
    }

    public void addSamples(long count) {
        this.samples += count;
    }

    public FlameGraphNode getOrCreateChild(String methodName, String className, String packageName) {
        for (FlameGraphNode child : children) {
            if (child.methodName.equals(methodName) &&
                child.className.equals(className) &&
                child.packageName.equals(packageName)) {
                return child;
            }
        }

        FlameGraphNode child = new FlameGraphNode(methodName, className, packageName);
        child.parent = this;
        children.add(child);
        return child;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    public long getSamples() {
        return samples;
    }

    public List<FlameGraphNode> getChildren() {
        return children;
    }

    public FlameGraphNode getParent() {
        return parent;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    public long getTotalSamples() {
        long total = samples;
        for (FlameGraphNode child : children) {
            total += child.getTotalSamples();
        }
        return total;
    }

    public String getFullName() {
        if (packageName != null && !packageName.isEmpty()) {
            return packageName + "." + className + "." + methodName;
        } else if (className != null && !className.isEmpty()) {
            return className + "." + methodName;
        } else {
            return methodName;
        }
    }

    public String getDisplayName() {
        return className + "." + methodName;
    }
}
