package com.tonic.services.worldselect;

import net.runelite.client.plugins.worldhopper.ping.Ping;
import net.runelite.http.api.worlds.World;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pings worlds with RuneLite's world hopper ping, falling back to TCP where ICMP fails, on a few threads of its own, keeping each result for a minute; pings queued by an earlier opening are dropped once the world select closes.
 */
final class WorldPinger
{
    private static final int THREADS = 8;
    private static final long FRESH_MILLIS = 60_000;

    private final ThreadPoolExecutor pool;
    private final Map<Integer, long[]> results = new ConcurrentHashMap<>();
    private final AtomicInteger generation = new AtomicInteger();
    private volatile boolean changed;

    WorldPinger()
    {
        AtomicInteger threads = new AtomicInteger();
        pool = new ThreadPoolExecutor(THREADS, THREADS, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), task ->
        {
            Thread thread = new Thread(task, "world-select-ping-" + threads.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        pool.allowCoreThreadTimeOut(true);
    }

    /**
     * @param world a world number
     * @return the last ping to it in milliseconds, or -1 when it has none or it failed
     */
    int ping(int world)
    {
        long[] result = results.get(world);
        return result == null ? -1 : (int) result[0];
    }

    /**
     * Pings every world whose result is missing or older than a minute.
     *
     * @param worlds the worlds
     */
    void pingAll(List<World> worlds)
    {
        int ticket = generation.incrementAndGet();
        long now = System.currentTimeMillis();
        for (World world : worlds)
        {
            long[] result = results.get(world.getId());
            if (result != null && now - result[1] < FRESH_MILLIS)
            {
                continue;
            }
            pool.execute(() ->
            {
                if (generation.get() != ticket)
                {
                    return;
                }
                int millis = Ping.ping(world, true);
                results.put(world.getId(), new long[]{millis, System.currentTimeMillis()});
                changed = true;
            });
        }
    }

    /**
     * Drops the pings still queued.
     */
    void stop()
    {
        generation.incrementAndGet();
    }

    /**
     * @return whether a ping finished since the last call
     */
    boolean takeChanged()
    {
        if (!changed)
        {
            return false;
        }
        changed = false;
        return true;
    }
}
