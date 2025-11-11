package com.tonic.services.mouserecorder.markov;

import lombok.Getter;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores Markov chain transition probabilities for mouse movement generation.
 * Thread-safe for concurrent training and generation.
 */
public class MarkovChainData
{
    @Getter
    private final int binSize;
    @Getter
    private final int timeBinSize;
    final Map<MovementState, Map<MovementState, AtomicLong>> transitionCounts;
    private final AtomicLong totalTransitions;
    private final AtomicLong totalSamples;
    @Getter
    private final long createdTime;
    @Getter
    private volatile long lastUpdateTime;

    /**
     * Creates new Markov chain data with specified discretization granularity.
     *
     * @param binSize     Pixels per bin (e.g., 5 = movements grouped by 5-pixel increments)
     * @param timeBinSize Milliseconds per time bin
     */
    public MarkovChainData(int binSize, int timeBinSize)
    {
        this.binSize = binSize;
        this.timeBinSize = timeBinSize;
        this.transitionCounts = new ConcurrentHashMap<>();
        this.totalTransitions = new AtomicLong(0);
        this.totalSamples = new AtomicLong(0);
        this.createdTime = System.currentTimeMillis();
        this.lastUpdateTime = createdTime;
    }

    /**
     * Records a transition from one state to another.
     *
     * @param fromState Current state
     * @param toState   Next state
     */
    public void recordTransition(MovementState fromState, MovementState toState)
    {
        transitionCounts
            .computeIfAbsent(fromState, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(toState, k -> new AtomicLong(0))
            .incrementAndGet();

        totalTransitions.incrementAndGet();
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Records a sample (updates sample count).
     */
    public void recordSample()
    {
        totalSamples.incrementAndGet();
    }

    /**
     * Gets the probability distribution for transitions from a given state.
     *
     * @param fromState The starting state
     * @return Map of next states to their probabilities (sums to 1.0)
     */
    public Map<MovementState, Double> getTransitionProbabilities(MovementState fromState)
    {
        Map<MovementState, AtomicLong> counts = transitionCounts.get(fromState);
        if (counts == null || counts.isEmpty())
        {
            return Collections.emptyMap();
        }

        long total = counts.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();

        if (total == 0)
        {
            return Collections.emptyMap();
        }

        Map<MovementState, Double> probabilities = new HashMap<>();
        for (Map.Entry<MovementState, AtomicLong> entry : counts.entrySet())
        {
            double probability = entry.getValue().get() / (double) total;
            probabilities.put(entry.getKey(), probability);
        }

        return probabilities;
    }

    /**
     * Samples a next state based on transition probabilities from current state.
     *
     * @param fromState Current state
     * @param random    Random number generator
     * @return Next state, or null if no data available
     */
    public MovementState sampleNextState(MovementState fromState, Random random)
    {
        Map<MovementState, Double> probabilities = getTransitionProbabilities(fromState);
        if (probabilities.isEmpty())
        {
            return null;
        }

        double rand = random.nextDouble();
        double cumulative = 0.0;

        for (Map.Entry<MovementState, Double> entry : probabilities.entrySet())
        {
            cumulative += entry.getValue();
            if (rand <= cumulative)
            {
                return entry.getKey();
            }
        }

        return probabilities.keySet().iterator().next();
    }

    /**
     * Gets a random starting state weighted by frequency.
     *
     * @param random Random number generator
     * @return A starting state, or null if no data
     */
    public MovementState getRandomStartState(Random random)
    {
        if (transitionCounts.isEmpty())
        {
            return null;
        }

        List<MovementState> states = new ArrayList<>(transitionCounts.keySet());
        List<Long> weights = new ArrayList<>();

        for (MovementState state : states)
        {
            long weight = transitionCounts.get(state).values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
            weights.add(weight);
        }

        long totalWeight = weights.stream().mapToLong(Long::longValue).sum();
        long rand = (long) (random.nextDouble() * totalWeight);

        long cumulative = 0;
        for (int i = 0; i < states.size(); i++)
        {
            cumulative += weights.get(i);
            if (rand <= cumulative)
            {
                return states.get(i);
            }
        }

        return states.get(0);
    }

    /**
     * Merges data from another MarkovChainData (must have same binning).
     *
     * @param other Other chain data to merge
     */
    public void merge(MarkovChainData other)
    {
        if (this.binSize != other.binSize || this.timeBinSize != other.timeBinSize)
        {
            throw new IllegalArgumentException("Cannot merge chains with different bin sizes");
        }

        for (Map.Entry<MovementState, Map<MovementState, AtomicLong>> entry : other.transitionCounts.entrySet())
        {
            MovementState fromState = entry.getKey();
            Map<MovementState, AtomicLong> otherTransitions = entry.getValue();

            Map<MovementState, AtomicLong> ourTransitions = transitionCounts
                .computeIfAbsent(fromState, k -> new ConcurrentHashMap<>());

            for (Map.Entry<MovementState, AtomicLong> transitionEntry : otherTransitions.entrySet())
            {
                MovementState toState = transitionEntry.getKey();
                long count = transitionEntry.getValue().get();

                ourTransitions
                    .computeIfAbsent(toState, k -> new AtomicLong(0))
                    .addAndGet(count);
            }
        }

        totalTransitions.addAndGet(other.totalTransitions.get());
        totalSamples.addAndGet(other.totalSamples.get());
        lastUpdateTime = System.currentTimeMillis();
    }

    public long getTotalTransitions()
    {
        return totalTransitions.get();
    }

    public long getTotalSamples()
    {
        return totalSamples.get();
    }

    /**
     * Restores total transitions count (package-private for persistence layer).
     * Used during deserialization to efficiently restore saved state.
     */
    void setTotalTransitions(long value)
    {
        totalTransitions.set(value);
    }

    /**
     * Restores total samples count (package-private for persistence layer).
     * Used during deserialization to efficiently restore saved state.
     */
    void setTotalSamples(long value)
    {
        totalSamples.set(value);
    }

    public int getStateCount()
    {
        return transitionCounts.size();
    }

    /**
     * Gets the number of unique movement patterns (TO states) learned.
     * This is a better metric for training diversity than FROM state count.
     *
     * @return Number of unique movement deltas observed
     */
    public int getUniqueMovementCount()
    {
        Set<MovementState> uniqueToStates = new HashSet<>();

        for (Map<MovementState, AtomicLong> toStates : transitionCounts.values())
        {
            uniqueToStates.addAll(toStates.keySet());
        }

        return uniqueToStates.size();
    }

    /**
     * Returns statistics about this Markov chain.
     */
    public ChainStatistics getStatistics()
    {
        return new ChainStatistics(this);
    }

    /**
     * Gets aggregated state frequencies by delta-X and delta-Y (ignoring delta-time).
     * Used for visualization purposes.
     *
     * @return Map of (deltaX, deltaY) points to their total frequency across all time bins
     */
    public Map<Point, Long> getStateFrequenciesByPosition()
    {
        Map<Point, Long> frequencies = new HashMap<>();
        for (Map.Entry<MovementState, Map<MovementState, AtomicLong>> entry : transitionCounts.entrySet())
        {
            Map<MovementState, AtomicLong> toStates = entry.getValue();
            for (Map.Entry<MovementState, AtomicLong> toEntry : toStates.entrySet())
            {
                MovementState toState = toEntry.getKey();
                long count = toEntry.getValue().get();
                Point key = new Point(toState.getDeltaXBin(), toState.getDeltaYBin());
                frequencies.merge(key, count, Long::sum);
            }
        }

        return frequencies;
    }

    /**
     * Statistics about the Markov chain quality and coverage.
     */
    public static class ChainStatistics
    {
        public final int uniqueStates;
        public final int uniqueMovements;
        public final long totalTransitions;
        public final long totalSamples;
        public final double avgTransitionsPerState;
        public final int maxTransitionsFromState;
        public final long ageMillis;

        private ChainStatistics(MarkovChainData data)
        {
            this.uniqueStates = data.getStateCount();
            this.uniqueMovements = data.getUniqueMovementCount();
            this.totalTransitions = data.getTotalTransitions();
            this.totalSamples = data.getTotalSamples();
            this.avgTransitionsPerState = uniqueStates > 0
                ? (double) totalTransitions / uniqueStates
                : 0.0;

            int maxTrans = 0;
            for (Map<MovementState, AtomicLong> transitions : data.transitionCounts.values())
            {
                maxTrans = Math.max(maxTrans, transitions.size());
            }
            this.maxTransitionsFromState = maxTrans;
            this.ageMillis = System.currentTimeMillis() - data.createdTime;
        }

        @Override
        public String toString()
        {
            return String.format(
                "ChainStats{states=%d, movements=%d, transitions=%d, samples=%d, avg=%.2f, max=%d, age=%dms}",
                uniqueStates, uniqueMovements, totalTransitions, totalSamples, avgTransitionsPerState,
                maxTransitionsFromState, ageMillis
            );
        }
    }
}
