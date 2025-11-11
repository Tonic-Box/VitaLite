package com.tonic.util.handler;

import com.tonic.data.WorldLocation;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import java.util.function.*;

public abstract class AbstractHandlerBuilder extends HandlerBuilder
{
    protected int currentStep = 0;

    /**
     * Adds a step to the handler
     *
     * @param function the function that returns the next step number
     * @return the HandlerBuilder
     */
    public HandlerBuilder add(Function<StepContext,Integer> function) {
        return add(currentStep++, function);
    }

    /**
     * Adds a step to the handler
     *
     * @param supplier the supplier that returns the next step number
     * @return the HandlerBuilder
     */
    public HandlerBuilder add(Supplier<Integer> supplier) {
        return add(currentStep++, supplier);
    }

    /**
     * Adds a step to the handler and assumes to return the next step number
     *
     * @param consumer the runnable to run
     * @return the HandlerBuilder
     */
    public HandlerBuilder add(Consumer<StepContext> consumer) {
        return add(currentStep++, consumer);
    }

    /**
     * Adds a step to the handler and assumes to return the next step number
     *
     * @param runnable the runnable to run
     * @return the HandlerBuilder
     */
    public HandlerBuilder add(Runnable runnable) {
        return add(currentStep++, runnable);
    }

    /**
     * Adds a delay to the handler
     *
     * @param delay the delay in steps
     * @return the HandlerBuilder
     */
    public HandlerBuilder addDelay(int delay)
    {
        return addDelay(currentStep++, delay);
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public HandlerBuilder addDelayUntil(int timeout, Predicate<StepContext> condition, Consumer<StepContext> onTimeout)
    {
        return addDelayUntil(currentStep++, timeout, condition, onTimeout);
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public HandlerBuilder addDelayUntil(int timeout, BooleanSupplier condition, Runnable onTimeout)
    {
        return addDelayUntil(
                currentStep++,
                timeout,
                condition,
                onTimeout
        );
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public HandlerBuilder addDelayUntil(Predicate<StepContext> condition)
    {
        return addDelayUntil(
                currentStep++,
                condition
        );
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public HandlerBuilder addDelayUntil(BooleanSupplier condition)
    {
        return addDelayUntil(
                currentStep++,
                condition
        );
    }

    public HandlerBuilder append(StepHandler handler)
    {
        return append(currentStep++, handler);
    }

    public HandlerBuilder append(HandlerBuilder builder)
    {
        return append(currentStep++, builder);
    }

    public HandlerBuilder walkTo(WorldPoint location)
    {
        return walkTo(currentStep++, location);
    }

    public HandlerBuilder walkTo(WorldArea location)
    {
        return walkTo(currentStep++, location);
    }

    public HandlerBuilder walkToWorldAreaSupplier(Supplier<WorldArea> location)
    {
        return walkToWorldAreaSupplier(currentStep++, location);
    }

    public HandlerBuilder walkToWorldPointSupplier(Supplier<WorldPoint> location)
    {
        return walkToWorldPointSupplier(currentStep++, location);
    }
}
