package com.tonic.services.mouserecorder.trajectory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrajectoryAnalyzer
{
    private static final double[] DISTANCE_BUCKETS = {0, 50, 100, 150, 200, 300, 500, Double.MAX_VALUE};

    public static AdaptiveMovementProfile analyzeDatabase(TrajectoryDatabase database,
                                                          TrajectoryGeneratorConfig config)
    {
        List<Trajectory> trajectories = database.getAllTrajectories();

        if (trajectories.isEmpty() || trajectories.size() < 20)
        {
            return new AdaptiveMovementProfile(config);
        }

        Map<Integer, BucketStats> buckets = new HashMap<>();
        for (int i = 0; i < DISTANCE_BUCKETS.length - 1; i++)
        {
            buckets.put(i, new BucketStats());
        }

        for (Trajectory traj : trajectories)
        {
            double distance = traj.getMetadata().getDistance();
            int sampleCount = traj.size();
            long duration = traj.getMetadata().getDuration();

            if (sampleCount < 1 || duration < 10) continue;

            int bucketIndex = findBucketIndex(distance);
            if (bucketIndex >= 0)
            {
                buckets.get(bucketIndex).addSample(sampleCount, (int) duration);
            }
        }

        AdaptiveMovementProfile.Builder builder = new AdaptiveMovementProfile.Builder(config);
        builder.setCount(trajectories.size());

        for (int i = 0; i < DISTANCE_BUCKETS.length - 1; i++)
        {
            BucketStats stats = buckets.get(i);
            if (stats.count >= 3)
            {
                AdaptiveMovementProfile.DistanceRange range =
                    new AdaptiveMovementProfile.DistanceRange(DISTANCE_BUCKETS[i], DISTANCE_BUCKETS[i + 1]);

                int avgSamples = (int) Math.round(stats.avgSamples());
                int avgDuration = (int) Math.round(stats.avgDuration());

                avgSamples = Math.max(1, Math.min(avgSamples, 10));
                avgDuration = Math.max(50, Math.min(avgDuration, 800));

                AdaptiveMovementProfile.MovementCharacteristics chars =
                    new AdaptiveMovementProfile.MovementCharacteristics(avgSamples, avgDuration);

                builder.addRange(range, chars);
            }
        }

        return builder.build();
    }

    private static int findBucketIndex(double distance)
    {
        for (int i = 0; i < DISTANCE_BUCKETS.length - 1; i++)
        {
            if (distance >= DISTANCE_BUCKETS[i] && distance < DISTANCE_BUCKETS[i + 1])
            {
                return i;
            }
        }
        return -1;
    }

    private static class BucketStats
    {
        int count = 0;
        long sumSamples = 0;
        long sumDuration = 0;

        void addSample(int samples, int duration)
        {
            count++;
            sumSamples += samples;
            sumDuration += duration;
        }

        double avgSamples()
        {
            return count > 0 ? (double) sumSamples / count : 0;
        }

        double avgDuration()
        {
            return count > 0 ? (double) sumDuration / count : 0;
        }
    }
}
