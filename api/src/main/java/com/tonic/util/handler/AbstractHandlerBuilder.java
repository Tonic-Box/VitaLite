package com.tonic.util.handler;

import com.tonic.data.WorldLocation;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import java.util.function.*;

public abstract class AbstractHandlerBuilder<Q> extends HandlerBuilder
{
    protected int currentStep = 0;

    @SuppressWarnings("unchecked")
    protected final Q self() {
        return (Q) this;
    }

    /**
     * Adds a step to the handler
     *
     * @param function the function that returns the next step number
     * @return the HandlerBuilder
     */
    public Q add(Function<StepContext,Integer> function) {
        add(currentStep++, function);
        return self();
    }

    /**
     * Adds a step to the handler
     *
     * @param supplier the supplier that returns the next step number
     * @return the HandlerBuilder
     */
    public Q add(Supplier<Integer> supplier) {
        add(currentStep++, supplier);
        return self();
    }

    /**
     * Adds a step to the handler and assumes to return the next step number
     *
     * @param consumer the runnable to run
     * @return the HandlerBuilder
     */
    public Q add(Consumer<StepContext> consumer) {
        add(currentStep++, consumer);
        return self();
    }

    /**
     * Adds a step to the handler and assumes to return the next step number
     *
     * @param runnable the runnable to run
     * @return the HandlerBuilder
     */
    public Q add(Runnable runnable) {
        add(currentStep++, runnable);
        return self();
    }

    /**
     * Adds a delay to the handler
     *
     * @param delay the delay in steps
     * @return the HandlerBuilder
     */
    public Q addDelay(int delay)
    {
        addDelay(currentStep++, delay);
        return self();
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public Q addDelayUntil(int timeout, Predicate<StepContext> condition, Consumer<StepContext> onTimeout)
    {
        addDelayUntil(currentStep++, timeout, condition, onTimeout);
        return self();
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public Q addDelayUntil(int timeout, BooleanSupplier condition, Runnable onTimeout)
    {
        addDelayUntil(
                currentStep++,
                timeout,
                condition,
                onTimeout
        );
        return self();
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public Q addDelayUntil(Predicate<StepContext> condition)
    {
        addDelayUntil(
                currentStep++,
                condition
        );
        return self();
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public Q addDelayUntil(BooleanSupplier condition)
    {
        addDelayUntil(
                currentStep++,
                condition
        );
        return self();
    }

    public Q append(StepHandler handler)
    {
        append(currentStep++, handler);
        return self();
    }

    public Q append(HandlerBuilder builder)
    {
        append(currentStep++, builder);
        return self();
    }

    public Q walkTo(WorldPoint location)
    {
        walkTo(currentStep++, location);
        return self();
    }

    public Q walkTo(WorldArea location)
    {
        walkTo(currentStep++, location);
        return self();
    }

    public Q walkToWorldAreaSupplier(Supplier<WorldArea> location)
    {
        walkToWorldAreaSupplier(currentStep++, location);
        return self();
    }

    public Q walkToWorldPointSupplier(Supplier<WorldPoint> location)
    {
        walkToWorldPointSupplier(currentStep++, location);
        return self();
    }
}
