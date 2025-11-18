package com.tonic.services.profiler.sampling;

/**
 * Immutable snapshot of a thread's stack trace at a point in time
 */
public class StackSample {
    public final long timestamp;
    public final long threadId;
    public final String threadName;
    public final Thread.State threadState;
    public final StackTraceElement[] stackTrace;
    public final long threadCpuTime;
    public final long threadUserTime;
    public final long blockedTime;
    public final long waitedTime;

    public StackSample(
        long timestamp,
        long threadId,
        String threadName,
        Thread.State threadState,
        StackTraceElement[] stackTrace,
        long threadCpuTime,
        long threadUserTime,
        long blockedTime,
        long waitedTime
    ) {
        this.timestamp = timestamp;
        this.threadId = threadId;
        this.threadName = threadName;
        this.threadState = threadState;
        this.stackTrace = stackTrace != null ? stackTrace : new StackTraceElement[0];
        this.threadCpuTime = threadCpuTime;
        this.threadUserTime = threadUserTime;
        this.blockedTime = blockedTime;
        this.waitedTime = waitedTime;
    }

    /**
     * Get top of stack (currently executing method)
     */
    public StackTraceElement getTopFrame() {
        return stackTrace.length > 0 ? stackTrace[0] : null;
    }

    /**
     * Get stack depth
     */
    public int getDepth() {
        return stackTrace.length;
    }

    /**
     * Check if thread was actually running (not waiting/blocked)
     */
    public boolean isRunnable() {
        return threadState == Thread.State.RUNNABLE;
    }

    /**
     * Get estimated size in bytes
     */
    public int getEstimatedSize() {
        // Rough estimate: base object + stack frames + strings
        return 100 + (stackTrace.length * 50);
    }

    @Override
    public String toString() {
        return String.format("StackSample[thread=%s, state=%s, depth=%d, time=%d]",
            threadName, threadState, stackTrace.length, timestamp);
    }
}
