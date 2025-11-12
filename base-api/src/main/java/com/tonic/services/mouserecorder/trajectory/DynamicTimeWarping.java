package com.tonic.services.mouserecorder.trajectory;

import com.tonic.services.mouserecorder.MouseDataPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Time Warping algorithm for trajectory alignment and warping.
 * Warps a source trajectory to match target start and end points while preserving path shape.
 */
public class DynamicTimeWarping
{
    public static List<MouseDataPoint> warp(List<MouseDataPoint> source, int targetStartX, int targetStartY,
                                            int targetEndX, int targetEndY, long startTime)
    {
        if (source.size() < 2)
        {
            List<MouseDataPoint> result = new ArrayList<>();
            result.add(new MouseDataPoint(targetStartX, targetStartY, startTime));
            result.add(new MouseDataPoint(targetEndX, targetEndY, startTime + 100));
            return result;
        }

        MouseDataPoint sourceStart = source.get(0);
        MouseDataPoint sourceEnd = source.get(source.size() - 1);

        int sourceDx = sourceEnd.getX() - sourceStart.getX();
        int sourceDy = sourceEnd.getY() - sourceStart.getY();
        int targetDx = targetEndX - targetStartX;
        int targetDy = targetEndY - targetStartY;

        double sourceDistance = Math.sqrt(sourceDx * sourceDx + sourceDy * sourceDy);
        double targetDistance = Math.sqrt(targetDx * targetDx + targetDy * targetDy);

        double scale = sourceDistance > 0 ? targetDistance / sourceDistance : 1.0;

        double sourceAngle = Math.atan2(sourceDy, sourceDx);
        double targetAngle = Math.atan2(targetDy, targetDx);
        double rotation = targetAngle - sourceAngle;

        long sourceDuration = sourceEnd.getTimestampMillis() - sourceStart.getTimestampMillis();
        double timeScale = sourceDuration > 0 ? 1.0 : 1.0;

        List<MouseDataPoint> warped = new ArrayList<>();

        for (MouseDataPoint point : source)
        {
            int relX = point.getX() - sourceStart.getX();
            int relY = point.getY() - sourceStart.getY();

            double rotatedX = relX * Math.cos(rotation) - relY * Math.sin(rotation);
            double rotatedY = relX * Math.sin(rotation) + relY * Math.cos(rotation);

            int scaledX = (int) Math.round(rotatedX * scale);
            int scaledY = (int) Math.round(rotatedY * scale);

            int newX = targetStartX + scaledX;
            int newY = targetStartY + scaledY;

            long relTime = point.getTimestampMillis() - sourceStart.getTimestampMillis();
            long newTime = startTime + (long) (relTime * timeScale);

            warped.add(new MouseDataPoint(newX, newY, newTime));
        }

        if (!warped.isEmpty())
        {
            MouseDataPoint last = warped.get(warped.size() - 1);
            warped.set(warped.size() - 1, new MouseDataPoint(targetEndX, targetEndY, last.getTimestampMillis()));
        }

        return warped;
    }

    public static List<MouseDataPoint> blend(List<List<MouseDataPoint>> trajectories, double[] weights)
    {
        if (trajectories.isEmpty()) return new ArrayList<>();
        if (trajectories.size() == 1) return new ArrayList<>(trajectories.get(0));

        int maxLength = trajectories.stream().mapToInt(List::size).max().orElse(0);
        List<List<MouseDataPoint>> resampled = new ArrayList<>();

        for (List<MouseDataPoint> traj : trajectories)
        {
            resampled.add(resample(traj, maxLength));
        }

        List<MouseDataPoint> blended = new ArrayList<>();

        for (int i = 0; i < maxLength; i++)
        {
            double x = 0;
            double y = 0;
            long time = 0;

            for (int j = 0; j < resampled.size(); j++)
            {
                MouseDataPoint point = resampled.get(j).get(i);
                x += point.getX() * weights[j];
                y += point.getY() * weights[j];
                time += point.getTimestampMillis() * weights[j];
            }

            blended.add(new MouseDataPoint((int) Math.round(x), (int) Math.round(y), (long) time));
        }

        return blended;
    }

    private static List<MouseDataPoint> resample(List<MouseDataPoint> points, int targetCount)
    {
        if (points.size() == targetCount) return new ArrayList<>(points);

        List<MouseDataPoint> resampled = new ArrayList<>();
        double step = (points.size() - 1.0) / (targetCount - 1.0);

        for (int i = 0; i < targetCount; i++)
        {
            double index = i * step;
            int lowerIndex = (int) Math.floor(index);
            int upperIndex = Math.min(lowerIndex + 1, points.size() - 1);
            double fraction = index - lowerIndex;

            MouseDataPoint p1 = points.get(lowerIndex);
            MouseDataPoint p2 = points.get(upperIndex);

            int x = (int) Math.round(p1.getX() * (1 - fraction) + p2.getX() * fraction);
            int y = (int) Math.round(p1.getY() * (1 - fraction) + p2.getY() * fraction);
            long time = (long) (p1.getTimestampMillis() * (1 - fraction) + p2.getTimestampMillis() * fraction);

            resampled.add(new MouseDataPoint(x, y, time));
        }

        return resampled;
    }
}
