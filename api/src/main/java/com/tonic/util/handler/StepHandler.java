package com.tonic.util.handler;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.services.pathfinder.model.WalkerPath;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Manages transport stateful step execution.
 */
public final class StepHandler
{
    private static final int MAGIC_LABEL_STEP = 0xDECAFBAD;
    @Getter
    private final StepContext context = new StepContext();

    private final Map<Integer, Function<StepContext,Integer>> steps;
    private int STEP_POINTER = 0;

    @Getter
    private boolean started = false;

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
        started = true;
        if(!steps.containsKey(STEP_POINTER))
        {
            reset();
            return false;
        }
        STEP_POINTER = steps.get(STEP_POINTER).apply(context);
        STEP_POINTER = translate(STEP_POINTER);
        if(context.get("SPEED_UP") != null && steps.containsKey(STEP_POINTER))
        {
            context.remove("SPEED_UP");
            return step();
        }
        return true;
    }

    private int translate(int pointer)
    {
        if(pointer != MAGIC_LABEL_STEP || !context.contains("MAGIC_LABEL_STEP"))
            return pointer;

        String label = context.get("MAGIC_LABEL_STEP");
        if(!context.getLabels().containsKey(label))
        {
            Logger.warn("No such label: " + label);
            return pointer;
        }

        return context.getLabels().get(label);
    }

    /**
     * Resets the step handler.
     */
    public void reset()
    {
        started = false;
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

    public void execute()
    {
        while (step())
        {
            Delays.tick();
        }
    }
}