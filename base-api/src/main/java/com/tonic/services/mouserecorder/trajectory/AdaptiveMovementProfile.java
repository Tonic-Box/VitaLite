package com.tonic.services.mouserecorder.trajectory;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class AdaptiveMovementProfile
{
    private final Map<DistanceRange, MovementCharacteristics> profile;
    private final TrajectoryGeneratorConfig fallbackConfig;
    @Getter
    private final long lastAnalysisTime;
    @Getter
    private final int trajectoriesAnalyzed;

    public AdaptiveMovementProfile(TrajectoryGeneratorConfig config)
    {
        this.profile = new HashMap<>();
        this.fallbackConfig = config;
        this.lastAnalysisTime = System.currentTimeMillis();
        this.trajectoriesAnalyzed = 0;
    }

    private AdaptiveMovementProfile(Map<DistanceRange, MovementCharacteristics> profile,
                                    TrajectoryGeneratorConfig config,
                                    int count)
    {
        this.profile = new HashMap<>(profile);
        this.fallbackConfig = config;
        this.lastAnalysisTime = System.currentTimeMillis();
        this.trajectoriesAnalyzed = count;
    }

    public MovementCharacteristics getCharacteristics(double distance)
    {
        for (Map.Entry<DistanceRange, MovementCharacteristics> entry : profile.entrySet())
        {
            if (entry.getKey().contains(distance))
            {
                return entry.getValue();
            }
        }
        return new MovementCharacteristics(
            fallbackConfig.getMaxSamplesPerMovement(),
            fallbackConfig.getMovementDurationMs()
        );
    }

    public boolean hasData()
    {
        return !profile.isEmpty() && trajectoriesAnalyzed > 0;
    }

    public static class Builder
    {
        private final Map<DistanceRange, MovementCharacteristics> profile = new HashMap<>();
        private final TrajectoryGeneratorConfig config;
        private int count = 0;

        public Builder(TrajectoryGeneratorConfig config)
        {
            this.config = config;
        }

        public void addRange(DistanceRange range, MovementCharacteristics chars)
        {
            profile.put(range, chars);
        }

        public void setCount(int count)
        {
            this.count = count;
        }

        public AdaptiveMovementProfile build()
        {
            return new AdaptiveMovementProfile(profile, config, count);
        }
    }

    public static class DistanceRange
    {
        @Getter
        private final double min;
        @Getter
        private final double max;

        public DistanceRange(double min, double max)
        {
            this.min = min;
            this.max = max;
        }

        public boolean contains(double distance)
        {
            return distance >= min && distance < max;
        }

        @Override
        public String toString()
        {
            return String.format("%.0f-%.0fpx", min, max);
        }

        @Override
        public int hashCode()
        {
            return Double.hashCode(min) * 31 + Double.hashCode(max);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof DistanceRange)) return false;
            DistanceRange other = (DistanceRange) obj;
            return this.min == other.min && this.max == other.max;
        }
    }

    @Getter
    public static class MovementCharacteristics
    {
        private final int avgSamples;
        private final int avgDurationMs;

        public MovementCharacteristics(int avgSamples, int avgDurationMs)
        {
            this.avgSamples = avgSamples;
            this.avgDurationMs = avgDurationMs;
        }

        @Override
        public String toString()
        {
            return String.format("%d samples, %dms", avgSamples, avgDurationMs);
        }
    }
}
