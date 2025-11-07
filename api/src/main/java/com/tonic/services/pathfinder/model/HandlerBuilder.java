package com.tonic.services.pathfinder.model;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Builder for TransportHandler
 */
public class HandlerBuilder {
    /**
     * Creates a new HandlerBuilder
     *
     * @return the HandlerBuilder
     */
    public static HandlerBuilder get() {
        return new HandlerBuilder();
    }

    private final TransportHandler handler;

    HandlerBuilder() {
        handler = new TransportHandler();
    }

    /**
     * Adds a step to the handler
     *
     * @param step     the step number
     * @param supplier the supplier that returns the next step number
     * @return the HandlerBuilder
     */
    public HandlerBuilder add(int step, Supplier<Integer> supplier) {
        handler.add(step, supplier);
        return this;
    }

    /**
     * Adds a step to the handler and assumes to return the next step number
     *
     * @param step     the step number
     * @param runnable the runnable to run
     * @return the HandlerBuilder
     */
    public HandlerBuilder add(int step, Runnable runnable) {
        handler.add(step, () -> {
            runnable.run();
            return step + 1;
        });
        return this;
    }

    /**
     * Adds a delay to the handler
     *
     * @param step  the step number
     * @param delay the delay in steps
     * @return the HandlerBuilder
     */
    public HandlerBuilder addDelay(int step, int delay)
    {
        for(int i = step; i < step + delay; i++)
        {
            int finalI = i;
            handler.add(i, () -> finalI + 1);
        }
        return this;
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param step      the step number
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public HandlerBuilder addDelayUntil(int step, BooleanSupplier condition)
    {
        handler.add(step, () -> condition.getAsBoolean() ? step + 1 : step);
        return this;
    }

    /**
     * Builds the TransportHandler
     *
     * @return the TransportHandler
     */
    public TransportHandler build() {
        return handler;
    }
}
