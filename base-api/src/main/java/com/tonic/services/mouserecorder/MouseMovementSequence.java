package com.tonic.services.mouserecorder;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sequence of mouse movement samples.
 * Immutable collection of MouseDataPoints with metadata.
 */
@Getter
public class MouseMovementSequence
{
    private final List<MouseDataPoint> points;
    private final long startTime;
    private final long endTime;

    public MouseMovementSequence(List<MouseDataPoint> points)
    {
        if (points == null || points.isEmpty())
        {
            throw new IllegalArgumentException("Points cannot be null or empty");
        }

        this.points = List.copyOf(points);
        this.startTime = points.get(0).getTimestampMillis();
        this.endTime = points.get(points.size() - 1).getTimestampMillis();
    }

    /**
     * Builder for creating MouseMovementSequence
     */
    public static class Builder
    {
        private final List<MouseDataPoint> points = new ArrayList<>();

        public Builder add(MouseDataPoint point)
        {
            points.add(point);
            return this;
        }

        public Builder add(int x, int y, long timestampMillis)
        {
            points.add(new MouseDataPoint(x, y, timestampMillis));
            return this;
        }

        public Builder addNow(int x, int y)
        {
            points.add(MouseDataPoint.now(x, y));
            return this;
        }

        public Builder addAll(List<MouseDataPoint> points)
        {
            this.points.addAll(points);
            return this;
        }

        public MouseMovementSequence build()
        {
            return new MouseMovementSequence(points);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Number of samples in this sequence
     */
    public int size()
    {
        return points.size();
    }

    /**
     * Duration of this sequence in milliseconds
     */
    public long getDurationMillis()
    {
        return endTime - startTime;
    }

    /**
     * Returns a clamped version of this sequence with all coordinates within valid ranges
     */
    public MouseMovementSequence clamped()
    {
        List<MouseDataPoint> clamped = new ArrayList<>();
        for (MouseDataPoint point : points)
        {
            clamped.add(point.clamped());
        }
        return new MouseMovementSequence(clamped);
    }

    /**
     * Returns a subsequence from startIndex (inclusive) to endIndex (exclusive)
     */
    public MouseMovementSequence slice(int startIndex, int endIndex)
    {
        if (startIndex < 0 || endIndex > points.size() || startIndex >= endIndex)
        {
            throw new IllegalArgumentException("Invalid slice indices");
        }
        return new MouseMovementSequence(points.subList(startIndex, endIndex));
    }

    @Override
    public String toString()
    {
        return String.format("MouseMovementSequence{size=%d, duration=%dms}", size(), getDurationMillis());
    }
}
