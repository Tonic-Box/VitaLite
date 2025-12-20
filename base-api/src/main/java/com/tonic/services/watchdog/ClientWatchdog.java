package com.tonic.services.watchdog;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientWatchdog {
    private static final int MISSED_TICKS_THRESHOLD = 10;
    private static final long CHECK_INTERVAL_MS = 600L;
    private static final int MAX_RECOVERY_ATTEMPTS = 3;

    private static ClientWatchdog INSTANCE;

    private volatile int lastTickCount;
    private final AtomicInteger missedTickChecks = new AtomicInteger(0);
    private final AtomicInteger recoveryAttempts = new AtomicInteger(0);
    private final AtomicBoolean desyncDetected = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler;
    private volatile boolean loggedIn;

    private ClientWatchdog() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VitaLite-Watchdog");
            t.setDaemon(true);
            return t;
        });
    }

    public static synchronized void init() {
        if (INSTANCE != null) {
            return;
        }
        INSTANCE = new ClientWatchdog();
        INSTANCE.scheduler.scheduleAtFixedRate(INSTANCE::checkHealth, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        Logger.info("[Watchdog] Client watchdog initialized");
    }

    public static void shutdown() {
        if (INSTANCE != null) {
            INSTANCE.scheduler.shutdownNow();
            INSTANCE = null;
            Logger.info("[Watchdog] Client watchdog shutdown");
        }
    }

    public static void setLoggedIn(boolean loggedIn) {
        if (INSTANCE != null) {
            INSTANCE.loggedIn = loggedIn;
            if (!loggedIn) {
                INSTANCE.missedTickChecks.set(0);
                INSTANCE.desyncDetected.set(false);
            }
        }
    }

    private void checkHealth() {
        if (!loggedIn) {
            return;
        }

        try {
            TClient client = Static.getClient();
            if (client == null) {
                return;
            }

            int currentTick = client.getTickCount();
            if (currentTick == lastTickCount) {
                int newMissed = missedTickChecks.incrementAndGet();
                if (newMissed >= MISSED_TICKS_THRESHOLD) {
                    onDesync(newMissed);
                }
            } else {
                lastTickCount = currentTick;
                missedTickChecks.set(0);
                if (desyncDetected.compareAndSet(true, false)) {
                    Logger.info("[Watchdog] Client thread recovered - ticks resumed");
                    recoveryAttempts.set(0);
                    TrackedInvoke.triggerResetHook();
                }
            }
        } catch (Throwable t) {
            Logger.error("[Watchdog] Error in health check: " + t.getMessage());
        }
    }

    private void onDesync(int missedChecks) {
        if (!desyncDetected.compareAndSet(false, true)) {
            return;
        }

        Logger.warn("[Watchdog] No game ticks for " + missedChecks + " checks (~" + (missedChecks * 600) + "ms) - client thread may be stuck");

        TrackedRunnable current = TrackedRunnable.getCurrentlyExecuting();
        if (current != null) {
            long elapsed = System.currentTimeMillis() - current.getQueuedAt();
            Logger.error("[Watchdog] Client Thread Dead-Lock detected (running for " + elapsed + "ms)");
            Logger.error(current.getCallerStack());
        }

        int attempts = recoveryAttempts.incrementAndGet();
        if (attempts <= MAX_RECOVERY_ATTEMPTS) {
            Logger.info("[Watchdog] Attempting recovery (attempt " + attempts + "/" + MAX_RECOVERY_ATTEMPTS + ")");
            RecoveryManager.attemptRecovery(RecoveryManager.RecoveryStrategy.CANCEL_PENDING);
        } else {
            Logger.error("[Watchdog] Max recovery attempts exceeded - manual intervention required");
        }
    }
}
