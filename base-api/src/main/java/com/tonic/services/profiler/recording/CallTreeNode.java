package com.tonic.services.profiler.recording;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node in a call tree representing a method and its callers/callees.
 * Used for hierarchical method breakdown and flame graph generation.
 */
public class CallTreeNode {
    private final String methodKey;      // ClassName.methodName
    private final String className;
    private final String methodName;
    private final String packageName;

    private int selfSamples;             // Times at top of stack (executing)
    private int totalSamples;            // Times anywhere on stack

    private CallTreeNode parent;
    private final Map<String, CallTreeNode> children;  // methodKey -> child node

    // For caller/callee analysis
    private final Map<String, Integer> callers;   // methodKey -> count
    private final Map<String, Integer> callees;   // methodKey -> count

    public CallTreeNode(String methodKey, String className, String methodName, String packageName) {
        this.methodKey = methodKey;
        this.className = className;
        this.methodName = methodName;
        this.packageName = packageName;
        this.children = new HashMap<>();
        this.callers = new HashMap<>();
        this.callees = new HashMap<>();
    }

    /**
     * Create root node for call tree
     */
    public static CallTreeNode createRoot() {
        return new CallTreeNode("(root)", "", "(all)", "");
    }

    /**
     * Get or create a child node for the given method
     */
    public CallTreeNode getOrCreateChild(String methodKey, String className, String methodName, String packageName) {
        return children.computeIfAbsent(methodKey, k -> {
            CallTreeNode child = new CallTreeNode(methodKey, className, methodName, packageName);
            child.parent = this;
            return child;
        });
    }

    /**
     * Add a sample where this method was at top of stack
     */
    public void addSelfSample() {
        selfSamples++;
    }

    /**
     * Add a sample where this method was on the stack
     */
    public void addTotalSample() {
        totalSamples++;
    }

    /**
     * Record that this method was called by another method
     */
    public void addCaller(String callerKey) {
        callers.merge(callerKey, 1, Integer::sum);
    }

    /**
     * Record that this method called another method
     */
    public void addCallee(String calleeKey) {
        callees.merge(calleeKey, 1, Integer::sum);
    }

    // ==================== Getters ====================

    public String getMethodKey() {
        return methodKey;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getSelfSamples() {
        return selfSamples;
    }

    public int getTotalSamples() {
        return totalSamples;
    }

    public CallTreeNode getParent() {
        return parent;
    }

    public List<CallTreeNode> getChildren() {
        return new ArrayList<>(children.values());
    }

    /**
     * Get children sorted by total samples (highest first)
     */
    public List<CallTreeNode> getChildrenSorted() {
        List<CallTreeNode> sorted = new ArrayList<>(children.values());
        sorted.sort(Comparator.comparingInt(CallTreeNode::getTotalSamples).reversed());
        return sorted;
    }

    public Map<String, Integer> getCallers() {
        return callers;
    }

    public Map<String, Integer> getCallees() {
        return callees;
    }

    /**
     * Get callers sorted by count (highest first)
     */
    public List<Map.Entry<String, Integer>> getCallersSorted() {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(callers.entrySet());
        sorted.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        return sorted;
    }

    /**
     * Get callees sorted by count (highest first)
     */
    public List<Map.Entry<String, Integer>> getCalleesSorted() {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(callees.entrySet());
        sorted.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        return sorted;
    }

    /**
     * Get display name (ShortClassName.methodName)
     */
    public String getDisplayName() {
        if (className == null || className.isEmpty()) {
            return methodName;
        }
        int lastDot = className.lastIndexOf('.');
        String shortClass = lastDot > 0 ? className.substring(lastDot + 1) : className;
        return shortClass + "." + methodName;
    }

    /**
     * Get full name including package
     */
    public String getFullName() {
        return methodKey;
    }

    /**
     * Calculate percentage of total samples
     */
    public double getSelfPercent(int totalRecordingSamples) {
        if (totalRecordingSamples <= 0) return 0;
        return (selfSamples * 100.0) / totalRecordingSamples;
    }

    public double getTotalPercent(int totalRecordingSamples) {
        if (totalRecordingSamples <= 0) return 0;
        return (totalSamples * 100.0) / totalRecordingSamples;
    }

    /**
     * Check if this node has any children
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Get depth of this node in the tree
     */
    public int getDepth() {
        int depth = 0;
        CallTreeNode node = this;
        while (node.parent != null) {
            depth++;
            node = node.parent;
        }
        return depth;
    }

    @Override
    public String toString() {
        return String.format("%s (self=%d, total=%d)", methodKey, selfSamples, totalSamples);
    }
}
