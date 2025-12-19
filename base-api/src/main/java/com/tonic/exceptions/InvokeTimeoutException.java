package com.tonic.exceptions;

public class InvokeTimeoutException extends RuntimeException {
    private final String callerStack;
    private final long timeoutMs;
    private final String threadDump;

    public InvokeTimeoutException(String callerStack, long timeoutMs, String threadDump) {
        super("Invoke timeout after " + timeoutMs + "ms");
        this.callerStack = callerStack;
        this.timeoutMs = timeoutMs;
        this.threadDump = threadDump;
    }

    public String getCallerStack() {
        return callerStack;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public String getThreadDump() {
        return threadDump;
    }
}
