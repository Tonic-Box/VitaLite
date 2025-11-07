package com.tonic.util.handler;

import com.tonic.api.threaded.Delays;
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
        System.out.println("Executing step: " + STEP_POINTER);
        if(!steps.containsKey(STEP_POINTER))
        {
            STEP_POINTER = 0;
            return false;
        }

        STEP_POINTER = steps.get(STEP_POINTER).apply(context);
        System.out.println("Next step: " + STEP_POINTER);
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

    public void threaded()
    {
        while (step())
        {
            Delays.tick();
        }
    }
}