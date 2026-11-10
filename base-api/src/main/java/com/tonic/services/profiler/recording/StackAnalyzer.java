package com.tonic.services.profiler.recording;

import com.tonic.services.profiler.visualization.FlameGraphNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Builds every view of a recording from its stack samples: per-method counts, the flame graph, the top-down call
 * tree, the bottom-up callers tree, callers and callees, the methods blocked in native code, and allocations
 */
public final class StackAnalyzer {
    private final Predicate<String> include;
    private final Map<String, MethodStats> stats = new HashMap<>();
    private final Map<String, MethodStats> nativeStats = new HashMap<>();
    private final Map<String, CallTreeNode> methodNodes = new HashMap<>();
    private final FlameGraphNode flameRoot = new FlameGraphNode("(all)", "", "");
    private final CallTreeNode callTree = CallTreeNode.createRoot();
    private final CallTreeNode callersTree = CallTreeNode.createRoot();
    private final Map<String, long[]> allocationsByClass = new HashMap<>();
    private final Map<String, long[]> allocationsBySite = new HashMap<>();
    private int samples;
    private int nativeSamples;
    private int truncatedSamples;
    private long allocatedBytes;

    /**
     * One Java frame of a stack
     */
    public static final class Frame {
        public final String className;
        public final String methodName;

        /**
         * Creates the frame
         * @param className the fully qualified class name
         * @param methodName the method name
         */
        public Frame(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        String key() {
            return className + '.' + methodName;
        }

        String packageName() {
            int lastDot = className.lastIndexOf('.');
            return lastDot > 0 ? className.substring(0, lastDot) : "";
        }
    }

    /**
     * Creates an analyzer
     * @param include which classes the views show; frames of other classes are skipped but still separate the
     *                methods around them, so no caller and callee pair is invented across them
     */
    public StackAnalyzer(Predicate<String> include) {
        this.include = include;
    }

    /**
     * Adds a sample of a thread running Java code
     * @param frames the stack, innermost frame first
     * @param truncated whether the recorder cut the stack short
     */
    public void addCpuSample(List<Frame> frames, boolean truncated) {
        samples++;
        if (truncated) {
            truncatedSamples++;
        }
        List<Frame> shown = new ArrayList<>();
        Set<String> counted = new HashSet<>();
        for (int index = 0; index < frames.size(); index++) {
            Frame frame = frames.get(index);
            if (!include.test(frame.className)) {
                continue;
            }
            shown.add(frame);
            String key = frame.key();
            MethodStats method = stats.computeIfAbsent(key, MethodStats::new);
            CallTreeNode node = methodNodes.computeIfAbsent(key, ignored -> new CallTreeNode(key, frame.className, frame.methodName, frame.packageName()));
            if (index == 0) {
                method.incrementSelfSamples();
                node.addSelfSample();
            }
            if (counted.add(key)) {
                method.incrementTotalSamples();
                node.addTotalSample();
            }
        }

        Set<String> pairs = new HashSet<>();
        for (int index = 0; index + 1 < frames.size(); index++) {
            Frame callee = frames.get(index);
            Frame caller = frames.get(index + 1);
            if (include.test(callee.className) && include.test(caller.className) && pairs.add(caller.key() + '\u0000' + callee.key())) {
                methodNodes.get(callee.key()).addCaller(caller.key());
                methodNodes.get(caller.key()).addCallee(callee.key());
            }
        }

        if (shown.isEmpty()) {
            return;
        }
        boolean topShown = shown.get(0) == frames.get(0);
        FlameGraphNode flame = flameRoot;
        flameRoot.addSample();
        CallTreeNode down = callTree;
        callTree.addTotalSample();
        for (int index = shown.size() - 1; index >= 0; index--) {
            Frame frame = shown.get(index);
            flame = flame.getOrCreateChild(frame.methodName, frame.className, frame.packageName());
            flame.addSample();
            down = down.getOrCreateChild(frame.key(), frame.className, frame.methodName, frame.packageName());
            down.addTotalSample();
            if (index == 0 && topShown) {
                down.addSelfSample();
            }
        }
        CallTreeNode up = callersTree;
        callersTree.addTotalSample();
        for (int index = 0; index < shown.size(); index++) {
            Frame frame = shown.get(index);
            up = up.getOrCreateChild(frame.key(), frame.className, frame.methodName, frame.packageName());
            up.addTotalSample();
            if (index == 0 && topShown) {
                up.addSelfSample();
            }
        }
    }

    /**
     * Adds a sample of a thread inside a native method, usually waiting on I/O, a lock or the OS
     * @param frames the stack, innermost frame first
     */
    public void addNativeSample(List<Frame> frames) {
        nativeSamples++;
        Set<String> counted = new HashSet<>();
        for (int index = 0; index < frames.size(); index++) {
            Frame frame = frames.get(index);
            if (index > 0 && !include.test(frame.className)) {
                continue;
            }
            MethodStats method = nativeStats.computeIfAbsent(frame.key(), MethodStats::new);
            if (index == 0) {
                method.incrementSelfSamples();
            }
            if (counted.add(frame.key())) {
                method.incrementTotalSamples();
            }
        }
    }

    /**
     * Adds an allocation event
     * @param objectClass the class of the object allocated
     * @param bytes the bytes the event stands for
     * @param frames the allocating stack, innermost frame first
     */
    public void addAllocation(String objectClass, long bytes, List<Frame> frames) {
        allocatedBytes += bytes;
        add(allocationsByClass, objectClass, bytes);
        Frame site = null;
        for (Frame frame : frames) {
            if (include.test(frame.className)) {
                site = frame;
                break;
            }
        }
        add(allocationsBySite, site == null ? "(outside the filter)" : site.key(), bytes);
    }

    /**
     * Builds the results
     * @param samplePeriodMs the sampling period the recording used
     * @param startTime when recording started, in epoch milliseconds
     * @param endTime when recording stopped, in epoch milliseconds
     * @param allocationsRecorded whether allocation events were recorded
     * @return the results
     */
    public MethodTimingResults results(int samplePeriodMs, long startTime, long endTime, boolean allocationsRecorded) {
        MethodTimingResults.Allocations allocations = allocationsRecorded ? new MethodTimingResults.Allocations(allocatedBytes, allocationsByClass, allocationsBySite) : null;
        return new MethodTimingResults(stats, samples, truncatedSamples, samplePeriodMs, startTime, endTime, flameRoot, callTree, callersTree, methodNodes, nativeStats, nativeSamples, allocations);
    }

    private static void add(Map<String, long[]> totals, String key, long bytes) {
        long[] total = totals.computeIfAbsent(key, ignored -> new long[2]);
        total[0] += bytes;
        total[1]++;
    }
}
