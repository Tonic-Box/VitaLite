package com.tonic.util;

import com.tonic.Logger;

import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;

/**
 * Profiler for measuring task time duration
 */
@Singleton
public class Profiler {
    private String task;
    private Instant startup;
    private static Profiler INSTANCE;

    /**
     * start the profiler timer
     * @param task identifier of task we are profiling
     */
    public static void Start(String task)
    {
        if(INSTANCE == null)
            INSTANCE = new Profiler();
        INSTANCE.PStart(task);
    }

    /**
     * End the profiling session. Prints results to sout.
     */
    public static void Stop()
    {
        if(INSTANCE == null)
            return;
        INSTANCE.PStop();
    }

    public static void StopMS()
    {
        if(INSTANCE == null)
            return;
        INSTANCE.PStopMS();
    }

    /**
     * internal start()
     * @param task task name
     */
    private void PStart(String task)
    {
        this.task = task;
        startup = Instant.now();
    }

    /**
     * internal stop()
     */
    private void PStop()
    {
        Logger.info("[" + task + "] Took " + Duration.between(startup, Instant.now()).getSeconds() + " seconds.");
        System.out.println("[" + task + "] Took " + Duration.between(startup, Instant.now()).getSeconds() + " seconds.");
    }

    private void PStopMS()
    {
        Logger.info("[" + task + "] Took " + Duration.between(startup, Instant.now()).toMillis() + " ms.");
        System.out.println("[" + task + "] Took " + Duration.between(startup, Instant.now()).toMillis() + " ms.");
    }
}