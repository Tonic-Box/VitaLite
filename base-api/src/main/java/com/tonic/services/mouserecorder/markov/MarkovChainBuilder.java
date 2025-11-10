package com.tonic.services.mouserecorder.markov;

import com.tonic.services.mouserecorder.MouseDataPoint;

import java.util.List;

/**
 * Builds and refines Markov chain models from recorded mouse movement data.
 * Analyzes movement patterns and extracts transition probabilities.
 */
public class MarkovChainBuilder
{
    private final int binSize;
    private final int timeBinSize;
    private final boolean ignoreStationary;

    /**
     * Creates a Markov chain builder with specified discretization parameters.
     *
     * @param binSize          Pixels per spatial bin (e.g., 5 = group movements by 5-pixel increments)
     * @param timeBinSize      Milliseconds per time bin
     * @param ignoreStationary If true, ignores samples where mouse didn't move
     */
    public MarkovChainBuilder(int binSize, int timeBinSize, boolean ignoreStationary)
    {
        this.binSize = binSize;
        this.timeBinSize = timeBinSize;
        this.ignoreStationary = ignoreStationary;
    }

    /**
     * Builds a new Markov chain from recorded samples.
     *
     * @param samples List of recorded mouse samples
     * @return MarkovChainData containing learned transition probabilities
     */
    public MarkovChainData build(List<MouseDataPoint> samples)
    {
        MarkovChainData chain = new MarkovChainData(binSize, timeBinSize);
        train(chain, samples);
        return chain;
    }

    /**
     * Trains an existing Markov chain with new samples (incremental learning).
     *
     * @param chain   Existing chain to update
     * @param samples New samples to learn from
     */
    public void train(MarkovChainData chain, List<MouseDataPoint> samples)
    {
        if (samples == null || samples.size() < 2)
        {
            return;
        }

        // Ensure chain has compatible binning
        if (chain.getBinSize() != binSize || chain.getTimeBinSize() != timeBinSize)
        {
            throw new IllegalArgumentException(
                String.format("Chain binning (%d, %d) doesn't match builder (%d, %d)",
                    chain.getBinSize(), chain.getTimeBinSize(), binSize, timeBinSize)
            );
        }

        // Process sequential pairs of samples
        for (int i = 0; i < samples.size() - 1; i++)
        {
            MouseDataPoint current = samples.get(i);
            MouseDataPoint next = samples.get(i + 1);

            // Skip samples with invalid coordinates (off-screen)
            // -1,-1 means mouse went off-screen, these create bad transitions
            if (current.getX() == -1 || current.getY() == -1 ||
                next.getX() == -1 || next.getY() == -1)
            {
                continue;
            }

            // Calculate deltas
            int deltaX = next.getX() - current.getX();
            int deltaY = next.getY() - current.getY();
            int deltaMs = (int) (next.getTimestampMillis() - current.getTimestampMillis());

            // Skip unreasonably large deltas (likely camera movements or drag errors)
            // Normal mouse movements shouldn't exceed ~500px in a single sample
            // This filters out middle-mouse camera drags and other erratic movements
            int deltaDistance = (int) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            if (deltaDistance > 500)
            {
                continue;
            }

            // Skip stationary movements if configured
            if (ignoreStationary && deltaX == 0 && deltaY == 0)
            {
                continue;
            }

            // Skip negative time deltas (shouldn't happen, but be defensive)
            if (deltaMs < 0)
            {
                continue;
            }

            // Quantize into states
            MovementState fromState = MovementState.fromDeltas(0, 0, 0, binSize, timeBinSize);
            MovementState toState = MovementState.fromDeltas(deltaX, deltaY, deltaMs, binSize, timeBinSize);

            // Record transition
            chain.recordTransition(fromState, toState);
            chain.recordSample();
        }
    }

    /**
     * Analyzes sample quality and returns metrics.
     *
     * @param samples Samples to analyze
     * @return Quality metrics
     */
    public SampleQualityMetrics analyzeQuality(List<MouseDataPoint> samples)
    {
        if (samples.isEmpty())
        {
            return new SampleQualityMetrics(0, 0, 0, 0, 0, 0);
        }

        int totalSamples = samples.size();
        int stationarySamples = 0;
        int validTransitions = 0;
        long totalDeltaTime = 0;
        long minDeltaTime = Long.MAX_VALUE;
        long maxDeltaTime = 0;

        for (int i = 0; i < samples.size() - 1; i++)
        {
            MouseDataPoint current = samples.get(i);
            MouseDataPoint next = samples.get(i + 1);

            int deltaX = next.getX() - current.getX();
            int deltaY = next.getY() - current.getY();
            long deltaMs = next.getTimestampMillis() - current.getTimestampMillis();

            if (deltaX == 0 && deltaY == 0)
            {
                stationarySamples++;
            }

            if (deltaMs >= 0)
            {
                validTransitions++;
                totalDeltaTime += deltaMs;
                minDeltaTime = Math.min(minDeltaTime, deltaMs);
                maxDeltaTime = Math.max(maxDeltaTime, deltaMs);
            }
        }

        double avgDeltaTime = validTransitions > 0 ? (double) totalDeltaTime / validTransitions : 0;

        return new SampleQualityMetrics(
            totalSamples,
            validTransitions,
            stationarySamples,
            avgDeltaTime,
            minDeltaTime == Long.MAX_VALUE ? 0 : minDeltaTime,
            maxDeltaTime
        );
    }

    /**
     * Quality metrics for a set of samples.
     */
    public static class SampleQualityMetrics
    {
        public final int totalSamples;
        public final int validTransitions;
        public final int stationarySamples;
        public final double avgDeltaTimeMs;
        public final long minDeltaTimeMs;
        public final long maxDeltaTimeMs;

        private SampleQualityMetrics(int totalSamples, int validTransitions, int stationarySamples,
                                     double avgDeltaTimeMs, long minDeltaTimeMs, long maxDeltaTimeMs)
        {
            this.totalSamples = totalSamples;
            this.validTransitions = validTransitions;
            this.stationarySamples = stationarySamples;
            this.avgDeltaTimeMs = avgDeltaTimeMs;
            this.minDeltaTimeMs = minDeltaTimeMs;
            this.maxDeltaTimeMs = maxDeltaTimeMs;
        }

        public double getStationaryRatio()
        {
            return totalSamples > 0 ? (double) stationarySamples / totalSamples : 0;
        }

        public double getEffectiveSamplingRate()
        {
            return avgDeltaTimeMs > 0 ? 1000.0 / avgDeltaTimeMs : 0;
        }

        @Override
        public String toString()
        {
            return String.format(
                "Quality{samples=%d, valid=%d, stationary=%d (%.1f%%), avg=%.1fms, rate=%.1fHz}",
                totalSamples, validTransitions, stationarySamples,
                getStationaryRatio() * 100, avgDeltaTimeMs, getEffectiveSamplingRate()
            );
        }
    }
}
