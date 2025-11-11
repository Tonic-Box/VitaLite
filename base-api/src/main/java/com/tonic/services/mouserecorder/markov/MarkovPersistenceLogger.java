package com.tonic.services.mouserecorder.markov;

import com.tonic.Static;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Validation logger for Markov persistence operations.
 * Writes detailed logs to a file in VITA_DIR for debugging persistence issues.
 */
public class MarkovPersistenceLogger
{
    private static final Path LOG_FILE = Static.VITA_DIR.resolve("markov-persistence.log");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int MAX_LOG_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    /**
     * Logs a validation event to the persistence log file.
     *
     * @param level   Log level (INFO, WARN, ERROR)
     * @param message Log message
     */
    public static void log(String level, String message)
    {
        try
        {
            ensureLogFileExists();
            rotateLogIfNeeded();

            String timestamp = LocalDateTime.now().format(TIME_FORMAT);
            String logEntry = String.format("[%s] [%s] %s%n", timestamp, level, message);

            Files.writeString(LOG_FILE, logEntry,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException e)
        {
            System.err.println("Failed to write to persistence log: " + e.getMessage());
        }
    }

    /**
     * Logs detailed information about a save operation.
     */
    public static void logSave(Path filePath, MarkovChainData chain, long durationMs)
    {
        String message = String.format(
            "SAVE | File: %s | Transitions: %d | Samples: %d | States: %d | Movements: %d | Duration: %dms | Size: %.2f KB",
            filePath.getFileName(),
            chain.getTotalTransitions(),
            chain.getTotalSamples(),
            chain.getStateCount(),
            chain.getUniqueMovementCount(),
            durationMs,
            getFileSizeKB(filePath)
        );
        log("INFO", message);
    }

    /**
     * Logs detailed information about a successful load operation.
     */
    public static void logLoad(Path filePath, MarkovChainData chain, long durationMs)
    {
        String message = String.format(
            "LOAD SUCCESS | File: %s | Transitions: %d | Samples: %d | States: %d | Movements: %d | Duration: %dms | Size: %.2f KB",
            filePath.getFileName(),
            chain.getTotalTransitions(),
            chain.getTotalSamples(),
            chain.getStateCount(),
            chain.getUniqueMovementCount(),
            durationMs,
            getFileSizeKB(filePath)
        );
        log("INFO", message);
    }

    /**
     * Logs a load failure with detailed error information.
     */
    public static void logLoadFailure(Path filePath, String reason, Exception exception)
    {
        String message = String.format(
            "LOAD FAILURE | File: %s | Reason: %s | Error: %s",
            filePath.getFileName(),
            reason,
            exception != null ? exception.getMessage() : "None"
        );
        log("ERROR", message);

        if (exception != null && exception.getStackTrace().length > 0)
        {
            log("ERROR", "Stack trace: " + exception.getStackTrace()[0].toString());
        }
    }

    /**
     * Logs validation of binary format compatibility.
     */
    public static void logFormatValidation(Path filePath, int expectedBinSize, int actualBinSize,
                                          int expectedTimeBinSize, int actualTimeBinSize)
    {
        boolean compatible = (expectedBinSize == actualBinSize) && (expectedTimeBinSize == actualTimeBinSize);
        String level = compatible ? "INFO" : "WARN";

        String message = String.format(
            "FORMAT VALIDATION | File: %s | Compatible: %s | Expected bins: %d/%d | Actual bins: %d/%d",
            filePath.getFileName(),
            compatible ? "YES" : "NO",
            expectedBinSize, expectedTimeBinSize,
            actualBinSize, actualTimeBinSize
        );
        log(level, message);
    }

    /**
     * Logs the start of persistence operations.
     */
    public static void logPersistenceStart(Path filePath, int saveIntervalSeconds)
    {
        String message = String.format(
            "PERSISTENCE START | File: %s | Save interval: %ds",
            filePath.getFileName(),
            saveIntervalSeconds
        );
        log("INFO", message);
    }

    /**
     * Logs the stop of persistence operations.
     */
    public static void logPersistenceStop(long totalSaves, long totalTransitionsSaved)
    {
        String message = String.format(
            "PERSISTENCE STOP | Total saves: %d | Total transitions saved: %d",
            totalSaves,
            totalTransitionsSaved
        );
        log("INFO", message);
    }

    /**
     * Ensures the log file and its parent directories exist.
     */
    private static void ensureLogFileExists() throws IOException
    {
        Files.createDirectories(LOG_FILE.getParent());
    }

    /**
     * Rotates the log file if it exceeds the maximum size.
     */
    private static void rotateLogIfNeeded() throws IOException
    {
        if (Files.exists(LOG_FILE) && Files.size(LOG_FILE) > MAX_LOG_SIZE_BYTES)
        {
            Path rotated = Static.VITA_DIR.resolve("markov-persistence.log.old");
            Files.deleteIfExists(rotated);
            Files.move(LOG_FILE, rotated);
        }
    }

    /**
     * Gets file size in KB, returns 0 if file doesn't exist.
     */
    private static double getFileSizeKB(Path filePath)
    {
        try
        {
            return Files.exists(filePath) ? Files.size(filePath) / 1024.0 : 0.0;
        }
        catch (IOException e)
        {
            return 0.0;
        }
    }
}
