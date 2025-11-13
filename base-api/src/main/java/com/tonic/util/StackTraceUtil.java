package com.tonic.util;

public class StackTraceUtil
{
    /**
     * Prints the full stack trace showing how this method was called
     */
    public static void printStackTrace() {
        printStackTrace("Stack Trace");
    }

    /**
     * Prints the full stack trace with a custom label
     */
    public static void printStackTrace(String label) {
        System.out.println("\n========== " + label + " ==========");
        StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();

        // Skip the first element (this method itself)
        for (int i = 1; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            System.out.printf("  [%d] %s.%s(%s:%d)%n",
                    i - 1,
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber()
            );
        }
        System.out.println("=====================================\n");
    }

    /**
     * Prints stack trace with depth limit
     */
    public static void printStackTrace(String label, int maxDepth) {
        System.out.println("\n========== " + label + " ==========");
        StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();

        int limit = Math.min(maxDepth + 1, stackTrace.length);
        for (int i = 1; i < limit; i++) {
            StackTraceElement element = stackTrace[i];
            System.out.printf("  [%d] %s.%s(%s:%d)%n",
                    i - 1,
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber()
            );
        }
        if (stackTrace.length > limit) {
            System.out.println("  ... " + (stackTrace.length - limit) + " more");
        }
        System.out.println("=====================================\n");
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
