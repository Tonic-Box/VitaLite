package com.tonic.services.watchdog;

import com.tonic.Logger;
import com.tonic.Static;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ThreadDiagnostics {
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Path DUMPS_DIR = Static.VITA_DIR.resolve("dumps");

    public static String generateFullDump() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== THREAD DUMP ===\n");
        sb.append("Timestamp: ").append(Instant.now()).append("\n\n");

        Map<Thread, StackTraceElement[]> allStacks = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStacks.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stack = entry.getValue();
            sb.append(formatThread(thread, stack));
            sb.append("\n");
        }

        return sb.toString();
    }

    public static String dumpClientThread() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CLIENT THREAD DUMP ===\n");
        sb.append("Timestamp: ").append(Instant.now()).append("\n\n");

        Map<Thread, StackTraceElement[]> allStacks = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStacks.entrySet()) {
            Thread thread = entry.getKey();
            if (thread.getName().toLowerCase().contains("client")) {
                sb.append(formatThread(thread, entry.getValue()));
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public static void dumpToFile(String dump) {
        try {
            Files.createDirectories(DUMPS_DIR);
            String filename = "thread_dump_" + LocalDateTime.now().format(FILE_FORMAT) + ".txt";
            Path dumpPath = DUMPS_DIR.resolve(filename);
            Files.writeString(dumpPath, dump);
            Logger.info("[Watchdog] Thread dump written to: " + dumpPath);
        } catch (IOException e) {
            Logger.error("[Watchdog] Failed to write thread dump: " + e.getMessage());
        }
    }

    public static String getCaller() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stack.length; i++) {
            StackTraceElement element = stack[i];
            String className = element.getClassName();
            if (!className.startsWith("com.tonic.Static") &&
                !className.startsWith("com.tonic.services.watchdog")) {
                return element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber();
            }
        }
        return "unknown";
    }

    private static String formatThread(Thread thread, StackTraceElement[] stack) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Thread: %s (id=%d, state=%s)\n",
                thread.getName(), thread.getId(), thread.getState()));
        sb.append(String.format("  Priority: %d, Daemon: %b\n",
                thread.getPriority(), thread.isDaemon()));

        for (int i = 0; i < stack.length; i++) {
            StackTraceElement element = stack[i];
            sb.append(String.format("  [%d] %s.%s(%s:%d)\n",
                    i,
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName() != null ? element.getFileName() : "Unknown",
                    element.getLineNumber()));
        }

        return sb.toString();
    }
}
