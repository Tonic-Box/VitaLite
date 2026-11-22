package com.tonic.injector.util;

import java.util.ArrayList;
import java.util.List;

/**
 * The hooks an injector run could not apply, collected so the run fails after every mixin has been tried and before patches.zip is written.
 */
public final class InjectionFailures
{
    private static final List<String> failures = new ArrayList<>();

    private InjectionFailures()
    {
    }

    /**
     * Records a hook that could not be applied.
     *
     * @param failure what could not be applied and why
     */
    public static void record(String failure)
    {
        failures.add(failure);
        System.err.println("Injection failed: " + failure);
    }

    /**
     * Records a failure unless a condition holds.
     *
     * @param condition what a hook needs to have found
     * @param failure what could not be applied when it does not hold
     * @return the condition
     */
    public static boolean require(boolean condition, String failure)
    {
        if (!condition)
        {
            record(failure);
        }
        return condition;
    }

    /**
     * Fails the run when any hook could not be applied.
     *
     * @throws IllegalStateException listing every hook that could not be applied
     */
    public static void throwIfAny()
    {
        if (failures.isEmpty())
        {
            return;
        }
        StringBuilder message = new StringBuilder(failures.size() + " hook(s) could not be applied, patches.zip was not written:");
        for (String failure : failures)
        {
            message.append(System.lineSeparator()).append("  ").append(failure);
        }
        throw new IllegalStateException(message.toString());
    }
}
