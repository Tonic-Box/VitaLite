package com.tonic.util.handler;

import com.tonic.Logger;
import com.tonic.api.threaded.Delays;
import com.tonic.services.pathfinder.model.WalkerPath;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Manages transport stateful step execution.
 */
public final class StepHandler
{
    @Getter
    private final StepContext context = new StepContext();

    private final Map<Integer, Function<StepContext,Integer>> steps;
    private int STEP_POINTER = 0;

    public StepHandler(Map<Integer, Function<StepContext,Integer>> steps) {
        this.steps = steps;
    }

    StepHandler() {
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
            reset();
            return false;
        }
        STEP_POINTER = steps.get(STEP_POINTER).apply(context);
        if(context.get("SPEED_UP") != null && steps.containsKey(STEP_POINTER))
        {
            context.remove("SPEED_UP");
            return step();
        }
        return true;
    }

    /**
     * Resets the step handler.
     */
    public void reset()
    {
        STEP_POINTER = 0;
        context.remove("SPEED_UP");
        for(Object value : context.values())
        {
            if(value instanceof WalkerPath)
            {
                ((WalkerPath) value).shutdown();
            }
        }
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
     * @param function the action to perform at this step, returning the next step index.
     */
    public void add(int step, Function<StepContext,Integer> function)
    {
        steps.put(step, function);
    }

    /**
     * Creates a simple transport handler that runs a single step followed by a delay.
     * @param runnable the action to perform.
     * @param delay the number of delay steps after the action.
     * @return the transport handler.
     */
    public static StepHandler simple(Runnable runnable, int delay)
    {
        return HandlerBuilder.get()
                .add(0, runnable)
                .addDelay(1, delay)
                .build();
    }

    public void execute()
    {
        while (step())
        {
            Delays.tick();
        }
    }
}