package com.tonic.util.handler;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

/**
 * Builder for TransportHandler
 */
public class HandlerBuilder
{
    public static final int END_EXECUTION = Integer.MAX_VALUE;
    /**
     * Creates a new HandlerBuilder
     *
     * @return the HandlerBuilder
     */
    public static HandlerBuilder get() {
        return new HandlerBuilder();
    }

    protected final StepHandler handler;

    public HandlerBuilder() {
        handler = new StepHandler();
    }

    /**
     * Adds a step to the handler
     *
     * @param step     the step number
     * @param function the function that returns the next step number
     * @return the HandlerBuilder
     */
    public HandlerBuilder add(int step, Function<StepContext,Integer> function) {
        handler.add(step, function);
        return this;
    }

    /**
     * Adds a step to the handler
     *
     * @param step     the step number
     * @param supplier the supplier that returns the next step number
     * @return the HandlerBuilder
     */
    public HandlerBuilder add(int step, Supplier<Integer> supplier) {
        return add(step, context -> {
            return supplier.get();
        });
    }

    /**
     * Adds a step to the handler and assumes to return the next step number
     *
     * @param step     the step number
     * @param consumer the runnable to run
     * @return the HandlerBuilder
     */
    public HandlerBuilder add(int step, Consumer<StepContext> consumer) {
        return add(step, context -> {
            consumer.accept(context);
            return step + 1;
        });
    }

    /**
     * Adds a step to the handler and assumes to return the next step number
     *
     * @param step     the step number
     * @param runnable the runnable to run
     * @return the HandlerBuilder
     */
    public HandlerBuilder add(int step, Runnable runnable) {
        return add(step, context -> {
            runnable.run();
            return step + 1;
        });
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
        AtomicInteger remaining = new AtomicInteger(delay);
        handler.add(step, context -> {
            if (remaining.get() <= 1) {
                remaining.set(delay);
                return step + 1;
            }
            remaining.decrementAndGet();
            return step;
        });
        return this;
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param step      the step number
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public HandlerBuilder addDelayUntil(int step, int timeout, Predicate<StepContext> condition, Consumer<StepContext> onTimeout)
    {
        AtomicInteger remaining = new AtomicInteger(timeout);
        handler.add(step, context -> {
            boolean met = condition.test(context);
            if (met) {
                remaining.set(timeout);
                return step + 1;
            }
            if (remaining.get() <= 1) {
                remaining.set(timeout);
                onTimeout.accept(context);
                return END_EXECUTION;
            }
            return step;
        });
        return this;
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param step      the step number
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public HandlerBuilder addDelayUntil(int step, int timeout, BooleanSupplier condition, Runnable onTimeout)
    {
        return addDelayUntil(
                step, timeout,
                context -> condition.getAsBoolean(),
                context -> onTimeout.run()
        );
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param step      the step number
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public HandlerBuilder addDelayUntil(int step, Predicate<StepContext> condition)
    {
        handler.add(step, context -> condition.test(context) ? step + 1 : step);
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
        return addDelayUntil(
                step,
                context -> condition.getAsBoolean()
        );
    }

    public HandlerBuilder addWhileLoop(int step, int exit, Predicate<StepContext> condition, Consumer<HandlerBuilder> body)
    {
        handler.add(step, context -> {
            if(condition.test(context))
            {
                return exit + 1;
            }
            else
            {
                return step + 1;
            }
        });
        body.accept(this);
        handler.add(exit, context -> step);
        return this;
    }

    public HandlerBuilder addWhileLoop(int step, int exit, BooleanSupplier condition, Consumer<HandlerBuilder> body)
    {
        return addWhileLoop(
                step,
                exit,
                context -> condition.getAsBoolean(),
                body
        );
    }

    /**
     * Builds the TransportHandler
     *
     * @return the TransportHandler
     */
    public StepHandler build() {
        return handler;
    }
}
