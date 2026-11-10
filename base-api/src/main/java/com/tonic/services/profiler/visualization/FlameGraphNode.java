package com.tonic.services.profiler.visualization;

import lombok.Getter;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One frame of a flame graph: a method and the samples whose stacks passed through it at this point
 */
public class FlameGraphNode {
    @Getter
    private final String methodName;
    @Getter
    private final String className;
    @Getter
    private final String packageName;
    @Getter
    private long samples;
    @Getter
    private final List<FlameGraphNode> children = new ArrayList<>();
    private final Map<String, FlameGraphNode> childrenByMethod = new HashMap<>();
    @Getter
    private FlameGraphNode parent;
    @Getter
    private Rectangle bounds = new Rectangle();

    /**
     * Creates the node
     * @param methodName the method's name
     * @param className the fully qualified class name
     * @param packageName the class's package, used to colour the frame
     */
    public FlameGraphNode(String methodName, String className, String packageName) {
        this.methodName = methodName;
        this.className = className;
        this.packageName = packageName;
    }

    /**
     * Counts one more sample through this frame
     */
    public void addSample() {
        samples++;
    }

    /**
     * Gets the child for a method, creating it the first time
     * @param methodName the method's name
     * @param className the fully qualified class name
     * @param packageName the class's package
     * @return the child node
     */
    public FlameGraphNode getOrCreateChild(String methodName, String className, String packageName) {
        return childrenByMethod.computeIfAbsent(className + '.' + methodName, key -> {
            FlameGraphNode child = new FlameGraphNode(methodName, className, packageName);
            child.parent = this;
            children.add(child);
            return child;
        });
    }

    /**
     * Sets where the frame is drawn
     * @param bounds the frame's rectangle
     */
    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    /**
     * Gets the class and method name
     * @return the fully qualified method name
     */
    public String getFullName() {
        return className == null || className.isEmpty() ? methodName : className + '.' + methodName;
    }

    /**
     * Gets the short class name and method name
     * @return the name shown on the frame
     */
    public String getDisplayName() {
        if (className == null || className.isEmpty()) {
            return methodName;
        }
        int lambda = className.indexOf("$$Lambda$");
        String type = lambda >= 0 ? className.substring(0, lambda + "$$Lambda".length()) : className;
        return type.substring(type.lastIndexOf('.') + 1) + '.' + methodName;
    }
}
