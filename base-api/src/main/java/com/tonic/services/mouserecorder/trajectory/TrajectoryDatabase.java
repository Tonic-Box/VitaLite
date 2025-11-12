package com.tonic.services.mouserecorder.trajectory;

import com.tonic.services.mouserecorder.MouseDataPoint;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Thread-safe storage for recorded trajectories with similarity-based retrieval.
 * Maintains training data and provides context-aware trajectory matching.
 */
public class TrajectoryDatabase
{
    private final List<Trajectory> trajectories;
    @Getter
    private volatile long lastUpdateTime;

    public TrajectoryDatabase()
    {
        this.trajectories = new CopyOnWriteArrayList<>();
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void addTrajectory(List<MouseDataPoint> points)
    {
        if (points.size() < 2) return;

        Trajectory trajectory = new Trajectory(points);
        trajectories.add(trajectory);
        lastUpdateTime = System.currentTimeMillis();
    }

    public List<Trajectory> findSimilar(TrajectoryMetadata query, int count)
    {
        if (trajectories.isEmpty()) return new ArrayList<>();

        return trajectories.stream()
            .map(t -> new ScoredTrajectory(t, t.getSimilarityScore(query)))
            .filter(st -> st.score > 0.3)
            .sorted(Comparator.comparingDouble(st -> -st.score))
            .limit(count)
            .map(st -> st.trajectory)
            .collect(Collectors.toList());
    }

    public int getTrajectoryCount()
    {
        return trajectories.size();
    }

    public List<Trajectory> getAllTrajectories()
    {
        return new ArrayList<>(trajectories);
    }

    public void clear()
    {
        trajectories.clear();
        lastUpdateTime = System.currentTimeMillis();
    }

    public TrajectoryStatistics getStatistics()
    {
        return new TrajectoryStatistics(this);
    }

    private static class ScoredTrajectory
    {
        final Trajectory trajectory;
        final double score;

        ScoredTrajectory(Trajectory trajectory, double score)
        {
            this.trajectory = trajectory;
            this.score = score;
        }
    }

    public static class TrajectoryStatistics
    {
        @Getter
        private final int totalCount;
        @Getter
        private final double avgDistance;
        @Getter
        private final double avgDuration;
        @Getter
        private final double avgPointsPerTrajectory;

        TrajectoryStatistics(TrajectoryDatabase db)
        {
            this.totalCount = db.trajectories.size();

            if (totalCount == 0)
            {
                this.avgDistance = 0;
                this.avgDuration = 0;
                this.avgPointsPerTrajectory = 0;
            }
            else
            {
                double sumDistance = 0;
                double sumDuration = 0;
                double sumPoints = 0;

                for (Trajectory t : db.trajectories)
                {
                    sumDistance += t.getMetadata().getDistance();
                    sumDuration += t.getMetadata().getDuration();
                    sumPoints += t.size();
                }

                this.avgDistance = sumDistance / totalCount;
                this.avgDuration = sumDuration / totalCount;
                this.avgPointsPerTrajectory = sumPoints / totalCount;
            }
        }
    }
}
