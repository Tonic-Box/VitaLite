package com.tonic.plugins.breakhandler;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;

@AllArgsConstructor
@Data
public class Break
{
    private String pluginName;
    private Instant startTime;
    private Duration duration;
    private BooleanSupplier canAccess;
    private BooleanSupplier canStart;
    private Runnable startCallback;
    private Runnable endCallback;
    private boolean started;

    /**
     * @return if a break has expired
     */
    public boolean isBreakOver()
    {
        if (!started)
        {
            return false;
        }

        return !startTime.plus(duration).isAfter(Instant.now());
    }

    /**
     * @return if a break is ready but not started
     */
    public boolean isBreakReady()
    {
        if (started)
        {
            return false;
        }

        return !startTime.isAfter(Instant.now());
    }
}
