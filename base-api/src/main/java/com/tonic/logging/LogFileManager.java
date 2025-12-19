package com.tonic.logging;

import com.tonic.Static;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages JVM-wide logging to daily rotating log files.
 * Redirects System.out and System.err to both console and file.
 */
public class LogFileManager {
    private static final Path LOGS_DIR = Static.VITA_DIR.resolve("logs");
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static PrintStream originalOut;
    private static PrintStream originalErr;
    private static FileOutputStream fileStream;
    private static LocalDate currentDate;
    private static int rotationIndex = 0;
    private static ScheduledExecutorService flushScheduler;
    private static volatile boolean initialized = false;

    /**
     * Initializes logging redirection. Call once at JVM startup before other initialization.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            Files.createDirectories(LOGS_DIR);

            originalOut = System.out;
            originalErr = System.err;

            openLogFile();

            TimestampedOutputStream timestampedFile = new TimestampedOutputStream(new BufferedOutputStream(fileStream, 8192));
            TeeOutputStream teeOut = new TeeOutputStream(originalOut, timestampedFile);
            TeeOutputStream teeErr = new TeeOutputStream(originalErr, timestampedFile);

            System.setOut(new PrintStream(teeOut, false));
            System.setErr(new PrintStream(teeErr, false));

            startFlushScheduler();

            initialized = true;

            Runtime.getRuntime().addShutdownHook(new Thread(LogFileManager::shutdown));

        } catch (IOException e) {
            System.err.println("Failed to initialize log file manager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void openLogFile() throws IOException {
        if (fileStream != null) {
            try {
                fileStream.close();
            } catch (IOException ignored) {
            }
        }

        currentDate = LocalDate.now();
        Path logPath = getLogFilePath();
        fileStream = new FileOutputStream(logPath.toFile(), true);
    }

    private static Path getLogFilePath() {
        String filename = "vitalite-" + currentDate.format(DATE_FORMATTER);
        if (rotationIndex > 0) {
            filename += "-" + rotationIndex;
        }
        filename += ".log";
        return LOGS_DIR.resolve(filename);
    }

    private static void startFlushScheduler() {
        flushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LogFileManager-Flush");
            t.setDaemon(true);
            return t;
        });

        flushScheduler.scheduleAtFixedRate(LogFileManager::periodicMaintenance, 1, 1, TimeUnit.SECONDS);
    }

    private static synchronized void periodicMaintenance() {
        try {
            System.out.flush();
            System.err.flush();

            checkRotation();
        } catch (Exception e) {
            originalErr.println("Log maintenance error: " + e.getMessage());
        }
    }

    private static void checkRotation() throws IOException {
        LocalDate now = LocalDate.now();
        if (!now.equals(currentDate)) {
            rotationIndex = 0;
            rotateFile();
            return;
        }

        if (fileStream != null && fileStream.getChannel().size() > MAX_FILE_SIZE_BYTES) {
            rotationIndex++;
            rotateFile();
        }
    }

    private static void rotateFile() throws IOException {
        System.out.flush();
        System.err.flush();

        openLogFile();

        TimestampedOutputStream timestampedFile = new TimestampedOutputStream(new BufferedOutputStream(fileStream, 8192));
        TeeOutputStream teeOut = new TeeOutputStream(originalOut, timestampedFile);
        TeeOutputStream teeErr = new TeeOutputStream(originalErr, timestampedFile);

        System.setOut(new PrintStream(teeOut, false));
        System.setErr(new PrintStream(teeErr, false));
    }

    /**
     * Shuts down the logging system and restores original streams.
     */
    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }

        try {
            if (flushScheduler != null) {
                flushScheduler.shutdown();
            }

            System.out.flush();
            System.err.flush();

            if (originalOut != null) {
                System.setOut(originalOut);
            }
            if (originalErr != null) {
                System.setErr(originalErr);
            }

            if (fileStream != null) {
                fileStream.close();
            }
        } catch (IOException e) {
            if (originalErr != null) {
                originalErr.println("Error during log shutdown: " + e.getMessage());
            }
        }

        initialized = false;
    }

    /**
     * Returns the current log file path.
     */
    public static Path getCurrentLogFile() {
        return getLogFilePath();
    }

    /**
     * Returns the logs directory path.
     */
    public static Path getLogsDirectory() {
        return LOGS_DIR;
    }
}
