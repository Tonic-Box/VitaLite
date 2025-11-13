package com.tonic.services.mouserecorder.trajectory;

import com.tonic.services.mouserecorder.MouseDataPoint;
import lombok.Getter;

import java.util.List;

/**
 * Metadata describing trajectory context for similarity matching.
 * Includes distance, direction, duration, and curvature properties.
 */
@Getter
public class TrajectoryMetadata
{
    private final double distance;
    private final double angle;
    private final long duration;
    private final double avgVelocity;
    private final double curvature;
    private final int startX;
    private final int startY;
    private final int endX;
    private final int endY;

    private TrajectoryMetadata(double distance, double angle, long duration,
                              double avgVelocity, double curvature,
                              int startX, int startY, int endX, int endY)
    {
        this.distance = distance;
        this.angle = angle;
        this.duration = duration;
        this.avgVelocity = avgVelocity;
        this.curvature = curvature;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public static TrajectoryMetadata fromPoints(List<MouseDataPoint> points)
    {
        if (points.size() < 2)
        {
            return new TrajectoryMetadata(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        MouseDataPoint start = points.get(0);
        MouseDataPoint end = points.get(points.size() - 1);

        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.atan2(dy, dx);
        long duration = end.getTimestampMillis() - start.getTimestampMillis();
        double avgVelocity = duration > 0 ? distance / duration : 0;

        double curvature = computeCurvature(points);

        return new TrajectoryMetadata(distance, angle, duration, avgVelocity, curvature,
            start.getX(), start.getY(), end.getX(), end.getY());
    }

    public static TrajectoryMetadata create(int startX, int startY, int endX, int endY)
    {
        return create(startX, startY, endX, endY, 0);
    }

    public static TrajectoryMetadata create(int startX, int startY, int endX, int endY, long expectedDuration)
    {
        int dx = endX - startX;
        int dy = endY - startY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.atan2(dy, dx);
        double avgVelocity = expectedDuration > 0 ? distance / expectedDuration : 0;

        return new TrajectoryMetadata(distance, angle, expectedDuration, avgVelocity, 0, startX, startY, endX, endY);
    }

    public double computeSimilarity(TrajectoryMetadata other)
    {
        double distanceRatio = Math.min(this.distance, other.distance) / Math.max(this.distance, other.distance);
        if (distanceRatio < 0.5) return 0.0;

        double angleDiff = Math.abs(normalizeAngle(this.angle - other.angle));
        double angleScore = 1.0 - (angleDiff / Math.PI);

        double curvatureDiff = Math.abs(this.curvature - other.curvature);
        double curvatureScore = Math.max(0.0, 1.0 - curvatureDiff);

        double velocityScore = 0.5;
        if (this.avgVelocity > 0 && other.avgVelocity > 0)
        {
            double velocityRatio = Math.min(this.avgVelocity, other.avgVelocity) / Math.max(this.avgVelocity, other.avgVelocity);
            velocityScore = velocityRatio;
        }

        return (distanceRatio * 0.4) + (angleScore * 0.35) + (velocityScore * 0.15) + (curvatureScore * 0.1);
    }

    private static double computeCurvature(List<MouseDataPoint> points)
    {
        if (points.size() < 3) return 0.0;

        double totalCurvature = 0.0;
        int samples = 0;

        for (int i = 1; i < points.size() - 1; i++)
        {
            MouseDataPoint p0 = points.get(i - 1);
            MouseDataPoint p1 = points.get(i);
            MouseDataPoint p2 = points.get(i + 1);

            double angle1 = Math.atan2(p1.getY() - p0.getY(), p1.getX() - p0.getX());
            double angle2 = Math.atan2(p2.getY() - p1.getY(), p2.getX() - p1.getX());

            double curvature = Math.abs(normalizeAngle(angle2 - angle1));
            totalCurvature += curvature;
            samples++;
        }

        return samples > 0 ? totalCurvature / samples : 0.0;
    }

    private static double normalizeAngle(double angle)
    {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
}
