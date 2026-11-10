package com.tonic.services.profiler.gc;

import java.util.List;

/**
 * How GC pauses in a recent window affect frames: pauses long enough to drop a frame at 60 and at 30 FPS, the longest
 * pause and GC time per minute
 */
public final class FrameImpact {
    public static final long FRAME_60_FPS = 16;
    public static final long FRAME_30_FPS = 33;

    public final long windowMs;
    public final int pauses;
    public final int over60Fps;
    public final int over30Fps;
    public final long longestMs;
    public final long totalMs;

    private FrameImpact(long windowMs, int pauses, int over60Fps, int over30Fps, long longestMs, long totalMs) {
        this.windowMs = windowMs;
        this.pauses = pauses;
        this.over60Fps = over60Fps;
        this.over30Fps = over30Fps;
        this.longestMs = longestMs;
        this.totalMs = totalMs;
    }

    /**
     * Measures the pauses that started within a window ending now
     * @param events the pauses, in any order
     * @param now the window's end in epoch milliseconds
     * @param windowMs the window's length in milliseconds
     * @return the impact over that window
     */
    public static FrameImpact of(List<GCPauseEvent> events, long now, long windowMs) {
        int pauses = 0;
        int over60 = 0;
        int over30 = 0;
        long longest = 0;
        long total = 0;
        for (GCPauseEvent event : events) {
            if (event.getTimestamp() <= now - windowMs || event.getTimestamp() > now) {
                continue;
            }
            long duration = event.getDuration();
            pauses++;
            total += duration;
            longest = Math.max(longest, duration);
            if (duration > FRAME_60_FPS) {
                over60++;
            }
            if (duration > FRAME_30_FPS) {
                over30++;
            }
        }
        return new FrameImpact(windowMs, pauses, over60, over30, longest, total);
    }

    /**
     * Gets GC pause time scaled to one minute
     * @return milliseconds of pause per minute over the window
     */
    public double msPerMinute() {
        return totalMs * 60000.0 / windowMs;
    }
}
