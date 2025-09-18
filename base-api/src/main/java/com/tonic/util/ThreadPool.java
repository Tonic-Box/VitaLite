package com.tonic.util;

import com.tonic.Static;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class ThreadPool
{
    private final static ExecutorService executor = Executors.newCachedThreadPool();
    private static ScheduledExecutorService executorService;

    public static Future<?> submit(Runnable runnable)
    {
        return executor.submit(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                System.err.println("Task execution failed: " + e.getMessage());
            }
        });
    }

    public static <T> T submit(Supplier<T> supplier)
    {
        CompletableFuture<T> future = new CompletableFuture<>();
        Runnable runnable = () -> future.complete(supplier.get());
        submit(runnable);
        return future.join();
    }

    public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
    {
        if(executorService == null || executorService.isShutdown())
        {
            executorService = Static.getInjector().getInstance(ScheduledExecutorService.class);
        }
        return executorService.schedule(command, delay, unit);
    }

    public static void shutdown()
    {
        executor.shutdown();
    }
}