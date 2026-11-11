package com.tonic.util.handler;

import com.tonic.api.game.MovementAPI;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.services.pathfinder.model.WalkerPath;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.*;

/**
 * Builder for TransportHandler
 */
public class HandlerBuilder
{
    public static final int END_EXECUTION = Integer.MAX_VALUE;
    static final String PATH_KEY = "PATH";
    static final String TARGET_KEY = "TARGET_AREA";
    private static final String DELAY_PREFIX = "DELAY_";

    /**
     * Creates a new HandlerBuilder
     *
     * @return the HandlerBuilder
     */
    public static HandlerBuilder get() {
        return new HandlerBuilder();
    }

    public static StepHandler blank()
    {
        return new HandlerBuilder().build();
    }

    protected final StepHandler handler;
    protected Map<String,Integer> LABELS = new HashMap<>();

    public HandlerBuilder() {
        handler = new StepHandler();
    }

    public static void speedUp(StepContext context)
    {
        context.put("SPEED_UP", true);
    }

    public static int jump(String label, StepContext context)
    {
        context.put("MAGIC_LABEL_STEP", label);
        return 0xDECAFBAD;
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
        String key = DELAY_PREFIX + step;
        handler.add(step, context -> {
            int remaining = context.getOrDefault(key, delay) - 1;
            if (remaining <= 1) {
                context.remove(key);
                context.put("SPEED_UP", true);
                return step + 1;
            }
            context.put(key, remaining);
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
        String key = DELAY_PREFIX + step;
        handler.add(step, context -> {
            boolean met = condition.test(context);
            if (met) {
                context.remove(key);
                context.put("SPEED_UP", true);
                return step + 1;
            }
            int remaining = context.getOrDefault(key, timeout) - 1;
            if (remaining <= 0) {
                context.remove(key);
                onTimeout.accept(context);
                return END_EXECUTION;
            }
            context.put(key, remaining);
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
        handler.add(step, context -> {
            if(condition.test(context))
            {
                context.put("SPEED_UP", true);
                return step + 1;
            } else {
                return step;
            }
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
    public HandlerBuilder addDelayUntil(int step, BooleanSupplier condition)
    {
        return addDelayUntil(
                step,
                context -> condition.getAsBoolean()
        );
    }

    public HandlerBuilder append(int step, StepHandler handler)
    {
        return addDelayUntil(step, () -> !handler.step());
    }

    public HandlerBuilder append(int step, HandlerBuilder builder)
    {
        StepHandler handler = builder.build();
        return addDelayUntil(step, () -> !handler.step());
    }

    public HandlerBuilder walkTo(int step, WorldPoint location)
    {
        return walkTo(step, location, true);
    }

    public HandlerBuilder walkTo(int step, WorldPoint location, boolean useTeleports)
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .addDelayUntil(0, context -> {
                    WalkerPath path = context.get(PATH_KEY);
                    if(path == null)
                    {
                        path = WalkerPath.get(location, useTeleports);
                        context.put(PATH_KEY, path);
                    }
                    boolean value = !path.step();
                    if(value)
                    {
                        context.remove(PATH_KEY);
                    }
                    return value;
                })
                .addDelayUntil(1, () -> !MovementAPI.isMoving());
        return append(step, builder);
    }

    public HandlerBuilder walkToWorldPointSupplier(int step, Supplier<WorldPoint> locationSupplier)
    {
        return walkToWorldPointSupplier(step, locationSupplier, true);
    }

    public HandlerBuilder walkToWorldPointSupplier(int step, Supplier<WorldPoint> locationSupplier, boolean useTeleports)
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .addDelayUntil(0, context -> {
                    WorldPoint location = context.get(TARGET_KEY);
                    if(location == null)
                    {
                        location = locationSupplier.get();
                        context.put(TARGET_KEY, location);
                    }
                    WalkerPath path = context.get(PATH_KEY);
                    if(path == null)
                    {
                        path = WalkerPath.get(location, useTeleports);
                        context.put(PATH_KEY, path);
                    }
                    boolean value = !path.step();
                    if(value)
                    {
                        context.remove(TARGET_KEY);
                        context.remove(PATH_KEY);
                    }
                    return value;
                })
                .addDelayUntil(1, () -> !MovementAPI.isMoving());
        return append(step, builder);
    }

    public HandlerBuilder walkTo(int step, WorldArea location)
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .addDelayUntil(0, context -> {
                    if(location.contains(PlayerEx.getLocal().getWorldPoint()))
                    {
                        context.remove(PATH_KEY);
                        return true;
                    }
                    WalkerPath path = context.get(PATH_KEY);
                    if(path == null)
                    {
                        path = WalkerPath.get(List.of(location));
                        context.put(PATH_KEY, path);
                    }
                    boolean value = !path.step();
                    if(value)
                    {
                        context.remove(PATH_KEY);
                    }
                    return value;
                })
                .addDelayUntil(1, () -> !MovementAPI.isMoving());
        return append(step, builder);
    }

    public HandlerBuilder walkToWorldAreaSupplier(int step, Supplier<WorldArea> locationSupplier)
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .addDelayUntil(0, context -> {
                    WorldArea location = context.get(TARGET_KEY);
                    if(location == null)
                    {
                        location = locationSupplier.get();
                        context.put(TARGET_KEY, location);
                    }
                    if(location.contains(PlayerEx.getLocal().getWorldPoint()))
                    {
                        context.remove(TARGET_KEY);
                        context.remove(PATH_KEY);
                        return true;
                    }
                    WalkerPath path = context.get(PATH_KEY);
                    if(path == null)
                    {
                        path = WalkerPath.get(List.of(location));
                        context.put(PATH_KEY, path);
                    }
                    boolean value = !path.step();
                    if(value)
                    {
                        context.remove(TARGET_KEY);
                        context.remove(PATH_KEY);
                    }
                    return value;
                })
                .addDelayUntil(1, () -> !MovementAPI.isMoving());
        return append(step, builder);
    }

    /**
     * Builds the TransportHandler
     *
     * @return the TransportHandler
     */
    public StepHandler build() {
        if(!LABELS.isEmpty())
        {
            handler.getContext().getLabels().putAll(LABELS);
        }
        return handler;
    }

    static boolean isInternalKey(String key)
    {
        return key.equals(PATH_KEY) || key.equals(TARGET_KEY) || key.startsWith(DELAY_PREFIX);
    }
}
