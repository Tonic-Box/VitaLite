package com.tonic.services.profiler.leak;

/**
 * Represents a suspected memory leak for a specific class
 */
public class LeakSuspicion implements Comparable<LeakSuspicion> {
    private final String className;
    private final long currentInstances;
    private final long baselineInstances;
    private final long currentBytes;
    private final long baselineBytes;
    private final double growthRate; // instances per minute
    private final int samplesCollected;
    private final LeakConfidence confidence;
    private final LeakType type;
    private final long firstSeenTimestamp;
    private final long lastSeenTimestamp;

    public LeakSuspicion(String className, long currentInstances, long baselineInstances,
                        long currentBytes, long baselineBytes, double growthRate,
                        int samplesCollected, long firstSeenTimestamp, long lastSeenTimestamp) {
        this.className = className;
        this.currentInstances = currentInstances;
        this.baselineInstances = baselineInstances;
        this.currentBytes = currentBytes;
        this.baselineBytes = baselineBytes;
        this.growthRate = growthRate;
        this.samplesCollected = samplesCollected;
        this.firstSeenTimestamp = firstSeenTimestamp;
        this.lastSeenTimestamp = lastSeenTimestamp;
        this.confidence = calculateConfidence();
        this.type = determineLeakType();
    }

    private LeakConfidence calculateConfidence() {
        // Calculate confidence based on multiple factors
        int score = 0;

        // Factor 1: Consistent growth (more samples = higher confidence)
        if (samplesCollected >= 10) score += 30;
        else if (samplesCollected >= 5) score += 20;
        else if (samplesCollected >= 3) score += 10;

        // Factor 2: Growth rate (higher rate = higher confidence)
        if (growthRate > 1000) score += 30;
        else if (growthRate > 100) score += 20;
        else if (growthRate > 10) score += 10;

        // Factor 3: Relative growth (percentage increase)
        double relativeGrowth = baselineInstances > 0
            ? ((double)(currentInstances - baselineInstances) / baselineInstances)
            : 0;
        if (relativeGrowth > 2.0) score += 25; // 200%+ growth
        else if (relativeGrowth > 1.0) score += 15; // 100%+ growth
        else if (relativeGrowth > 0.5) score += 10; // 50%+ growth

        // Factor 4: Absolute growth
        long absoluteGrowth = currentInstances - baselineInstances;
        if (absoluteGrowth > 10000) score += 15;
        else if (absoluteGrowth > 1000) score += 10;
        else if (absoluteGrowth > 100) score += 5;

        // Convert score to confidence level
        if (score >= 80) return LeakConfidence.CRITICAL;
        if (score >= 60) return LeakConfidence.HIGH;
        if (score >= 40) return LeakConfidence.MEDIUM;
        if (score >= 20) return LeakConfidence.LOW;
        return LeakConfidence.UNKNOWN;
    }

    private LeakType determineLeakType() {
        String name = className.toLowerCase();

        if (name.contains("classloader")) {
            return LeakType.CLASSLOADER;
        } else if (name.contains("thread")) {
            return LeakType.THREAD;
        } else if (name.contains("hashmap") || name.contains("map$node") ||
                   name.contains("map$entry")) {
            return LeakType.COLLECTION;
        } else if (name.contains("cache")) {
            return LeakType.CACHE;
        } else if (name.contains("listener") || name.contains("handler")) {
            return LeakType.LISTENER;
        } else if (name.contains("byte[]") || name.contains("char[]")) {
            return LeakType.ARRAY;
        } else if (name.contains("string")) {
            return LeakType.STRING;
        } else {
            return LeakType.OBJECT;
        }
    }

    public String getClassName() {
        return className;
    }

    public long getCurrentInstances() {
        return currentInstances;
    }

    public long getBaselineInstances() {
        return baselineInstances;
    }

    public long getCurrentBytes() {
        return currentBytes;
    }

    public long getBaselineBytes() {
        return baselineBytes;
    }

    public long getInstanceDelta() {
        return currentInstances - baselineInstances;
    }

    public long getBytesDelta() {
        return currentBytes - baselineBytes;
    }

    public double getGrowthRate() {
        return growthRate;
    }

    public int getSamplesCollected() {
        return samplesCollected;
    }

    public LeakConfidence getConfidence() {
        return confidence;
    }

    public LeakType getType() {
        return type;
    }

    public long getFirstSeenTimestamp() {
        return firstSeenTimestamp;
    }

    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    public double getRelativeGrowth() {
        return baselineInstances > 0
            ? ((double)(currentInstances - baselineInstances) / baselineInstances) * 100.0
            : 0;
    }

    @Override
    public int compareTo(LeakSuspicion other) {
        // Sort by confidence first, then growth rate
        int confidenceCompare = other.confidence.compareTo(this.confidence);
        if (confidenceCompare != 0) {
            return confidenceCompare;
        }
        return Double.compare(other.growthRate, this.growthRate);
    }

    public enum LeakConfidence {
        CRITICAL("Critical", 0xD32F2F),  // Red
        HIGH("High", 0xFF5722),          // Deep Orange
        MEDIUM("Medium", 0xFF9800),      // Orange
        LOW("Low", 0xFFC107),            // Amber
        UNKNOWN("Unknown", 0x9E9E9E);    // Gray

        private final String label;
        private final int color;

        LeakConfidence(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public int getColor() {
            return color;
        }
    }

    public enum LeakType {
        CLASSLOADER("ClassLoader", "Plugin/module not unloading properly"),
        THREAD("Thread", "Threads not being cleaned up"),
        COLLECTION("Collection", "Map/List/Set growing unbounded"),
        CACHE("Cache", "Cache not evicting entries"),
        LISTENER("Listener", "Event listeners not being removed"),
        ARRAY("Array", "Large arrays accumulating"),
        STRING("String", "String instances accumulating"),
        OBJECT("Object", "General object accumulation");

        private final String label;
        private final String description;

        LeakType(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }
}
