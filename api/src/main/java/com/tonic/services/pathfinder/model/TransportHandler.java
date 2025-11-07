package com.tonic.services.pathfinder.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Manages transport stateful step execution for transports.
 */
public final class TransportHandler
{
    private final Map<Integer, Supplier<Integer>> steps;
    private int STEP_POINTER = 0;

    public TransportHandler(Map<Integer, Supplier<Integer>> steps) {
        this.steps = steps;
    }

    TransportHandler() {
        this.steps = new HashMap<>();
    }

    /**
     * Executes the current step and advances the step pointer.
     * @return true if there are more steps to execute, false if the transport is complete.
     */
    public boolean step()
    {
        if(!steps.containsKey(STEP_POINTER))
        {
            STEP_POINTER = 0;
            return false;
        }

        STEP_POINTER = steps.get(STEP_POINTER).get();
        return true;
    }

    /**
     * Gets the number of steps in this transport handler.
     * @return the number of steps.
     */
    public int size()
    {
        return steps.size();
    }

    /**
     * Adds a step to the transport handler.
     * @param step the step index.
     * @param action the action to perform at this step, returning the next step index.
     */
    public void add(int step, Supplier<Integer> action)
    {
        steps.put(step, action);
    }

    /**
     * Creates a simple transport handler that runs a single step followed by a delay.
     * @param runnable the action to perform.
     * @param delay the number of delay steps after the action.
     * @return the transport handler.
     */
    public static TransportHandler simple(Runnable runnable, int delay)
    {
        Map<Integer, Supplier<Integer>> steps = new HashMap<>();
        steps.put(0, () -> {
            runnable.run();
            return 1;
        });
        for(int i = 1; i < delay; i++)
        {
            final int next = i + 1;
            steps.put(i, () -> next);
        }
        return new TransportHandler(steps);
    }
}
