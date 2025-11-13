package com.tonic.util.handler;

import com.tonic.data.WorldLocation;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.HashMap;
import java.util.Map;
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
     * @param label the label for the step
     * @param function the function that returns the next step number
     * @return the HandlerBuilder
     */
    public Q add(String label, Function<StepContext,Integer> function) {
        LABELS.put(label, currentStep);
        return add(function);
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
     * Adds a step to the handler
     *
     * @param label the label for the step
     * @param supplier the supplier that returns the next step number
     * @return the HandlerBuilder
     */
    public Q add(String label, Supplier<Integer> supplier) {
        LABELS.put(label, currentStep);
        return add(supplier);
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
     * @param label the label for the step
     * @param consumer the runnable to run
     * @return the HandlerBuilder
     */
    public Q add(String label, Consumer<StepContext> consumer) {
        LABELS.put(label, currentStep);
        return add(consumer);
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
     * Adds a step to the handler and assumes to return the next step number
     *
     * @param label the label for the step
     * @param runnable the runnable to run
     * @return the HandlerBuilder
     */
    public Q add(String label, Runnable runnable) {
        LABELS.put(label, currentStep);
        return add(runnable);
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
     * Adds a delay to the handler
     *
     * @param label the label for the step
     * @param delay the delay in steps
     * @return the HandlerBuilder
     */
    public Q addDelay(String label, int delay)
    {
        LABELS.put(label, currentStep);
        return addDelay(delay);
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param timeout the timeout in steps
     * @param condition the condition to meet
     * @param onTimeout the action to perform on timeout
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
     * @param label the label for the step
     * @param timeout the timeout in steps
     * @param condition the condition to meet
     * @param onTimeout the action to perform on timeout
     * @return the HandlerBuilder
     */
    public Q addDelayUntil(String label, int timeout, Predicate<StepContext> condition, Consumer<StepContext> onTimeout)
    {
        LABELS.put(label, currentStep);
        return addDelayUntil(timeout, condition, onTimeout);
    }

    /**
     * Adds a delay until a condition is met
     *
     * @param timeout the timeout in steps
     * @param condition the condition to meet
     * @param onTimeout the action to perform on timeout
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
     * @param label the label for the step
     * @param timeout the timeout in steps
     * @param condition the condition to meet
     * @param onTimeout the action to perform on timeout
     * @return the HandlerBuilder
     */
    public Q addDelayUntil(String label, int timeout, BooleanSupplier condition, Runnable onTimeout)
    {
        LABELS.put(label, currentStep);
        return addDelayUntil(
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
     * @param label the label for the step
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public Q addDelayUntil(String label, Predicate<StepContext> condition)
    {
        LABELS.put(label, currentStep);
        return addDelayUntil(condition);
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

    /**
     * Adds a delay until a condition is met
     *
     * @param label the label for the step
     * @param condition the condition to meet
     * @return the HandlerBuilder
     */
    public Q addDelayUntil(String label, BooleanSupplier condition)
    {
        LABELS.put(label, currentStep);
        return addDelayUntil(condition);
    }

    /**
     * Appends a step handler to the handler
     *
     * @param handler the step handler to append
     * @return the HandlerBuilder
     */
    public Q append(StepHandler handler)
    {
        append(currentStep++, handler);
        return self();
    }

    /**
     * Appends a step handler to the handler
     *
     * @param label the label for the step
     * @param handler the step handler to append
     * @return the HandlerBuilder
     */
    public Q append(String label, StepHandler handler)
    {
        LABELS.put(label, currentStep);
        return append(handler);
    }

    /**
     * Appends a handler builder to the handler
     *
     * @param builder the handler builder to append
     * @return the HandlerBuilder
     */
    public Q append(HandlerBuilder builder)
    {
        append(currentStep++, builder);
        return self();
    }

    /**
     * Appends a handler builder to the handler
     *
     * @param label the label for the step
     * @param builder the handler builder to append
     * @return the HandlerBuilder
     */
    public Q append(String label, HandlerBuilder builder)
    {
        LABELS.put(label, currentStep);
        return append(builder);
    }

    /**
     * Adds a walk to step to the handler
     *
     * @param location the location to walk to
     * @return the HandlerBuilder
     */
    public Q walkTo(WorldPoint location)
    {
        walkTo(currentStep++, location);
        return self();
    }

    /**
     * Adds a walk to step to the handler
     *
     * @param label the label for the step
     * @param location the location to walk to
     * @return the HandlerBuilder
     */
    public Q walkTo(String label, WorldPoint location)
    {
        LABELS.put(label, currentStep);
        return walkTo(location);
    }

    /**
     * Adds a walk to step to the handler
     *
     * @param location the location to walk to
     * @return the HandlerBuilder
     */
    public Q walkTo(WorldArea location)
    {
        walkTo(currentStep++, location);
        return self();
    }

    /**
     * Adds a walk to step to the handler
     *
     * @param label the label for the step
     * @param location the location to walk to
     * @return the HandlerBuilder
     */
    public Q walkTo(String label, WorldArea location)
    {
        LABELS.put(label, currentStep);
        return walkTo(location);
    }

    /**
     * Adds a walk to step to the handler
     *
     * @param location the location to walk to
     * @return the HandlerBuilder
     */
    public Q walkToWorldAreaSupplier(Supplier<WorldArea> location)
    {
        walkToWorldAreaSupplier(currentStep++, location);
        return self();
    }

    /**
     * Adds a walk to step to the handler
     *
     * @param label the label for the step
     * @param location the location to walk to
     * @return the HandlerBuilder
     */
    public Q walkToWorldAreaSupplier(String label, Supplier<WorldArea> location)
    {
        LABELS.put(label, currentStep);
        return walkToWorldAreaSupplier(location);
    }

    /**
     * Adds a walk to step to the handler
     *
     * @param location the location to walk to
     * @return the HandlerBuilder
     */
    public Q walkToWorldPointSupplier(Supplier<WorldPoint> location)
    {
        walkToWorldPointSupplier(currentStep++, location);
        return self();
    }

    /**
     * Adds a walk to step to the handler
     *
     * @param label the label for the step
     * @param location the location to walk to
     * @return the HandlerBuilder
     */
    public Q walkToWorldPointSupplier(String label, Supplier<WorldPoint> location)
    {
        LABELS.put(label, currentStep);
        return walkToWorldPointSupplier(location);
    }
}
