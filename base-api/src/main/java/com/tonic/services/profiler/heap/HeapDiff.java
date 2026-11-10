package com.tonic.services.profiler.heap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-class change in live instances and bytes between two heap snapshots
 */
public final class HeapDiff {
    private HeapDiff() {
    }

    /**
     * One class's change
     */
    public static final class Row {
        public final String name;
        public final String module;
        public final long instancesBefore;
        public final long instancesAfter;
        public final long bytesBefore;
        public final long bytesAfter;

        private Row(String name, String module, long instancesBefore, long instancesAfter, long bytesBefore, long bytesAfter) {
            this.name = name;
            this.module = module;
            this.instancesBefore = instancesBefore;
            this.instancesAfter = instancesAfter;
            this.bytesBefore = bytesBefore;
            this.bytesAfter = bytesAfter;
        }

        /**
         * Gets the change in instances
         * @return instances after minus instances before
         */
        public long instanceDelta() {
            return instancesAfter - instancesBefore;
        }

        /**
         * Gets the change in bytes
         * @return bytes after minus bytes before
         */
        public long byteDelta() {
            return bytesAfter - bytesBefore;
        }
    }

    /**
     * Compares two snapshots class by class, summing classes of the same name and module loaded by different loaders
     * @param before the earlier snapshot, or null to compare against an empty heap
     * @param after the later snapshot
     * @return one row per class in either snapshot, largest byte growth first
     */
    public static List<Row> between(HeapSnapshot before, HeapSnapshot after) {
        Map<String, long[]> earlier = before == null ? new LinkedHashMap<>() : totals(before);
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : totals(after).entrySet()) {
            long[] now = entry.getValue();
            long[] old = earlier.remove(entry.getKey());
            rows.add(row(entry.getKey(), old == null ? 0 : old[0], now[0], old == null ? 0 : old[1], now[1]));
        }
        for (Map.Entry<String, long[]> gone : earlier.entrySet()) {
            rows.add(row(gone.getKey(), gone.getValue()[0], 0, gone.getValue()[1], 0));
        }
        rows.sort(Comparator.comparingLong(Row::byteDelta).reversed().thenComparing(row -> row.name));
        return rows;
    }

    private static Map<String, long[]> totals(HeapSnapshot snapshot) {
        Map<String, long[]> totals = new LinkedHashMap<>();
        for (HeapSnapshot.ClassCount count : snapshot.classes) {
            long[] sum = totals.computeIfAbsent(count.name + '\u0000' + count.module, key -> new long[2]);
            sum[0] += count.instances;
            sum[1] += count.bytes;
        }
        return totals;
    }

    private static Row row(String key, long instancesBefore, long instancesAfter, long bytesBefore, long bytesAfter) {
        int split = key.indexOf('\u0000');
        return new Row(key.substring(0, split), key.substring(split + 1), instancesBefore, instancesAfter, bytesBefore, bytesAfter);
    }
}
