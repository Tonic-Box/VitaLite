package com.tonic.plugins.breakhandler;

import com.tonic.Logger;
import com.tonic.plugins.breakhandler.settings.Property;
import com.tonic.services.ConfigManager;
import lombok.Getter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.util.Text;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class BreakHandler
{
    private static final ConfigManager CONFIG = new ConfigManager("BreakHandler");
    private static BreakHandler INSTANCE;

    public static BreakHandler getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new BreakHandler(CONFIG);
        }

        return INSTANCE;
    }

    @Getter
    private final ConfigManager configManager;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private BreakHandler(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    /**
     * Registers a plugin for breaks without any conditions to logout.
     *
     * @param plugin Plugin
     */
    public void register(Plugin plugin)
    {
        register(plugin, () -> true, () -> true, null, null);
    }

    /**
     * Registers plugin for breaks without any conditions to break, and with
     * Runnables to execute at break start and end.
     *
     * @param plugin Plugin
     * @param startCallback executes on break start
     * @param endCallback executes on break end
     */
    public void register(Plugin plugin, Runnable startCallback, Runnable endCallback)
    {
        register(plugin, () -> true, () -> true, startCallback, endCallback);
    }

    /**
     * Registers plugin for breaks with specific BooleanSuppliers letting the break handler
     * know if it needs to wait to take control and log out for the break.
     *
     * @param plugin Plugin
     * @param access If breaks are accessible
     * @param start If breaks are possible
     */

    public void register(Plugin plugin, BooleanSupplier access, BooleanSupplier start)
    {
        register(plugin, access, start, null, null);
    }

    /**
     * Registers plugin for breaks with BooleanSuppliers letting the break handler know
     * if it needs to wait to take control and log out for the break, and Runnables to
     * execute at break start and end.
     *
     * @param plugin Plugin
     * @param access If breaks are accessible
     * @param start If breaks are possible
     * @param startCallback executes on break start
     * @param endCallback executes on break end
     */
    public void register(Plugin plugin, BooleanSupplier access, BooleanSupplier start, Runnable startCallback, Runnable endCallback)
    {
        sessions.computeIfAbsent(plugin.getName(), k -> new Session(plugin, access, start, startCallback, endCallback));
    }

    /**
     * Unregisters a plugin from the break handler.
     *
     * @param plugin Plugin
     */
    public void unregister(Plugin plugin)
    {
        stop(plugin);
        sessions.remove(plugin.getName());
    }

    /**
     * Starts scheduling breaks for a plugin.
     *
     * @param plugin Plugin
     */
    public void start(Plugin plugin)
    {
        Session session = sessions.get(plugin.getName());
        if (session == null) return;

        scheduleBreak(session);
    }

    /**
     * Stops scheduling breaks for a plugin.
     *
     * @param plugin Plugin
     */
    public void stop(Plugin plugin)
    {
        Session session = sessions.get(plugin.getName());
        if (session == null) return;
        session.scheduledBreak = null;
    }

    /**
     * Gets the next scheduled break for this plugin
     * @param plugin
     * @return scheduled break, might be null
     */
    public Break getScheduledBreak(Plugin plugin)
    {
        return Optional.ofNullable(sessions.get(plugin.getName()))
                .map(s -> s.scheduledBreak)
                .orElse(null);
    }

    /**
     * @return is taking a break
     * @param plugin
     */
    public boolean isBreaking(Plugin plugin)
    {
        Session session = sessions.get(plugin.getName());

        if (session == null)
        {
            return false;
        }

        Break scheduledBreak = session.scheduledBreak;

        if (scheduledBreak.isStarted())
        {
            return true;
        }

        return scheduledBreak.isBreakReady()
                && scheduledBreak.getCanStart().getAsBoolean()
                && scheduledBreak.getCanAccess().getAsBoolean();
    }

    /**
     * Reschedules break by force.
     * @param plugin
     */
    public void reschedule(Plugin plugin)
    {
        Session session = sessions.get(plugin.getName());
        if (session == null) return;

        scheduleBreak(session);
    }

    /**
     * Starts the next scheduled break by force, will schedule and instantly start if no breaks
     * are scheduled.
     * @param plugin
     */
    public void forceStartBreak(Plugin plugin)
    {
        Session session = sessions.get(plugin.getName());
        if (session == null) return;

        session.scheduledBreak.setStartTime(Instant.now());

        log("[%s] Break force started for %d minutes",
                Text.removeTags(plugin.getName()), session.scheduledBreak.getDuration().toMinutes());
    }

    /**
     * Stops the current break by force, will schedule the next break using the user defined
     * settings.
     * @param plugin
     */
    public void forceStopBreak(Plugin plugin)
    {
        Session session = sessions.get(plugin.getName());
        if (session == null) return;

        log("[%s] Break force ended", Text.removeTags(plugin.getName()));
        scheduleBreak(session);
    }

    /**
     * Gets all breaks
     * @return list of all breaks from registered plugins
     */
    public List<Break> getAllBreaks()
    {
        return sessions.values().stream()
                .filter(session -> session.scheduledBreak != null)
                .map(session -> session.scheduledBreak)
                .collect(Collectors.toList());
    }

    boolean isReadyToLogin()
    {
        return sessions.entrySet()
                .stream()
                .anyMatch(entry -> {
                    Session session = entry.getValue();
                    Break scheduledBreak = session.scheduledBreak;

                    if (scheduledBreak == null)
                    {
                        return false;
                    }

                    return scheduledBreak.isBreakOver();
                });
    }

    boolean isReadyToBreak()
    {
        return sessions.entrySet()
                .stream()
                .anyMatch(entry -> {
                    Session session = entry.getValue();
                    Break scheduledBreak = session.scheduledBreak;

                    if (scheduledBreak == null)
                    {
                        return false;
                    }

                    return scheduledBreak.isBreakReady()
                            && scheduledBreak.getCanAccess().getAsBoolean()
                            && scheduledBreak.getCanStart().getAsBoolean();
                });
    }

    void log(String format, Object... args)
    {
        Logger.info(String.format(format, args));
    }

    void cancel()
    {
        sessions.clear();
    }

    void notifyLogout()
    {
        Instant now = Instant.now();
        for (Session session : sessions.values())
        {
            Break b = session.scheduledBreak;
            if (b == null) continue;

            boolean due = !now.isBefore(b.getStartTime());
            boolean allowed = (b.getCanAccess() == null || b.getCanAccess().getAsBoolean())
                    && (b.getCanStart()  == null || b.getCanStart().getAsBoolean());

            if (due && allowed)
            {
                if (!b.isStarted())
                {
                    b.setStarted(true);
                    b.setStartTime(Instant.now());

                    if (b.getStartCallback() != null)
                    {
                        b.getStartCallback().run();
                    }

                    log("[%s] Break started for %d minutes",
                            Text.removeTags(b.getPluginName()), b.getDuration().toMinutes());
                }
            }
        }
    }

    void notifyLogin()
    {
        for (Session session : sessions.values())
        {
            Break b = session.scheduledBreak;
            if (b == null)
            {
                continue;
            }

            if (b.isBreakOver())
            {
                if (b.getEndCallback() != null)
                {
                    b.getEndCallback().run();
                }

                log("[%s] Break ended", Text.removeTags(b.getPluginName()));
                scheduleBreak(session);
            }
        }
    }

    private void scheduleBreak(Session s)
    {
        Duration between = randomBetweenMinutes(Property.MIN_BETWEEN.key(), Property.MAX_BETWEEN.key(), 120, 240);
        Duration duration = randomBetweenMinutes(Property.MIN_DURATION.key(), Property.MAX_DURATION.key(), 120, 240);

        Instant startAt = Instant.now().plus(between);

        s.scheduledBreak = new Break(
                s.plugin.getName(), startAt, duration,
                s.canAccess, s.canStart, s.startCallback, s.endCallback,
                false
        );

        Logger.info(String.format("[%s] Break scheduled in %d minutes for %d minutes",
                Text.removeTags(s.plugin.getName()), Duration.between(Instant.now(), startAt).toMinutes(),
                duration.toMinutes()));
    }

    private Duration randomBetweenMinutes(String minKey, String maxKey, int defMin, int defMax)
    {
        int min = configManager.getIntOrDefault(minKey, defMin);
        int max = configManager.getIntOrDefault(maxKey, defMax);
        if (max < min) max = min;
        int span = max - min;
        int pick = min + (span == 0 ? 0 : random.nextInt(span + 1));
        return Duration.ofMinutes(pick);
    }

    private static class Session
    {
        final Plugin plugin;
        final BooleanSupplier canAccess;
        final BooleanSupplier canStart;
        final Runnable startCallback;
        final Runnable endCallback;

        volatile Break scheduledBreak;

        Session(Plugin plugin, BooleanSupplier access, BooleanSupplier start, Runnable startCallback, Runnable endCallback)
        {
            this.plugin = plugin;
            this.canAccess = access;
            this.canStart = start;
            this.startCallback = startCallback;
            this.endCallback = endCallback;
        }
    }
}
