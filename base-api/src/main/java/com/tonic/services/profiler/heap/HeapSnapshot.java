package com.tonic.services.profiler.heap;

import com.tonic.services.profiler.DiagnosticCommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class histogram of the live heap: instances and bytes per class, from the GC.class_histogram diagnostic command
 */
public final class HeapSnapshot {
    private static final Pattern ROW = Pattern.compile("^\\s*\\d+:\\s+(\\d+)\\s+(\\d+)\\s+(\\S+)(?:\\s+\\((.*)\\))?\\s*$");
    private static final Pattern TOTAL = Pattern.compile("^\\s*Total\\s+(\\d+)\\s+(\\d+)\\s*$");

    public final long time;
    public final List<ClassCount> classes;
    public final long totalInstances;
    public final long totalBytes;

    /**
     * The live instances of one class
     */
    public static final class ClassCount {
        public final String name;
        public final String module;
        public final long instances;
        public final long bytes;

        /**
         * Creates the count
         * @param name the class's JVM name, such as java.lang.String or [B
         * @param module the module it belongs to, or an empty string when unnamed
         * @param instances its live instance count
         * @param bytes the shallow bytes of those instances
         */
        public ClassCount(String name, String module, long instances, long bytes) {
            this.name = name;
            this.module = module;
            this.instances = instances;
            this.bytes = bytes;
        }

        /**
         * Gets the class's name as source code writes it, so [B becomes byte[]
         * @return the readable name
         */
        public String displayName() {
            return readable(name);
        }
    }

    private HeapSnapshot(long time, List<ClassCount> classes, long totalInstances, long totalBytes) {
        this.time = time;
        this.classes = Collections.unmodifiableList(classes);
        this.totalInstances = totalInstances;
        this.totalBytes = totalBytes;
    }

    /**
     * Takes a snapshot of the live heap, which runs a full garbage collection first
     * @return the snapshot
     * @throws IllegalStateException if the histogram command fails
     */
    public static HeapSnapshot capture() {
        long time = System.currentTimeMillis();
        return parse(DiagnosticCommands.run("gcClassHistogram"), time);
    }

    /**
     * Parses the output of GC.class_histogram
     * @param output the command's output
     * @param time when it was taken, in epoch milliseconds
     * @return the snapshot, with totals summed from the rows when the output has no total line
     */
    public static HeapSnapshot parse(String output, long time) {
        List<ClassCount> classes = new ArrayList<>();
        long instances = 0;
        long bytes = 0;
        long totalInstances = -1;
        long totalBytes = -1;
        for (String line : output.split("\\R")) {
            Matcher row = ROW.matcher(line);
            if (row.matches()) {
                ClassCount count = new ClassCount(row.group(3), row.group(4) == null ? "" : row.group(4), Long.parseLong(row.group(1)), Long.parseLong(row.group(2)));
                classes.add(count);
                instances += count.instances;
                bytes += count.bytes;
                continue;
            }
            Matcher total = TOTAL.matcher(line);
            if (total.matches()) {
                totalInstances = Long.parseLong(total.group(1));
                totalBytes = Long.parseLong(total.group(2));
            }
        }
        return new HeapSnapshot(time, classes, totalInstances >= 0 ? totalInstances : instances, totalBytes >= 0 ? totalBytes : bytes);
    }

    /**
     * Turns a JVM class name into the form source code uses
     * @param name a name such as java.lang.String, [B or [[Ljava.lang.Object;
     * @return the readable name, such as java.lang.String, byte[] or java.lang.Object[][]
     */
    public static String readable(String name) {
        int dimensions = 0;
        while (dimensions < name.length() && name.charAt(dimensions) == '[') {
            dimensions++;
        }
        if (dimensions == 0) {
            return name;
        }
        String element = name.substring(dimensions);
        String base;
        switch (element) {
            case "Z": base = "boolean"; break;
            case "B": base = "byte"; break;
            case "C": base = "char"; break;
            case "S": base = "short"; break;
            case "I": base = "int"; break;
            case "J": base = "long"; break;
            case "F": base = "float"; break;
            case "D": base = "double"; break;
            default: base = element.startsWith("L") && element.endsWith(";") ? element.substring(1, element.length() - 1) : element;
        }
        return base + "[]".repeat(dimensions);
    }
}
