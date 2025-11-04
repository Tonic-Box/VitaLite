package com.tonic.services.pathfinder.implimentations.flowfield;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU cache for flow fields with automatic eviction.
 * Caches frequently used destinations (banks, GE, etc).
 */
public class FlowFieldCache
{
    private static final int MAX_CACHE_SIZE = 100;  // Maximum cached flow fields
    private static final long MAX_AGE_MS = 5 * 60 * 1000;  // 5 minutes

    private final Map<Integer, FlowField> cache;

    public FlowFieldCache() {
        this.cache = new LinkedHashMap<Integer, FlowField>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, FlowField> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
    }

    /**
     * Gets cached flow field for goal position.
     * Returns null if not cached or stale.
     */
    public synchronized FlowField get(int goalPosition) {
        FlowField field = cache.get(goalPosition);
        if (field == null) {
            return null;
        }

        // Check if stale
        long age = System.currentTimeMillis() - field.getTimestamp();
        if (age > MAX_AGE_MS) {
            cache.remove(goalPosition);
            return null;
        }

        return field;
    }

    /**
     * Caches a flow field for goal position.
     */
    public synchronized void put(int goalPosition, FlowField field) {
        cache.put(goalPosition, field);
    }

    /**
     * Checks if position is in cache and valid.
     */
    public synchronized boolean has(int goalPosition) {
        return get(goalPosition) != null;
    }

    /**
     * Clears entire cache.
     */
    public synchronized void clear() {
        cache.clear();
    }

    /**
     * Gets cache statistics.
     */
    public synchronized CacheStats getStats() {
        int totalMemory = 0;
        for (FlowField field : cache.values()) {
            totalMemory += field.estimateMemoryBytes();
        }
        return new CacheStats(cache.size(), totalMemory);
    }

    public static class CacheStats {
        public final int entries;
        public final int memoryBytes;

        public CacheStats(int entries, int memoryBytes) {
            this.entries = entries;
            this.memoryBytes = memoryBytes;
        }
    }
}
