package com.tonic.services.mouserecorder.trajectory;

import com.tonic.services.mouserecorder.IMouseMovementGenerator;
import com.tonic.services.mouserecorder.MouseDataPoint;
import com.tonic.services.mouserecorder.MouseMovementSequence;
import com.tonic.util.config.ConfigFactory;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Trajectory-based mouse movement generator using DTW and retrieval.
 * Generates movements by retrieving similar recorded trajectories, warping them to fit context,
 * blending multiple candidates, and optionally adding noise for variation.
 */
public class TrajectoryGenerator implements IMouseMovementGenerator
{
    @Getter
    private final TrajectoryDatabase database;
    private final Random random;
    private final TrajectoryGeneratorConfig config;
    private final NoiseGenerator noiseGenerator;
    private AdaptiveMovementProfile adaptiveProfile;
    private long lastProfileUpdate;

    public TrajectoryGenerator(TrajectoryDatabase database, Random random, TrajectoryGeneratorConfig config)
    {
        this.database = database;
        this.random = random;
        this.config = config;
        this.noiseGenerator = new NoiseGenerator(random);
        this.lastProfileUpdate = 0;
        updateAdaptiveProfile();
    }

    public TrajectoryGenerator(TrajectoryDatabase database)
    {
        this(database, new Random(), ConfigFactory.create(TrajectoryGeneratorConfig.class));
    }

    private void updateAdaptiveProfile()
    {
        if (config.shouldUseAdaptiveProfiling())
        {
            long dbUpdateTime = database.getLastUpdateTime();
            if (dbUpdateTime > lastProfileUpdate)
            {
                adaptiveProfile = TrajectoryAnalyzer.analyzeDatabase(database, config);
                lastProfileUpdate = dbUpdateTime;
            }
        }
        else
        {
            adaptiveProfile = new AdaptiveMovementProfile(config);
        }
    }

    @Override
    public MouseMovementSequence generate(int startX, int startY, int endX, int endY)
    {
        return generate(startX, startY, endX, endY, System.currentTimeMillis());
    }

    @Override
    public MouseMovementSequence generate(int startX, int startY, int endX, int endY, long startTimeMs)
    {
        updateAdaptiveProfile();

        int dx = endX - startX;
        int dy = endY - startY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        int maxInstantJumpDist = config.getMaxDistanceForInstantJump();
        double instantJumpChance = config.getInstantJumpChance();

        if (distance < config.getMinDistanceForTrajectory())
        {
            return generateInstantJump(startX, startY, endX, endY, startTimeMs);
        }

        if (distance <= maxInstantJumpDist && random.nextDouble() < instantJumpChance)
        {
            return generateInstantJump(startX, startY, endX, endY, startTimeMs);
        }

        if (database.getTrajectoryCount() == 0)
        {
            return generateNaturalFallback(startX, startY, endX, endY, startTimeMs, distance);
        }

        TrajectoryMetadata query = TrajectoryMetadata.create(startX, startY, endX, endY);
        List<Trajectory> similar = database.findSimilar(query, config.getRetrievalCount());

        if (similar.isEmpty())
        {
            return generateNaturalFallback(startX, startY, endX, endY, startTimeMs, distance);
        }

        List<List<MouseDataPoint>> warpedTrajectories = new ArrayList<>();
        for (Trajectory traj : similar)
        {
            List<MouseDataPoint> warped = DynamicTimeWarping.warp(
                traj.getPoints(), startX, startY, endX, endY, startTimeMs);
            warpedTrajectories.add(warped);
        }

        double[] weights = generateBlendWeights(warpedTrajectories.size());
        List<MouseDataPoint> blended = DynamicTimeWarping.blend(warpedTrajectories, weights);

        List<MouseDataPoint> finalPath = applyNoise(blended);

        finalPath = downsampleTrajectory(finalPath, startTimeMs, distance);

        return new MouseMovementSequence(finalPath);
    }

    private List<MouseDataPoint> applyNoise(List<MouseDataPoint> points)
    {
        String noiseType = config.getNoiseType();

        if ("WHITE".equalsIgnoreCase(noiseType))
        {
            return noiseGenerator.applyWhiteNoise(points, config.getWhiteNoiseSigma());
        }
        else if ("CORRELATED".equalsIgnoreCase(noiseType))
        {
            return noiseGenerator.applyCorrelatedNoise(points, config.getCorrelatedNoiseSigma(),
                config.getCorrelatedNoiseCorrelation());
        }
        else
        {
            return points;
        }
    }

    private double[] generateBlendWeights(int count)
    {
        double[] weights = new double[count];
        double randomness = config.getBlendRandomness();

        if (count == 1)
        {
            weights[0] = 1.0;
            return weights;
        }

        for (int i = 0; i < count; i++)
        {
            weights[i] = (1.0 - randomness) / count + random.nextDouble() * randomness;
        }

        double sum = 0;
        for (double w : weights) sum += w;
        for (int i = 0; i < count; i++) weights[i] /= sum;

        return weights;
    }

    /**
     * Generates an instant jump (1-2 samples) mimicking natural fast mouse movements.
     * Real players often move quickly and the client only captures 1-2 positions.
     */
    private MouseMovementSequence generateInstantJump(int startX, int startY, int endX, int endY, long startTimeMs)
    {
        List<MouseDataPoint> points = new ArrayList<>();

        // 70% chance of single sample (pure jump), 30% chance of 2 samples
        if (random.nextDouble() < 0.7)
        {
            // Single destination sample (will encode as XLarge absolute position)
            points.add(new MouseDataPoint(endX, endY, startTimeMs + 50));
        }
        else
        {
            // Two samples: start and end
            points.add(new MouseDataPoint(startX, startY, startTimeMs));
            points.add(new MouseDataPoint(endX, endY, startTimeMs + 50));
        }

        return new MouseMovementSequence(points);
    }

    private List<MouseDataPoint> downsampleTrajectory(List<MouseDataPoint> points, long startTimeMs, double distance)
    {
        if (points.isEmpty())
        {
            return points;
        }

        AdaptiveMovementProfile.MovementCharacteristics chars = adaptiveProfile.getCharacteristics(distance);

        int maxSamples = chars.getAvgSamples();
        int targetDuration = chars.getAvgDurationMs();

        if (points.size() <= maxSamples)
        {
            return retimeTrajectory(points, startTimeMs, targetDuration);
        }

        List<MouseDataPoint> downsampled = new ArrayList<>();
        downsampled.add(points.get(0));

        double step = (points.size() - 1) / (double) (maxSamples - 1);
        for (int i = 1; i < maxSamples - 1; i++)
        {
            int index = (int) Math.round(i * step);
            downsampled.add(points.get(index));
        }

        downsampled.add(points.get(points.size() - 1));

        return retimeTrajectory(downsampled, startTimeMs, targetDuration);
    }

    /**
     * Retimes trajectory to match target duration (faster = more natural).
     */
    private List<MouseDataPoint> retimeTrajectory(List<MouseDataPoint> points, long startTimeMs, int targetDuration)
    {
        if (points.isEmpty())
        {
            return points;
        }

        List<MouseDataPoint> retimed = new ArrayList<>();
        int pointCount = points.size();

        for (int i = 0; i < pointCount; i++)
        {
            MouseDataPoint point = points.get(i);
            double progress = i / (double) (pointCount - 1);
            long newTime = startTimeMs + (long) (progress * targetDuration);
            retimed.add(new MouseDataPoint(point.getX(), point.getY(), newTime));
        }

        return retimed;
    }

    private MouseMovementSequence generateNaturalFallback(int startX, int startY, int endX, int endY, long startTimeMs, double distance)
    {
        AdaptiveMovementProfile.MovementCharacteristics chars = adaptiveProfile.getCharacteristics(distance);

        List<MouseDataPoint> points = new ArrayList<>();
        int dx = endX - startX;
        int dy = endY - startY;

        int steps = Math.max(2, chars.getAvgSamples() - 1 + random.nextInt(3) - 1);

        for (int i = 0; i <= steps; i++)
        {
            double t = i / (double) steps;
            int x = (int) (startX + dx * t);
            int y = (int) (startY + dy * t);
            points.add(new MouseDataPoint(x, y, 0));
        }

        points = retimeTrajectory(points, startTimeMs, chars.getAvgDurationMs());
        return new MouseMovementSequence(points);
    }

    @Override
    public String getName()
    {
        return "Trajectory";
    }

    @Override
    public String getDescription()
    {
        String adaptive = adaptiveProfile != null && adaptiveProfile.hasData() ? " [Adaptive]" : "";
        return String.format("Trajectory generator (recorded=%d)%s", database.getTrajectoryCount(), adaptive);
    }

    public void setSeed(long seed)
    {
        random.setSeed(seed);
    }
}
