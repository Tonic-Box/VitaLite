package com.tonic.services.profiler.jit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the output of the Compiler.codecache diagnostic command: each code heap's use and whether compilation is on
 */
public final class CodeCache {
    private static final Pattern SEGMENT = Pattern.compile("(?:CodeHeap '([^']+)'|CodeCache):\\s*size=(\\d+)Kb used=(\\d+)Kb max_used=(\\d+)Kb free=(\\d+)Kb");
    private static final Pattern TOTALS = Pattern.compile("total_blobs=(\\d+) nmethods=(\\d+) adapters=(\\d+)");
    private static final Pattern COMPILATION = Pattern.compile("compilation: (\\S+)");

    public final List<Segment> segments;
    public final int blobs;
    public final int nmethods;
    public final int adapters;
    public final String compilation;

    /**
     * One code heap
     */
    public static final class Segment {
        public final String name;
        public final long sizeKb;
        public final long usedKb;
        public final long maxUsedKb;
        public final long freeKb;

        private Segment(String name, long sizeKb, long usedKb, long maxUsedKb, long freeKb) {
            this.name = name;
            this.sizeKb = sizeKb;
            this.usedKb = usedKb;
            this.maxUsedKb = maxUsedKb;
            this.freeKb = freeKb;
        }
    }

    private CodeCache(List<Segment> segments, int blobs, int nmethods, int adapters, String compilation) {
        this.segments = segments;
        this.blobs = blobs;
        this.nmethods = nmethods;
        this.adapters = adapters;
        this.compilation = compilation;
    }

    /**
     * Parses the command's output
     * @param output the command's output
     * @return the code heaps and totals, with -1 counts and an unknown state for what the output lacks
     */
    public static CodeCache parse(String output) {
        List<Segment> segments = new ArrayList<>();
        Matcher segment = SEGMENT.matcher(output);
        while (segment.find()) {
            String name = segment.group(1) == null ? "CodeCache" : segment.group(1);
            segments.add(new Segment(name, Long.parseLong(segment.group(2)), Long.parseLong(segment.group(3)), Long.parseLong(segment.group(4)), Long.parseLong(segment.group(5))));
        }
        Matcher totals = TOTALS.matcher(output);
        boolean found = totals.find();
        Matcher compilation = COMPILATION.matcher(output);
        return new CodeCache(segments, found ? Integer.parseInt(totals.group(1)) : -1, found ? Integer.parseInt(totals.group(2)) : -1, found ? Integer.parseInt(totals.group(3)) : -1, compilation.find() ? compilation.group(1) : "unknown");
    }
}
