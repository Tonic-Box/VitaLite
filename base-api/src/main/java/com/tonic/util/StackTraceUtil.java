package com.tonic.util;

public class StackTraceUtil
{
    /**
     * Prints the full stack trace showing how this method was called
     */
    public static String getStackTrace() {
        return getStackTrace("Stack Trace");
    }

    /**
     * Prints the full stack trace with a custom label
     */
    public static String getStackTrace(String label) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== ").append(label).append(" ==========\n");
        StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();

        // Skip the first element (this method itself)
        for (int i = 1; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            sb.append(String.format("  [%d] %s.%s(%s:%d)%n",
                    i - 1,
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber()
            ));
        }
        sb.append("=====================================\n");
        return sb.toString();
    }

    /**
     * Prints stack trace with depth limit
     */
    public static String getStackTrace(String label, int maxDepth) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== ").append(label).append(" ==========\n");
        StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();

        int limit = Math.min(maxDepth + 1, stackTrace.length);
        for (int i = 1; i < limit; i++) {
            StackTraceElement element = stackTrace[i];
            sb.append(String.format("  [%d] %s.%s(%s:%d)%n",
                    i - 1,
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber()
            ));
        }
        if (stackTrace.length > limit) {
            sb.append("  ... ").append(stackTrace.length - limit).append(" more\n");
        }
        sb.append("=====================================\n");
        return sb.toString();
    }

    /**
     * Returns the caller's method name and line number
     */
    public static String getCaller() {
        StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
        if (stackTrace.length > 2) {
            StackTraceElement caller = stackTrace[2];
            return String.format("%s.%s:%d",
                    caller.getClassName(),
                    caller.getMethodName(),
                    caller.getLineNumber()
            );
        }
        return "Unknown caller";
    }

    /**
     * Prints a debug message with caller info
     */
    public static void debug(String message) {
        StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
        if (stackTrace.length > 1) {
            StackTraceElement caller = stackTrace[1];
            System.out.printf("[DEBUG] %s.%s:%d - %s%n",
                    caller.getClassName(),
                    caller.getMethodName(),
                    caller.getLineNumber(),
                    message
            );
        }
    }
}
