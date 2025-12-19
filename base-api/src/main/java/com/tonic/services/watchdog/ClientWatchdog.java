package com.tonic.services.watchdog;

import com.tonic.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientWatchdog {
    private static final int MISSED_TICKS_THRESHOLD = 5;
    private static final long CHECK_INTERVAL_MS = 600L;
    private static final int MAX_RECOVERY_ATTEMPTS = 3;

    private static ClientWatchdog INSTANCE;

    private volatile int lastTickCount;
    private final AtomicInteger missedTickChecks;
    private volatile boolean loggedIn;
    private final AtomicInteger recoveryAttempts;
    private final AtomicBoolean enabled;
    private final AtomicBoolean desyncDetected;
    private final ScheduledExecutorService scheduler;
    private final List<WatchdogListener> listeners;

    private ClientWatchdog() {
        this.lastTickCount = 0;
        this.missedTickChecks = new AtomicInteger(0);
        this.loggedIn = false;
        this.recoveryAttempts = new AtomicInteger(0);
        this.enabled = new AtomicBoolean(true);
        this.desyncDetected = new AtomicBoolean(false);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VitaLite-Watchdog");
            t.setDaemon(true);
            return t;
        });
        this.listeners = new ArrayList<>();
    }

    public static synchronized void init() {
        if (INSTANCE != null) {
            return;
        }
        INSTANCE = new ClientWatchdog();
        INSTANCE.startMonitoring();
        Logger.info("[Watchdog] Client watchdog initialized");
    }

    public static void shutdown() {
        if (INSTANCE != null) {
            INSTANCE.enabled.set(false);
            INSTANCE.scheduler.shutdownNow();
            INSTANCE = null;
            Logger.info("[Watchdog] Client watchdog shutdown");
        }
    }

    public static void recordHeartbeat(int tickCount) {
        if (INSTANCE != null) {
            INSTANCE.lastTickCount = tickCount;
            INSTANCE.missedTickChecks.set(0);
            if (INSTANCE.desyncDetected.compareAndSet(true, false)) {
                Logger.info("[Watchdog] Client thread recovered - ticks resumed");
                INSTANCE.recoveryAttempts.set(0);
                TrackedInvoke.triggerResetHook();
            }
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

    public static void addListener(WatchdogListener listener) {
        if (INSTANCE != null) {
            INSTANCE.listeners.add(listener);
        }
    }

    public static void removeListener(WatchdogListener listener) {
        if (INSTANCE != null) {
            INSTANCE.listeners.remove(listener);
        }
    }

    public static boolean isDesyncDetected() {
        return INSTANCE != null && INSTANCE.desyncDetected.get();
    }

    public static int getMissedTickChecks() {
        if (INSTANCE == null) {
            return 0;
        }
        return INSTANCE.missedTickChecks.get();
    }

    public static void setEnabled(boolean enabled) {
        if (INSTANCE != null) {
            INSTANCE.enabled.set(enabled);
        }
    }

    private void startMonitoring() {
        scheduler.scheduleAtFixedRate(this::checkHealth, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void checkHealth() {
        if (!enabled.get() || !loggedIn) {
            return;
        }

        try {
            int currentTick = lastTickCount;
            int missed = missedTickChecks.get();
            if (currentTick == lastTickCount && missed > 0) {
                int newMissed = missedTickChecks.incrementAndGet();
                if (newMissed >= MISSED_TICKS_THRESHOLD) {
                    onDesync(newMissed);
                }
            } else {
                missedTickChecks.set(1);
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

        String dump = ThreadDiagnostics.generateFullDump();
        Logger.error("[Watchdog] Thread dump:\n" + dump);
        ThreadDiagnostics.dumpToFile(dump);

        for (WatchdogListener listener : listeners) {
            try {
                listener.onDesyncDetected(missedChecks * 600L);
            } catch (Throwable t) {
                Logger.error("[Watchdog] Error notifying listener: " + t.getMessage());
            }
        }

        int attempts = recoveryAttempts.incrementAndGet();
        if (attempts <= MAX_RECOVERY_ATTEMPTS) {
            Logger.info("[Watchdog] Attempting recovery (attempt " + attempts + "/" + MAX_RECOVERY_ATTEMPTS + ")");
            RecoveryManager.attemptRecovery(RecoveryManager.RecoveryStrategy.CANCEL_PENDING);
        } else {
            Logger.error("[Watchdog] Max recovery attempts exceeded - manual intervention required");
            for (WatchdogListener listener : listeners) {
                try {
                    listener.onRecoveryFailed();
                } catch (Throwable t) {
                    Logger.error("[Watchdog] Error notifying listener of failure: " + t.getMessage());
                }
            }
        }
    }

    public interface WatchdogListener {
        void onDesyncDetected(long elapsedMs);
        void onRecoveryFailed();
    }
}
