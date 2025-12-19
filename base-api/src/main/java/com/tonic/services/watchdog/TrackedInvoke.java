package com.tonic.services.watchdog;

import com.tonic.Logger;
import com.tonic.exceptions.InvokeTimeoutException;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class TrackedInvoke<T> {
    private static final long DEFAULT_TIMEOUT_MS = 3000L;
    private static Runnable globalCancellationHook;
    private static Runnable globalResetHook;

    @Getter
    private final CompletableFuture<T> future;
    private final long startTime;
    @Getter
    private final String callerInfo;
    private final AtomicBoolean cancelled;

    public static void setGlobalCancellationHook(Runnable hook) {
        globalCancellationHook = hook;
    }

    public static void setGlobalResetHook(Runnable hook) {
        globalResetHook = hook;
    }

    public static void triggerResetHook() {
        if (globalResetHook != null) {
            try {
                globalResetHook.run();
            } catch (Throwable t) {
                Logger.error("[Watchdog] Error in reset hook: " + t.getMessage());
            }
        }
    }

    public static void triggerCancellationHook() {
        if (globalCancellationHook != null) {
            try {
                globalCancellationHook.run();
            } catch (Throwable t) {
                Logger.error("[Watchdog] Error in cancellation hook: " + t.getMessage());
            }
        }
    }

    public TrackedInvoke(CompletableFuture<T> future, String callerInfo) {
        this.future = future;
        this.startTime = System.currentTimeMillis();
        this.callerInfo = callerInfo;
        this.cancelled = new AtomicBoolean(false);
    }

    public T getWithTimeout() throws InvokeTimeoutException {
        return getWithTimeout(DEFAULT_TIMEOUT_MS);
    }

    public T getWithTimeout(long timeoutMs) throws InvokeTimeoutException {
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            onTimeout(timeoutMs);
            throw new InvokeTimeoutException(callerInfo, timeoutMs, ThreadDiagnostics.generateFullDump());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Invoke interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Invoke execution failed", cause);
        }
    }

    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            future.cancel(true);
            if (globalCancellationHook != null) {
                try {
                    globalCancellationHook.run();
                } catch (Throwable t) {
                    Logger.error("[Watchdog] Error in cancellation hook: " + t.getMessage());
                }
            }
        }
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public boolean isDone() {
        return future.isDone();
    }

    public long getElapsedMs() {
        return System.currentTimeMillis() - startTime;
    }

    private void onTimeout(long timeoutMs) {
        String dump = ThreadDiagnostics.generateFullDump();
        Logger.error("[Watchdog] Invoke timeout after " + timeoutMs + "ms from " + callerInfo);
        Logger.error(dump);
        ThreadDiagnostics.dumpToFile(dump);
        cancel();
    }
}
