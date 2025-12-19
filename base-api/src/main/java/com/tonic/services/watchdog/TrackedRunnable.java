package com.tonic.services.watchdog;

import java.util.function.BooleanSupplier;

public class TrackedRunnable implements Runnable, BooleanSupplier {
    private static volatile TrackedRunnable currentlyExecuting;

    private final Runnable delegate;
    private final String callerStack;
    private final long queuedAt;

    public TrackedRunnable(Runnable delegate) {
        this.delegate = delegate;
        this.callerStack = ThreadDiagnostics.getCallerStack();
        this.queuedAt = System.currentTimeMillis();
    }

    @Override
    public void run() {
        currentlyExecuting = this;
        try {
            delegate.run();
        } finally {
            currentlyExecuting = null;
        }
    }

    @Override
    public boolean getAsBoolean() {
        run();
        return true;
    }

    public static TrackedRunnable getCurrentlyExecuting() {
        return currentlyExecuting;
    }

    public String getCallerStack() {
        return callerStack;
    }

    public long getQueuedAt() {
        return queuedAt;
    }
}
