package com.tonic.services.mouserecorder.trajectory;

import com.tonic.services.mouserecorder.MouseDataPoint;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete mouse movement trajectory from start to end.
 * Stores the full path with timing and context metadata for retrieval and warping.
 */
@Getter
public class Trajectory
{
    private final List<MouseDataPoint> points;
    private final TrajectoryMetadata metadata;
    private final long recordedTime;

    public Trajectory(List<MouseDataPoint> points)
    {
        this.points = new ArrayList<>(points);
        this.metadata = TrajectoryMetadata.fromPoints(points);
        this.recordedTime = System.currentTimeMillis();
    }

    public int size()
    {
        return points.size();
    }

    public MouseDataPoint getStart()
    {
        return points.isEmpty() ? null : points.get(0);
    }

    public MouseDataPoint getEnd()
    {
        return points.isEmpty() ? null : points.get(points.size() - 1);
    }

    public double getSimilarityScore(TrajectoryMetadata query)
    {
        return metadata.computeSimilarity(query);
    }
}
