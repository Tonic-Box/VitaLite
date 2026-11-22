package com.tonic.services.profiler.frames;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Takes one thread's stack at a fixed period on its own daemon thread and keeps the samples until they are drained by time.
 */
final class StackSampler implements Runnable
{
    private static final int FRAMES = 16;
    private static final int MAX_KEPT = 4096;

    private final Thread target;
    private final long periodMillis;
    private final ArrayDeque<Sample> samples = new ArrayDeque<>();
    private final Thread sampler;
    private volatile boolean running = true;

    /**
     * Starts sampling.
     *
     * @param target the thread whose stack is taken
     * @param periodMillis milliseconds between samples
     */
    StackSampler(Thread target, int periodMillis)
    {
        this.target = target;
        this.periodMillis = periodMillis;
        sampler = new Thread(this, "Frame Stack Sampler");
        sampler.setDaemon(true);
        sampler.start();
    }

    @Override
    public void run()
    {
        while (running)
        {
            try
            {
                Thread.sleep(periodMillis);
            }
            catch (InterruptedException e)
            {
                return;
            }
            StackTraceElement[] stack = target.getStackTrace();
            long at = System.nanoTime();
            StringBuilder key = new StringBuilder();
            for (int i = 0; i < Math.min(FRAMES, stack.length); i++)
            {
                if (i > 0)
                {
                    key.append(" < ");
                }
                key.append(stack[i].getClassName()).append('.').append(stack[i].getMethodName());
            }
            synchronized (samples)
            {
                if (samples.size() == MAX_KEPT)
                {
                    samples.removeFirst();
                }
                samples.addLast(new Sample(at, key.toString()));
            }
        }
    }

    /**
     * Takes the samples taken before a time, dropping any taken before another.
     *
     * @param from the earliest time kept, from System.nanoTime
     * @param to the time samples are taken before
     * @param into receives the stacks of the samples taken from one time to the other, innermost frame first
     */
    void drain(long from, long to, List<String> into)
    {
        synchronized (samples)
        {
            while (!samples.isEmpty() && samples.peekFirst().at - to < 0)
            {
                Sample sample = samples.pollFirst();
                if (sample.at - from >= 0)
                {
                    into.add(sample.stack);
                }
            }
        }
    }

    /**
     * Stops sampling; safe to call more than once.
     */
    void stop()
    {
        running = false;
        sampler.interrupt();
    }

    private static final class Sample
    {
        private final long at;
        private final String stack;

        private Sample(long at, String stack)
        {
            this.at = at;
            this.stack = stack;
        }
    }
}
