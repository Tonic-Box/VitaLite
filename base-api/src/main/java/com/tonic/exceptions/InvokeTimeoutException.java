package com.tonic.exceptions;

public class InvokeTimeoutException extends RuntimeException {
    private final String callerInfo;
    private final long timeoutMs;
    private final String threadDump;

    public InvokeTimeoutException(String callerInfo, long timeoutMs, String threadDump) {
        super("Invoke timeout after " + timeoutMs + "ms from " + callerInfo);
        this.callerInfo = callerInfo;
        this.timeoutMs = timeoutMs;
        this.threadDump = threadDump;
    }

    public String getCallerInfo() {
        return callerInfo;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public String getThreadDump() {
        return threadDump;
    }
}
