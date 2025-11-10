package com.tonic.services.mouserecorder.markov;

import com.tonic.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages incremental persistence of Markov chain data to disk.
 * Batches writes to minimize I/O impact while ensuring data is regularly saved.
 * <p>
 * Features:
 * - Batched writes (only saves when there are new transitions)
 * - Configurable save interval
 * - Background thread for I/O
 * - Minimal performance impact
 * - Automatic recovery on startup
 */
public class IncrementalPersistenceManager
{
    private final MarkovChainData chainData;
    private final Path saveFilePath;
    private final int saveIntervalSeconds;

    private final AtomicBoolean running;
    private final AtomicLong lastSavedTransitionCount;
    private final AtomicLong lastSaveTime;
    private ScheduledExecutorService saveExecutor;

    /**
     * Creates a persistence manager.
     *
     * @param chainData           The chain data to persist
     * @param saveFilePath        Path to save file
     * @param saveIntervalSeconds Seconds between save attempts (only saves if dirty)
     */
    public IncrementalPersistenceManager(MarkovChainData chainData, Path saveFilePath, int saveIntervalSeconds)
    {
        this.chainData = chainData;
        this.saveFilePath = saveFilePath;
        this.saveIntervalSeconds = saveIntervalSeconds;

        this.running = new AtomicBoolean(false);
        this.lastSavedTransitionCount = new AtomicLong(0);
        this.lastSaveTime = new AtomicLong(0);
    }

    /**
     * Loads existing data from disk if available.
     * This can be called independently of starting the auto-save scheduler.
     */
    public synchronized void loadExistingData()
    {
        if (BinaryMarkovPersistence.isValidFile(saveFilePath))
        {
            try
            {
                MarkovChainData loaded = BinaryMarkovPersistence.loadBinary(saveFilePath);
                if (loaded.getBinSize() != chainData.getBinSize() ||
                    loaded.getTimeBinSize() != chainData.getTimeBinSize())
                {
                    Logger.warn(String.format(
                        "Existing data has incompatible bin sizes (file: %d/%d, current: %d/%d). " +
                        "Starting fresh training. Old file will be overwritten.",
                        loaded.getBinSize(), loaded.getTimeBinSize(),
                        chainData.getBinSize(), chainData.getTimeBinSize()));
                }
                else
                {
                    chainData.merge(loaded);
                    lastSavedTransitionCount.set(chainData.getTotalTransitions());

                    Logger.info(String.format("Loaded existing Markov data: %s (%.2f KB)",
                        loaded.getStatistics(),
                        BinaryMarkovPersistence.getFileSize(saveFilePath) / 1024.0));
                }
            }
            catch (IOException e)
            {
                Logger.error("Failed to load existing Markov data: " + e.getMessage());
            }
        }
    }

    /**
     * Starts the incremental persistence system.
     * Starts the auto-save scheduler (data loading should be done separately via loadExistingData()).
     */
    public synchronized void start()
    {
        if (running.get())
        {
            Logger.warn("IncrementalPersistenceManager already running");
            return;
        }

        running.set(true);
        saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MarkovPersistence");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);  // Low priority to avoid impacting gameplay
            return t;
        });

        saveExecutor.scheduleAtFixedRate(
            this::trySave,
            saveIntervalSeconds,
            saveIntervalSeconds,
            TimeUnit.SECONDS
        );

        Logger.info("Markov persistence started (saves every " + saveIntervalSeconds + "s to " + saveFilePath + ")");
    }

    /**
     * Stops the persistence system and performs a final save.
     */
    public synchronized void stop()
    {
        if (!running.get())
        {
            return;
        }

        running.set(false);

        if (saveExecutor != null)
        {
            saveExecutor.shutdown();
            try
            {
                saveExecutor.awaitTermination(5, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            saveExecutor = null;
        }

        forceSave();

        Logger.info("Markov persistence stopped");
    }

    /**
     * Attempts to save if there are new transitions.
     * Only performs I/O if data has changed since last save.
     * Runs every 30s - always saves if ANY unsaved data exists.
     */
    private void trySave()
    {
        try
        {
            long currentTransitions = chainData.getTotalTransitions();
            long savedTransitions = lastSavedTransitionCount.get();

            if (currentTransitions > savedTransitions)
            {
                long startTime = System.currentTimeMillis();

                BinaryMarkovPersistence.saveBinary(chainData, saveFilePath);

                long elapsed = System.currentTimeMillis() - startTime;
                long newTransitions = currentTransitions - savedTransitions;

                lastSavedTransitionCount.set(currentTransitions);
                lastSaveTime.set(System.currentTimeMillis());

                Logger.info(String.format("Auto-saved %d new transitions in %dms (%.2f KB)",
                    newTransitions, elapsed,
                    BinaryMarkovPersistence.getFileSize(saveFilePath) / 1024.0));
            }
        }
        catch (IOException e)
        {
            Logger.error("Failed to save Markov data: " + e.getMessage());
        }
    }

    /**
     * Forces an immediate save regardless of dirty state.
     */
    public void forceSave()
    {
        try
        {
            long startTime = System.currentTimeMillis();
            BinaryMarkovPersistence.saveBinary(chainData, saveFilePath);
            long elapsed = System.currentTimeMillis() - startTime;

            lastSavedTransitionCount.set(chainData.getTotalTransitions());
            lastSaveTime.set(System.currentTimeMillis());

            Logger.info(String.format("Force saved Markov data in %dms (%.2f KB)",
                elapsed, BinaryMarkovPersistence.getFileSize(saveFilePath) / 1024.0));
        }
        catch (IOException e)
        {
            Logger.error("Failed to force save Markov data: " + e.getMessage());
        }
    }

    /**
     * Returns the number of unsaved transitions.
     */
    public long getUnsavedTransitionCount()
    {
        return chainData.getTotalTransitions() - lastSavedTransitionCount.get();
    }

    /**
     * Checks if there are unsaved changes.
     */
    public boolean isDirty()
    {
        return getUnsavedTransitionCount() > 0;
    }

    /**
     * Returns statistics about persistence.
     */
    public PersistenceStatistics getStatistics()
    {
        return new PersistenceStatistics(
            saveFilePath,
            lastSavedTransitionCount.get(),
            getUnsavedTransitionCount(),
            lastSaveTime.get(),
            running.get()
        );
    }

    /**
     * Statistics about persistence status.
     */
    public static class PersistenceStatistics
    {
        public final Path filePath;
        public final long savedTransitions;
        public final long unsavedTransitions;
        public final long lastSaveTime;
        public final boolean running;

        private PersistenceStatistics(Path filePath, long savedTransitions,
                                      long unsavedTransitions, long lastSaveTime, boolean running)
        {
            this.filePath = filePath;
            this.savedTransitions = savedTransitions;
            this.unsavedTransitions = unsavedTransitions;
            this.lastSaveTime = lastSaveTime;
            this.running = running;
        }

        public long getTimeSinceLastSave()
        {
            return lastSaveTime > 0 ? System.currentTimeMillis() - lastSaveTime : -1;
        }

        @Override
        public String toString()
        {
            return String.format("Persistence{saved=%d, unsaved=%d, lastSave=%ds ago, running=%b}",
                savedTransitions, unsavedTransitions,
                getTimeSinceLastSave() / 1000, running);
        }
    }
}
