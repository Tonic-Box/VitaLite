package com.tonic.services.watchdog;

import com.tonic.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RecoveryManager {
    private static final AtomicBoolean recoveryInProgress = new AtomicBoolean(false);
    private static final AtomicInteger totalRecoveries = new AtomicInteger(0);

    public enum RecoveryStrategy {
        CANCEL_PENDING,
        RESET_APPLET,
        RELOAD_CLIENT
    }

    public static boolean attemptRecovery(RecoveryStrategy strategy) {
        if (!recoveryInProgress.compareAndSet(false, true)) {
            Logger.warn("[Recovery] Recovery already in progress, skipping");
            return false;
        }

        try {
            Logger.info("[Recovery] Attempting recovery strategy: " + strategy);
            boolean success;
            switch (strategy) {
                case CANCEL_PENDING:
                    success = cancelAllPendingInvokes();
                    break;
                case RESET_APPLET:
                    success = resetGameApplet();
                    break;
                case RELOAD_CLIENT:
                    success = reloadClient();
                    break;
                default:
                    success = false;
            }

            if (success) {
                totalRecoveries.incrementAndGet();
                Logger.info("[Recovery] Recovery successful using strategy: " + strategy);
            } else {
                Logger.error("[Recovery] Recovery failed using strategy: " + strategy);
            }

            return success;
        } catch (Throwable t) {
            Logger.error("[Recovery] Exception during recovery: " + t.getMessage());
            return false;
        } finally {
            recoveryInProgress.set(false);
        }
    }

    public static boolean escalateRecovery() {
        if (attemptRecovery(RecoveryStrategy.CANCEL_PENDING)) {
            return true;
        }
        if (attemptRecovery(RecoveryStrategy.RESET_APPLET)) {
            return true;
        }
        return attemptRecovery(RecoveryStrategy.RELOAD_CLIENT);
    }

    private static boolean cancelAllPendingInvokes() {
        try {
            TrackedInvoke.triggerCancellationHook();
            Logger.info("[Recovery] Signaled cancellation");
            return true;
        } catch (Throwable t) {
            Logger.error("[Recovery] Failed to signal cancellation: " + t.getMessage());
            return false;
        }
    }

    private static boolean resetGameApplet() {
        try {
            return ClientReloader.resetApplet();
        } catch (Throwable t) {
            Logger.error("[Recovery] Failed to reset applet: " + t.getMessage());
            return false;
        }
    }

    private static boolean reloadClient() {
        try {
            return ClientReloader.reloadClient();
        } catch (Throwable t) {
            Logger.error("[Recovery] Failed to reload client: " + t.getMessage());
            return false;
        }
    }

    public static boolean isRecoveryInProgress() {
        return recoveryInProgress.get();
    }

    public static int getTotalRecoveries() {
        return totalRecoveries.get();
    }
}
