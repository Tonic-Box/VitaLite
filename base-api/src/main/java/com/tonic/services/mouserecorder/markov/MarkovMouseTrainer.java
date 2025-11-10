package com.tonic.services.mouserecorder.markov;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.mouserecorder.MouseDataPoint;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Integrated service that manages mouse recording, training, and persistence.
 * Provides a high-level API for:
 * - Recording real gameplay mouse movements
 * - Building and refining Markov chain models
 * - Auto-saving trained models (real-time to markov.dat)
 * - Auto-loading existing models on startup
 * - Providing generators trained on real data
 * <p>
 * This is the main entry point for the Markov-based mouse generation system.
 * <p>
 * By default, automatically persists to {@code Static.VITA_DIR/markov.dat} in real-time.
 */
public class MarkovMouseTrainer
{
    private static final String DEFAULT_SAVE_FILE = "markov.dat";
    private static final int DEFAULT_SAVE_INTERVAL_SECONDS = 30;
    @Getter
    private final MouseRecordingService recordingService;
    private final MarkovChainBuilder chainBuilder;
    @Getter
    private MarkovChainData chainData;

    private final AtomicBoolean autoTraining;
    private ScheduledExecutorService trainingExecutor;
    private final int trainingIntervalSeconds;
    @Getter
    private IncrementalPersistenceManager persistenceManager;

    /**
     * Creates a new trainer with specified configuration.
     *
     * @param mouseXSupplier          Supplier for current mouse X
     * @param mouseYSupplier          Supplier for current mouse Y
     * @param samplingRateMs          Recording sampling rate in milliseconds
     * @param binSize                 Markov chain spatial bin size (pixels)
     * @param timeBinSize             Markov chain time bin size (milliseconds)
     * @param trainingIntervalSeconds Seconds between automatic training updates
     * @param autoPersistence         If true, automatically saves to markov.dat in real-time
     */
    public MarkovMouseTrainer(Supplier<Integer> mouseXSupplier,
                              Supplier<Integer> mouseYSupplier,
                              int samplingRateMs,
                              int binSize,
                              int timeBinSize,
                              int trainingIntervalSeconds,
                              boolean autoPersistence)
    {
        this.recordingService = new MouseRecordingService(mouseXSupplier, mouseYSupplier, samplingRateMs, 10000);
        this.chainBuilder = new MarkovChainBuilder(binSize, timeBinSize, true);
        this.chainData = new MarkovChainData(binSize, timeBinSize);
        this.autoTraining = new AtomicBoolean(false);
        this.trainingIntervalSeconds = trainingIntervalSeconds;
        if (autoPersistence)
        {
            Path saveFile = Static.VITA_DIR.resolve(DEFAULT_SAVE_FILE);
            this.persistenceManager = new IncrementalPersistenceManager(
                chainData, saveFile, DEFAULT_SAVE_INTERVAL_SECONDS
            );
            this.persistenceManager.loadExistingData();
        }
    }

    /**
     * Creates a trainer with default settings and auto-persistence enabled.
     *
     * @param mouseXSupplier Supplier for mouse X coordinate
     * @param mouseYSupplier Supplier for mouse Y coordinate
     */
    public MarkovMouseTrainer(Supplier<Integer> mouseXSupplier, Supplier<Integer> mouseYSupplier)
    {
        this(mouseXSupplier, mouseYSupplier, 50, 1, 20, 30, true);
    }

    /**
     * Starts recording and training.
     * This will:
     * - Load existing model from markov.dat if it exists (auto-resume)
     * - Begin capturing real mouse movements
     * - Periodically train the model
     * - Auto-save to markov.dat in real-time
     */
    public synchronized void start()
    {
        if (autoTraining.get())
        {
            Logger.warn("MarkovMouseTrainer already running");
            return;
        }
        if (persistenceManager != null)
        {
            persistenceManager.start();
        }
        recordingService.startRecording();
        autoTraining.set(true);
        trainingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MarkovTrainer");
            t.setDaemon(true);
            return t;
        });

        trainingExecutor.scheduleAtFixedRate(
            this::performTraining,
            trainingIntervalSeconds,
            trainingIntervalSeconds,
            TimeUnit.SECONDS
        );

        Logger.info("MarkovMouseTrainer started (training every " + trainingIntervalSeconds + "s)");
    }

    /**
     * Stops recording and training.
     * Performs final training and save before stopping.
     */
    public synchronized void stop()
    {
        if (!autoTraining.get())
        {
            return;
        }
        recordingService.stopRecording();
        autoTraining.set(false);
        if (trainingExecutor != null)
        {
            trainingExecutor.shutdown();
            try
            {
                trainingExecutor.awaitTermination(1, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            trainingExecutor = null;
        }
        performTraining();
        if (persistenceManager != null)
        {
            persistenceManager.stop();
        }

        Logger.info("MarkovMouseTrainer stopped");
    }

    /**
     * Performs a training iteration using newly recorded samples.
     */
    private void performTraining()
    {
        try
        {
            List<MouseDataPoint> newSamples = recordingService.pollNewSamples();

            if (newSamples.isEmpty())
            {
                return;
            }
            chainBuilder.train(chainData, newSamples);

            Logger.info(String.format("Trained with %d samples. Chain: %s",
                newSamples.size(), chainData.getStatistics()));
        }
        catch (Exception e)
        {
            Logger.error("Error during training: " + e.getMessage());
        }
    }

    /**
     * Saves the trained model to a file.
     *
     * @param file File to save to
     * @throws IOException If save fails
     */
    public void save(File file) throws IOException
    {
        MarkovChainPersistence.save(chainData, file);
    }

    /**
     * Loads a previously trained model from a file.
     *
     * @param file File to load from
     * @throws IOException If load fails
     */
    public void load(File file) throws IOException
    {
        this.chainData = MarkovChainPersistence.load(file);
        Logger.info("Loaded Markov model: " + chainData.getStatistics());
    }

    /**
     * Creates a generator using the currently trained model.
     *
     * @return MarkovMouseGenerator ready to use
     * @throws IllegalStateException if model has no training data
     */
    public MarkovMouseGenerator createGenerator()
    {
        if (chainData.getTotalTransitions() == 0)
        {
            throw new IllegalStateException("No training data available. Record some mouse movements first.");
        }

        return new MarkovMouseGenerator(chainData);
    }

    /**
     * Checks if currently training.
     */
    public boolean isRunning()
    {
        return autoTraining.get();
    }

    /**
     * Returns comprehensive statistics about the training session.
     */
    public TrainingStatistics getStatistics()
    {
        IncrementalPersistenceManager.PersistenceStatistics persistence =
            persistenceManager != null ? persistenceManager.getStatistics() : null;

        return new TrainingStatistics(
            recordingService.getStatistics(),
            chainData.getStatistics(),
            persistence,
            autoTraining.get()
        );
    }

    /**
     * Forces an immediate save to disk.
     */
    public void forceSave()
    {
        if (persistenceManager != null)
        {
            persistenceManager.forceSave();
        }
        else
        {
            Logger.warn("Auto-persistence is disabled, cannot force save");
        }
    }

    /**
     * Clears all training data from memory and deletes the saved file on disk.
     * This resets the model to a fresh state.
     */
    public synchronized void clearData()
    {
        boolean wasRunning = isRunning();
        if (wasRunning)
        {
            stop();
        }

        if (persistenceManager != null)
        {
            Path saveFile = Static.VITA_DIR.resolve(DEFAULT_SAVE_FILE);
            try
            {
                java.nio.file.Files.deleteIfExists(saveFile);
                Logger.info("Deleted existing Markov data file: " + saveFile);
            }
            catch (Exception e)
            {
                Logger.error("Failed to delete Markov data file: " + e.getMessage());
            }
        }

        this.chainData = new MarkovChainData(chainData.getBinSize(), chainData.getTimeBinSize());
        if (persistenceManager != null)
        {
            Path saveFile = Static.VITA_DIR.resolve(DEFAULT_SAVE_FILE);
            this.persistenceManager = new IncrementalPersistenceManager(
                chainData, saveFile, DEFAULT_SAVE_INTERVAL_SECONDS
            );
        }

        Logger.info("Markov training data cleared");
        if (wasRunning)
        {
            start();
        }
    }

    /**
     * Combined statistics for recording and training.
     */
    public static class TrainingStatistics
    {
        public final MouseRecordingService.RecordingStatistics recording;
        public final MarkovChainData.ChainStatistics chain;
        public final IncrementalPersistenceManager.PersistenceStatistics persistence;
        public final boolean isRunning;

        private TrainingStatistics(MouseRecordingService.RecordingStatistics recording,
                                   MarkovChainData.ChainStatistics chain,
                                   IncrementalPersistenceManager.PersistenceStatistics persistence,
                                   boolean isRunning)
        {
            this.recording = recording;
            this.chain = chain;
            this.persistence = persistence;
            this.isRunning = isRunning;
        }

        @Override
        public String toString()
        {
            String persistStr = persistence != null ? ", " + persistence : "";
            return String.format("Training{running=%b, %s, %s%s}",
                isRunning, recording, chain, persistStr);
        }
    }
}
