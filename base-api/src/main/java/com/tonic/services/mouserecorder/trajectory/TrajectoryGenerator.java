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
    private long lastMovementTimestamp;

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

        RapidModeAdjustments adjustments = calculateRapidModeAdjustments(startTimeMs, distance);

        long timeDelta = lastMovementTimestamp == 0 ? -1 : (startTimeMs - lastMovementTimestamp);
        System.out.println(String.format("RAPID MODE: distance=%.1f, timeDelta=%dms, adjustedMaxSamples=%d, adjustedJumpChance=%.2f",
            distance, timeDelta, adjustments.adjustedMaxSamples, adjustments.adjustedInstantJumpChance));

        lastMovementTimestamp = startTimeMs;

        int maxInstantJumpDist = config.getMaxDistanceForInstantJump();
        double instantJumpChance = adjustments.adjustedInstantJumpChance;

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
            System.out.println("FALLBACK: Empty database - no trajectories available");
            return generateNaturalFallback(startX, startY, endX, endY, startTimeMs, distance);
        }

        AdaptiveMovementProfile.MovementCharacteristics chars = adaptiveProfile.getCharacteristics(distance);
        int expectedDuration = adjustments.adjustedMaxSamples > 0
            ? (int)(chars.getAvgDurationMs() * config.getRapidModeSampleReduction())
            : chars.getAvgDurationMs();

        TrajectoryMetadata query = TrajectoryMetadata.create(startX, startY, endX, endY, expectedDuration);
        List<Trajectory> similarForAnalysis = database.findSimilar(query, config.getVariationAnalysisCount());

        if (similarForAnalysis.isEmpty())
        {
            System.out.println(String.format("FALLBACK: No similar trajectories found for distance=%.1f, from (%d,%d) to (%d,%d)",
                distance, startX, startY, endX, endY));
            return generateNaturalFallback(startX, startY, endX, endY, startTimeMs, distance);
        }

        List<Trajectory> similarForBlending = similarForAnalysis.size() > config.getRetrievalCount()
            ? similarForAnalysis.subList(0, config.getRetrievalCount())
            : similarForAnalysis;

        List<List<MouseDataPoint>> warpedTrajectories = new ArrayList<>();
        for (Trajectory traj : similarForBlending)
        {
            List<MouseDataPoint> warped = DynamicTimeWarping.warp(
                traj.getPoints(), startX, startY, endX, endY, startTimeMs);
            warpedTrajectories.add(warped);
        }

        double[] weights = generateBlendWeights(warpedTrajectories.size());
        List<MouseDataPoint> blended = DynamicTimeWarping.blend(warpedTrajectories, weights);

        List<MouseDataPoint> finalPath = applyNoise(blended);

        int targetSampleCount = calculateTargetSampleCount(similarForAnalysis, distance, adjustments);
        finalPath = downsampleTrajectory(finalPath, startTimeMs, distance, targetSampleCount);

        System.out.println(String.format("  → Final samples=%d", finalPath.size()));

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

        System.out.println(String.format("  → Instant jump samples=%d", points.size()));

        return new MouseMovementSequence(points);
    }

    private int calculateTargetSampleCount(List<Trajectory> retrievedTrajectories, double distance, RapidModeAdjustments adjustments)
    {
        AdaptiveMovementProfile.MovementCharacteristics chars = adaptiveProfile.getCharacteristics(distance);
        int configuredMax = config.getMaxSamplesPerMovement();

        int targetBase = chars.getAvgSamples();

        if (!retrievedTrajectories.isEmpty())
        {
            List<Trajectory> lowComplexity = new ArrayList<>();

            for (Trajectory traj : retrievedTrajectories)
            {
                double curvature = traj.getMetadata().getCurvature();
                int sampleCount = traj.getPoints().size();
                double sampleDensity = sampleCount / Math.max(1.0, traj.getMetadata().getDistance());

                if (curvature < 0.3 && sampleDensity < 0.05)
                {
                    lowComplexity.add(traj);
                }
            }

            if (!lowComplexity.isEmpty())
            {
                int sum = 0;
                for (Trajectory traj : lowComplexity)
                {
                    sum += traj.getPoints().size();
                }
                int observedAvg = sum / lowComplexity.size();
                targetBase = Math.min(configuredMax, observedAvg);
            }
        }

        int variation = random.nextInt(3) - 1;
        int baseSampleCount = Math.max(2, Math.min(configuredMax, targetBase + variation));

        if (adjustments.adjustedMaxSamples > 0)
        {
            double reductionFactor = config.getRapidModeSampleReduction();
            int reduced = Math.max(2, (int)(baseSampleCount * reductionFactor));
            int rapidVariation = random.nextInt(3) - 1;
            return Math.max(2, reduced + rapidVariation);
        }

        return baseSampleCount;
    }

    private List<MouseDataPoint> downsampleTrajectory(List<MouseDataPoint> points, long startTimeMs, double distance, int targetSampleCount)
    {
        if (points.isEmpty())
        {
            return points;
        }

        AdaptiveMovementProfile.MovementCharacteristics chars = adaptiveProfile.getCharacteristics(distance);
        int targetDuration = chars.getAvgDurationMs();

        if (points.size() <= targetSampleCount)
        {
            return retimeTrajectory(points, startTimeMs, targetDuration);
        }

        List<MouseDataPoint> downsampled = downsampleSpatially(points, targetSampleCount);

        return retimeTrajectory(downsampled, startTimeMs, targetDuration);
    }

    private List<MouseDataPoint> downsampleSpatially(List<MouseDataPoint> points, int maxSamples)
    {
        if (points.size() <= maxSamples)
        {
            return points;
        }

        double[] arcLengths = new double[points.size()];
        arcLengths[0] = 0.0;

        for (int i = 1; i < points.size(); i++)
        {
            MouseDataPoint p1 = points.get(i - 1);
            MouseDataPoint p2 = points.get(i);
            int dx = p2.getX() - p1.getX();
            int dy = p2.getY() - p1.getY();
            double segmentLength = Math.sqrt(dx * dx + dy * dy);
            arcLengths[i] = arcLengths[i - 1] + segmentLength;
        }

        double totalLength = arcLengths[arcLengths.length - 1];

        if (totalLength == 0)
        {
            List<MouseDataPoint> result = new ArrayList<>();
            result.add(points.get(0));
            double step = (points.size() - 1) / (double) (maxSamples - 1);
            for (int i = 1; i < maxSamples - 1; i++)
            {
                int index = (int) Math.round(i * step);
                result.add(points.get(index));
            }
            result.add(points.get(points.size() - 1));
            return result;
        }

        List<MouseDataPoint> downsampled = new ArrayList<>();
        downsampled.add(points.get(0));

        for (int i = 1; i < maxSamples - 1; i++)
        {
            double targetArcLength = (i / (double) (maxSamples - 1)) * totalLength;

            int closestIndex = 1;
            double minDiff = Double.MAX_VALUE;
            for (int j = 1; j < points.size() - 1; j++)
            {
                double diff = Math.abs(arcLengths[j] - targetArcLength);
                if (diff < minDiff)
                {
                    minDiff = diff;
                    closestIndex = j;
                }
            }

            downsampled.add(points.get(closestIndex));
        }

        downsampled.add(points.get(points.size() - 1));

        return downsampled;
    }

    /**
     * Retimes trajectory to match target duration while preserving natural acceleration patterns.
     * Scales the original relative timing from recorded data instead of using linear interpolation.
     */
    private List<MouseDataPoint> retimeTrajectory(List<MouseDataPoint> points, long startTimeMs, int targetDuration)
    {
        if (points.isEmpty())
        {
            return points;
        }

        if (points.size() == 1)
        {
            return List.of(new MouseDataPoint(points.get(0).getX(), points.get(0).getY(), startTimeMs));
        }

        List<MouseDataPoint> retimed = new ArrayList<>();

        long originalStartTime = points.get(0).getTimestampMillis();
        long originalEndTime = points.get(points.size() - 1).getTimestampMillis();
        long originalDuration = originalEndTime - originalStartTime;

        if (originalDuration == 0)
        {
            for (int i = 0; i < points.size(); i++)
            {
                MouseDataPoint point = points.get(i);
                double progress = i / (double) (points.size() - 1);
                long newTime = startTimeMs + (long) (progress * targetDuration);
                retimed.add(new MouseDataPoint(point.getX(), point.getY(), newTime));
            }
            return retimed;
        }

        for (MouseDataPoint point : points)
        {
            long originalTime = point.getTimestampMillis();
            double relativeProgress = (originalTime - originalStartTime) / (double) originalDuration;
            long newTime = startTimeMs + (long) (relativeProgress * targetDuration);
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

        System.out.println(String.format("  → Fallback samples=%d", points.size()));

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

    private RapidModeAdjustments calculateRapidModeAdjustments(long currentTimeMs, double distance)
    {
        RapidModeAdjustments adjustments = new RapidModeAdjustments();

        // Instant jump chance is NEVER modified by rapid mode (only distance-based)
        adjustments.adjustedInstantJumpChance = config.getInstantJumpChance();

        if (!config.isRapidModeEnabled() || lastMovementTimestamp == 0)
        {
            adjustments.adjustedMaxSamples = -1;
            return adjustments;
        }

        long timeSinceLastMovement = currentTimeMs - lastMovementTimestamp;
        boolean rapidMode = timeSinceLastMovement <= config.getRapidModeThresholdMs();

        if (!rapidMode)
        {
            double randomChance = config.getRapidModeRandomChance();
            if (randomChance > 0 && random.nextDouble() < randomChance)
            {
                rapidMode = true;
            }
            else
            {
                adjustments.adjustedMaxSamples = -1;
                return adjustments;
            }
        }

        // Rapid mode: ONLY adjust sample counts for faster-looking curved paths
        int shortThreshold = config.getRapidModeShortDistanceThreshold();
        int mediumThreshold = config.getRapidModeMediumDistanceThreshold();
        double sampleReduction = config.getRapidModeSampleReduction();

        int baseMaxSamples = config.getMaxSamplesPerMovement();

        if (distance < shortThreshold)
        {
            adjustments.adjustedMaxSamples = 1;
        }
        else if (distance < mediumThreshold)
        {
            adjustments.adjustedMaxSamples = Math.max(2, (int)(baseMaxSamples * sampleReduction * 0.7));
        }
        else
        {
            adjustments.adjustedMaxSamples = Math.max(2, (int)(baseMaxSamples * sampleReduction));
        }

        return adjustments;
    }

    private static class RapidModeAdjustments
    {
        int adjustedMaxSamples;
        double adjustedInstantJumpChance;
    }
}
