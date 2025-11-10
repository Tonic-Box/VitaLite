package com.tonic.services.mouserecorder.markov;

import com.tonic.services.mouserecorder.IMouseMovementGenerator;
import com.tonic.services.mouserecorder.MouseMovementSequence;
import lombok.Getter;

import java.util.Random;

/**
 * Mouse movement generator that uses learned Markov chain data to produce
 * realistic movements based on recorded human gameplay patterns.
 * -
 * This generator produces movements that statistically match the training data,
 * making them significantly harder to detect than algorithmic approaches.
 */
public class MarkovMouseGenerator implements IMouseMovementGenerator
{
    /**
     * -- GETTER --
     *  Returns the underlying Markov chain data.
     */
    @Getter
    private final MarkovChainData chainData;
    private final Random random;
    private final int maxSteps;
    private final double targetDistanceTolerance;

    /**
     * Creates a Markov-based mouse generator.
     *
     * @param chainData               Trained Markov chain data
     * @param random                  Random number generator
     * @param maxSteps                Maximum steps to generate (prevents infinite loops)
     * @param targetDistanceTolerance Acceptable distance from target (in pixels)
     */
    public MarkovMouseGenerator(MarkovChainData chainData, Random random, int maxSteps, double targetDistanceTolerance)
    {
        this.chainData = chainData;
        this.random = random;
        this.maxSteps = maxSteps;
        this.targetDistanceTolerance = targetDistanceTolerance;
    }

    /**
     * Creates a generator with default settings.
     *
     * @param chainData Trained Markov chain data
     */
    public MarkovMouseGenerator(MarkovChainData chainData)
    {
        this(chainData, new Random(), 200, 10.0);
    }

    @Override
    public MouseMovementSequence generate(int startX, int startY, int endX, int endY)
    {
        return generate(startX, startY, endX, endY, System.currentTimeMillis());
    }

    @Override
    public MouseMovementSequence generate(int startX, int startY, int endX, int endY, long startTimeMs)
    {
        if (chainData.getTotalTransitions() == 0)
        {
            throw new IllegalStateException("Markov chain has no training data");
        }

        MouseMovementSequence.Builder builder = MouseMovementSequence.builder();

        int currentX = startX;
        int currentY = startY;
        long currentTime = startTimeMs;

        builder.add(currentX, currentY, currentTime);

        MovementState currentState = chainData.getRandomStartState(random);
        if (currentState == null)
        {
            currentState = new MovementState(0, 0, 0);
        }

        int steps = 0;
        double distanceToTarget = distance(currentX, currentY, endX, endY);

        while (distanceToTarget > targetDistanceTolerance && steps < maxSteps)
        {
            MovementState nextState = chainData.sampleNextState(currentState, random);

            if (nextState == null)
            {
                nextState = chainData.getRandomStartState(random);
                if (nextState == null)
                {
                    break;
                }
            }

            int deltaX = nextState.toApproxDeltaX(chainData.getBinSize());
            int deltaY = nextState.toApproxDeltaY(chainData.getBinSize());
            int deltaTime = nextState.toApproxDeltaTime(chainData.getTimeBinSize());

            deltaX = biasTowardTarget(deltaX, currentX, endX, distanceToTarget);
            deltaY = biasTowardTarget(deltaY, currentY, endY, distanceToTarget);

            currentX += deltaX;
            currentY += deltaY;
            currentTime += Math.max(1, deltaTime);

            builder.add(currentX, currentY, currentTime);

            currentState = nextState;
            distanceToTarget = distance(currentX, currentY, endX, endY);
            steps++;
        }

        if (distanceToTarget > 0)
        {
            builder.add(endX, endY, currentTime + chainData.getTimeBinSize());
        }

        return builder.build();
    }

    /**
     * Biases a delta toward the target, with stronger bias as distance decreases.
     * This ensures we eventually reach the target while maintaining natural variation.
     */
    private int biasTowardTarget(int delta, int current, int target, double distanceToTarget)
    {
        int directionToTarget = target - current;

        // Calculate bias strength - balance between natural Markov curves and reaching target
        // Gentle minimum bias prevents extreme wandering while preserving natural curves
        // Gradual increase ensures smooth arrival without perfectly straight paths
        double normalizedDistance = Math.min(1.0, distanceToTarget / 400.0);
        double biasFactor = 0.15 + (0.60 * (1.0 - normalizedDistance));

        if (directionToTarget != 0)
        {
            return (int) (delta * (1.0 - biasFactor) + directionToTarget * biasFactor);
        }

        return delta;
    }

    /**
     * Calculates Euclidean distance between two points.
     */
    private double distance(int x1, int y1, int x2, int y2)
    {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    @Override
    public String getName()
    {
        return "Markov";
    }

    @Override
    public String getDescription()
    {
        return String.format("Markov chain generator (states=%d, transitions=%d)",
            chainData.getStateCount(), chainData.getTotalTransitions());
    }

    /**
     * Sets the random seed for reproducible generation.
     *
     * @param seed Random seed
     */
    public void setSeed(long seed)
    {
        random.setSeed(seed);
    }
}
