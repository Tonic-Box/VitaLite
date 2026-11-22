package com.tonic.services.profiler.frames;

import jdk.jfr.EventType;
import lombok.Getter;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Supplier;

/**
 * Frame timing of the client: frame rate, frame time percentiles, client thread allocation and GC since the last reset, named sections timed or counted inside frames, what ran in frames slower than a threshold, and the client thread's stacks sampled in slow frames against the others.
 */
public final class FrameProfiler
{
    /**
     * The name of the Flight Recorder event spanning each client frame.
     */
    public static final String EVENT_NAME = "vita.Frame";

    private static final EventType FRAME_EVENT = EventType.getEventType(FrameEvent.class);
    private static final int CAPACITY = 1 << 16;
    private static final int MAX_SECTIONS = 64;
    private static final int MAX_SLOW_FRAMES = 30;
    private static final int TOP_STACKS = 25;
    private static final long SECOND = 1_000_000_000L;
    private static final GarbageCollectorMXBean[] GC_BEANS = ManagementFactory.getGarbageCollectorMXBeans().toArray(new GarbageCollectorMXBean[0]);

    private static final Object LOCK = new Object();
    private static final long[] stamps = new long[CAPACITY];
    private static long frames;
    private static long resetAt = System.nanoTime();
    private static Thread client;
    private static StackSampler sampler;
    private static int samplePeriodMillis;
    private static final Map<String, Integer> slowStacks = new HashMap<>();
    private static final Map<String, Integer> normalStacks = new HashMap<>();
    private static int slowSamples;
    private static int normalSamples;
    private static long allocatedAtReset = -1;
    private static long gcCountAtReset = gcCount();
    private static long gcTimeAtReset = gcTime();
    private static long gcCountAtFrame = gcCountAtReset;
    private static long gcTimeAtFrame = gcTimeAtReset;
    private static final ArrayDeque<Map<String, Object>> slowFrames = new ArrayDeque<>();
    private static final List<String> drained = new ArrayList<>();

    private static final List<String> names = new ArrayList<>();
    private static final List<Boolean> counters = new ArrayList<>();
    private static final AtomicLongArray totals = new AtomicLongArray(MAX_SECTIONS);
    private static final AtomicLongArray calls = new AtomicLongArray(MAX_SECTIONS);
    private static final AtomicLongArray maxima = new AtomicLongArray(MAX_SECTIONS);
    private static final AtomicLongArray inFrame = new AtomicLongArray(MAX_SECTIONS);
    private static volatile long slowFrameNanos;
    @Getter
    private static volatile boolean sectionsEnabled = Boolean.getBoolean("vita.frameprofile");
    private static volatile Supplier<Map<String, Object>> context;
    private static FrameEvent frameEvent;
    private static long frameNumber;

    private FrameProfiler()
    {
    }

    /**
     * Marks the end of a client frame. Called once per frame on the client thread.
     */
    public static void frame()
    {
        recordFrameEvent();
        long now = System.nanoTime();
        synchronized (LOCK)
        {
            if (client == null)
            {
                client = Thread.currentThread();
                allocatedAtReset = allocated(client.getId());
            }
            if (slowFrameNanos > 0 && frames > 0)
            {
                endFrame(now - stamps[(int) ((frames - 1) & (CAPACITY - 1))], now);
            }
            stamps[(int) (frames & (CAPACITY - 1))] = now;
            frames++;
        }
    }

    /**
     * Starts or stops sampling the client thread's stack, sorting each sample into slow frames or the others; needs a slow frame threshold. Each sample briefly stops the client thread, so sampling costs a little frame time while on.
     *
     * @param periodMillis milliseconds between samples, or 0 to stop
     * @return whether sampling is on
     */
    public static boolean setStackSampling(int periodMillis)
    {
        synchronized (LOCK)
        {
            if (sampler != null)
            {
                sampler.stop();
                sampler = null;
            }
            clearStacks();
            samplePeriodMillis = 0;
            if (periodMillis > 0 && client != null)
            {
                samplePeriodMillis = periodMillis;
                sampler = new StackSampler(client, periodMillis);
            }
            return sampler != null;
        }
    }

    /**
     * Starts or stops keeping what ran in frames slower than a threshold: each section's share of the frame and the GC during it.
     *
     * @param milliseconds the threshold, or 0 to stop
     */
    public static void setSlowFrameMs(int milliseconds)
    {
        synchronized (LOCK)
        {
            slowFrameNanos = Math.max(0, milliseconds) * 1_000_000L;
            slowFrames.clear();
            gcCountAtFrame = gcCount();
            gcTimeAtFrame = gcTime();
            clearInFrame();
        }
    }

    /**
     * Registers a timed section, or finds the one already registered under the name.
     *
     * @param name the section's name
     * @return the section's id
     * @throws IllegalStateException if every section slot is taken
     */
    public static int timer(String name)
    {
        return register(name, false);
    }

    /**
     * Registers a counted section, whose samples are summed per frame rather than timed, or finds the one already registered under the name.
     *
     * @param name the section's name
     * @return the section's id
     * @throws IllegalStateException if every section slot is taken
     */
    public static int counter(String name)
    {
        return register(name, true);
    }

    /**
     * @return the start time to pass to end, or 0 while sections are not collected
     */
    public static long begin()
    {
        return sectionsEnabled ? System.nanoTime() : 0L;
    }

    /**
     * Adds the time since begin to a timed section, unless begin returned 0.
     *
     * @param section the section's id
     * @param began what begin returned
     */
    public static void end(int section, long began)
    {
        if (began != 0L)
        {
            record(section, System.nanoTime() - began);
        }
    }

    /**
     * Adds a sample to a counted section while sections are collected.
     *
     * @param section the section's id
     * @param amount the sample
     */
    public static void count(int section, long amount)
    {
        if (sectionsEnabled)
        {
            record(section, amount);
        }
    }

    /**
     * Starts or stops collecting sections; frame rate is always collected.
     *
     * @param enabled whether sections are collected
     */
    public static void setSectionsEnabled(boolean enabled)
    {
        sectionsEnabled = enabled;
    }

    /**
     * Sets what the snapshot reports about the client's state, such as its renderer.
     *
     * @param supplier the context, read when a snapshot is taken
     */
    public static void setContext(Supplier<Map<String, Object>> supplier)
    {
        context = supplier;
    }

    /**
     * Starts measuring again from now.
     */
    public static void reset()
    {
        synchronized (LOCK)
        {
            frames = 0;
            resetAt = System.nanoTime();
            allocatedAtReset = client == null ? -1 : allocated(client.getId());
            gcCountAtReset = gcCount();
            gcTimeAtReset = gcTime();
            gcCountAtFrame = gcCountAtReset;
            gcTimeAtFrame = gcTimeAtReset;
            slowFrames.clear();
            clearStacks();
            for (int i = 0; i < MAX_SECTIONS; i++)
            {
                totals.set(i, 0);
                calls.set(i, 0);
                maxima.set(i, 0);
                inFrame.set(i, 0);
            }
        }
    }

    /**
     * Summarises everything since the last reset.
     *
     * @return the frame rate, frame times, allocation, GC and sections, as nested maps
     */
    public static Map<String, Object> snapshot()
    {
        long count;
        long started;
        long[] window;
        long allocatedBefore;
        long thread;
        long gcCountBefore;
        long gcTimeBefore;
        List<Map<String, Object>> slow;
        Map<String, Object> stacks;
        synchronized (LOCK)
        {
            slow = new ArrayList<>(slowFrames);
            stacks = samplePeriodMillis > 0 || slowSamples + normalSamples > 0 ? stacks() : null;
            count = frames;
            started = resetAt;
            int kept = (int) Math.min(count, CAPACITY);
            window = new long[kept];
            for (int i = 0; i < kept; i++)
            {
                window[i] = stamps[(int) ((count - kept + i) & (CAPACITY - 1))];
            }
            allocatedBefore = allocatedAtReset;
            thread = client == null ? -1 : client.getId();
            gcCountBefore = gcCountAtReset;
            gcTimeBefore = gcTimeAtReset;
        }

        long now = System.nanoTime();
        double seconds = (now - started) / (double) SECOND;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("frames", count);
        out.put("seconds", round(seconds));
        out.put("fps", round(seconds > 0 ? count / seconds : 0));
        out.put("lastSecondFps", lastSecond(window, now));
        out.put("frameMs", frameTimes(window));
        if (thread >= 0 && allocatedBefore >= 0 && count > 0)
        {
            long allocatedBytes = allocated(thread) - allocatedBefore;
            out.put("clientAllocatedBytesPerFrame", allocatedBytes / count);
        }
        out.put("gc", map("count", gcCount() - gcCountBefore, "timeMs", gcTime() - gcTimeBefore));
        out.put("sectionsEnabled", sectionsEnabled);
        out.put("sections", sections(count));
        out.put("slowFrameMs", slowFrameNanos / 1_000_000L);
        out.put("slowFrames", slow);
        if (stacks != null)
        {
            out.put("stacks", stacks);
        }
        Supplier<Map<String, Object>> supplier = context;
        if (supplier != null)
        {
            try
            {
                out.put("context", supplier.get());
            }
            catch (RuntimeException e)
            {
                out.put("context", map("error", String.valueOf(e)));
            }
        }
        return out;
    }

    private static int register(String name, boolean counter)
    {
        synchronized (names)
        {
            int existing = names.indexOf(name);
            if (existing >= 0)
            {
                return existing;
            }
            if (names.size() == MAX_SECTIONS)
            {
                throw new IllegalStateException("every frame profiler section is taken");
            }
            names.add(name);
            counters.add(counter);
            return names.size() - 1;
        }
    }

    private static void recordFrameEvent()
    {
        if (frameEvent != null)
        {
            frameEvent.commit();
            frameEvent = null;
        }
        if (FRAME_EVENT.isEnabled())
        {
            frameEvent = new FrameEvent();
            frameEvent.number = frameNumber++;
            frameEvent.begin();
        }
    }

    private static void endFrame(long interval, long now)
    {
        boolean slowFrame = interval > slowFrameNanos;
        if (sampler != null)
        {
            drained.clear();
            sampler.drain(now - interval, now, drained);
            Map<String, Integer> stacks = slowFrame ? slowStacks : normalStacks;
            for (String stack : drained)
            {
                stacks.merge(stack, 1, Integer::sum);
            }
            if (slowFrame)
            {
                slowSamples += drained.size();
            }
            else
            {
                normalSamples += drained.size();
            }
        }
        long gcCount = gcCount();
        long gcTime = gcTime();
        if (slowFrame)
        {
            Map<String, Object> ran = new LinkedHashMap<>();
            synchronized (names)
            {
                for (int i = 0; i < names.size(); i++)
                {
                    long amount = inFrame.get(i);
                    if (amount != 0)
                    {
                        ran.put(names.get(i), round(counters.get(i) ? amount : amount / 1e6));
                    }
                }
            }
            if (slowFrames.size() == MAX_SLOW_FRAMES)
            {
                slowFrames.removeFirst();
            }
            slowFrames.addLast(map("atSeconds", round((now - resetAt) / (double) SECOND), "ms", round(interval / 1e6), "gcCount", gcCount - gcCountAtFrame, "gcMs", gcTime - gcTimeAtFrame, "sections", ran));
        }
        gcCountAtFrame = gcCount;
        gcTimeAtFrame = gcTime;
        clearInFrame();
    }

    private static void clearInFrame()
    {
        for (int i = 0; i < MAX_SECTIONS; i++)
        {
            inFrame.set(i, 0);
        }
    }

    private static void clearStacks()
    {
        slowStacks.clear();
        normalStacks.clear();
        slowSamples = 0;
        normalSamples = 0;
    }

    private static Map<String, Object> stacks()
    {
        Map<String, double[]> methods = new HashMap<>();
        slowStacks.forEach((stack, count) -> methods.computeIfAbsent(innermost(stack), key -> new double[2])[0] += count);
        normalStacks.forEach((stack, count) -> methods.computeIfAbsent(innermost(stack), key -> new double[2])[1] += count);
        List<Map<String, Object>> excess = new ArrayList<>();
        methods.entrySet().stream().sorted((a, b) -> Double.compare(share(b.getValue()[0], slowSamples) - share(b.getValue()[1], normalSamples), share(a.getValue()[0], slowSamples) - share(a.getValue()[1], normalSamples))).limit(TOP_STACKS).forEach(entry -> excess.add(map("method", entry.getKey(), "slowPercent", round(share(entry.getValue()[0], slowSamples)), "normalPercent", round(share(entry.getValue()[1], normalSamples)))));
        return map("periodMs", samplePeriodMillis, "slowSamples", slowSamples, "normalSamples", normalSamples, "innermostBySlowExcess", excess, "slowStacks", top(slowStacks, slowSamples), "normalStacks", top(normalStacks, normalSamples));
    }

    private static List<Map<String, Object>> top(Map<String, Integer> stacks, int total)
    {
        List<Map<String, Object>> out = new ArrayList<>();
        stacks.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(TOP_STACKS).forEach(entry -> out.add(map("percent", round(share(entry.getValue(), total)), "samples", entry.getValue(), "stack", entry.getKey())));
        return out;
    }

    private static String innermost(String stack)
    {
        int end = stack.indexOf(" < ");
        return end < 0 ? stack : stack.substring(0, end);
    }

    private static double share(double count, int total)
    {
        return total == 0 ? 0 : count * 100 / total;
    }

    private static void record(int section, long amount)
    {
        if (slowFrameNanos > 0)
        {
            inFrame.addAndGet(section, amount);
        }
        totals.addAndGet(section, amount);
        calls.incrementAndGet(section);
        long max = maxima.get(section);
        while (amount > max && !maxima.compareAndSet(section, max, amount))
        {
            max = maxima.get(section);
        }
    }

    private static List<Map<String, Object>> sections(long count)
    {
        List<Map<String, Object>> out = new ArrayList<>();
        synchronized (names)
        {
            for (int i = 0; i < names.size(); i++)
            {
                long sectionCalls = calls.get(i);
                if (sectionCalls == 0)
                {
                    continue;
                }
                long total = totals.get(i);
                boolean counter = counters.get(i);
                double scale = counter ? 1 : 1e6;
                Map<String, Object> section = new LinkedHashMap<>();
                section.put("name", names.get(i));
                section.put("kind", counter ? "count" : "ms");
                section.put("perFrame", round(count > 0 ? total / scale / count : 0));
                section.put("perCall", round(total / scale / sectionCalls));
                section.put("max", round(maxima.get(i) / scale));
                section.put("callsPerFrame", round(count > 0 ? sectionCalls / (double) count : 0));
                out.add(section);
            }
        }
        return out;
    }

    private static double lastSecond(long[] window, long now)
    {
        int frameCount = 0;
        for (int i = window.length - 1; i >= 0 && now - window[i] <= SECOND; i--)
        {
            frameCount++;
        }
        return frameCount;
    }

    private static Map<String, Object> frameTimes(long[] window)
    {
        if (window.length < 2)
        {
            return map();
        }
        long[] intervals = new long[window.length - 1];
        long sum = 0;
        for (int i = 1; i < window.length; i++)
        {
            intervals[i - 1] = window[i] - window[i - 1];
            sum += intervals[i - 1];
        }
        Arrays.sort(intervals);
        int onePercent = Math.max(1, intervals.length / 100);
        long slowest = 0;
        for (int i = intervals.length - onePercent; i < intervals.length; i++)
        {
            slowest += intervals[i];
        }
        return map("avg", round(sum / 1e6 / intervals.length), "p50", percentile(intervals, 0.50), "p90", percentile(intervals, 0.90), "p95", percentile(intervals, 0.95), "p99", percentile(intervals, 0.99), "max", round(intervals[intervals.length - 1] / 1e6), "onePercentLowFps", round(onePercent * 1e9 / slowest));
    }

    private static double percentile(long[] sorted, double fraction)
    {
        int index = (int) Math.min(sorted.length - 1, Math.round(fraction * (sorted.length - 1)));
        return round(sorted[index] / 1e6);
    }

    private static long allocated(long thread)
    {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean instanceof com.sun.management.ThreadMXBean)
        {
            return ((com.sun.management.ThreadMXBean) bean).getThreadAllocatedBytes(thread);
        }
        return -1;
    }

    private static long gcCount()
    {
        long count = 0;
        for (GarbageCollectorMXBean bean : GC_BEANS)
        {
            count += Math.max(0, bean.getCollectionCount());
        }
        return count;
    }

    private static long gcTime()
    {
        long time = 0;
        for (GarbageCollectorMXBean bean : GC_BEANS)
        {
            time += Math.max(0, bean.getCollectionTime());
        }
        return time;
    }

    private static double round(double value)
    {
        return Math.round(value * 100) / 100.0;
    }

    private static Map<String, Object> map(Object... pairs)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2)
        {
            out.put((String) pairs[i], pairs[i + 1]);
        }
        return out;
    }
}
