package com.tonic.plugins.walkassistant;

import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.data.locatables.BankLocations;
import com.tonic.services.pathfinder.Walker;
import com.tonic.util.ThreadPool;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@PluginDescriptor(
    name = "# Walk Assistant",
    description = "Pathfinds you to quest/clue destination, walk to nearest bank, cancel walk.",
    tags = {"walker", "walk", "assistant", "hotkey", "bank", "quest", "clue"}
)
public class WalkAssistantPlugin extends Plugin
{
    @Inject
    private WalkAssistantConfig config;

    @Inject
    private KeyManager keyManager;

    @Inject
    private PluginManager pluginManager;

    private final AtomicReference<Future<?>> activeWalk = new AtomicReference<>(null);

    private void log(String message)
    {
        if (config.debugLogging())
        {
            Logger.info(message);
        }
    }

    private void logWarn(String message)
    {
        if (config.debugLogging())
        {
            Logger.warn(message);
        }
    }

    private final HotkeyListener questDestListener = new HotkeyListener(() -> config.questDestHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            onQuestDestHotkey();
        }
    };

    private final HotkeyListener cancelWalkListener = new HotkeyListener(() -> config.cancelWalkHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            onCancelWalkHotkey();
        }
    };

    private final HotkeyListener nearestBankListener = new HotkeyListener(() -> config.nearestBankHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            onNearestBankHotkey();
        }
    };

    @Provides
    WalkAssistantConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(WalkAssistantConfig.class);
    }

    @Override
    protected void startUp()
    {
        keyManager.registerKeyListener(questDestListener);
        keyManager.registerKeyListener(cancelWalkListener);
        keyManager.registerKeyListener(nearestBankListener);
    }

    @Override
    protected void shutDown()
    {
        keyManager.unregisterKeyListener(questDestListener);
        keyManager.unregisterKeyListener(cancelWalkListener);
        keyManager.unregisterKeyListener(nearestBankListener);
        cancelActiveWalk();
    }

    private void onNearestBankHotkey()
    {
        submitWalk(BankLocations::walkToNearest);
    }

    private void onCancelWalkHotkey()
    {
        if (!Walker.isWalking() && !isWalkActive())
        {
            log("[WalkAssistant] No active walk to cancel.");
            return;
        }
        cancelActiveWalk();
        log("[WalkAssistant] Walk cancelled.");
    }

    private void onQuestDestHotkey()
    {
        WorldPoint destination = resolveQuestOrClueDestination();
        if (destination == null)
        {
            logWarn("[WalkAssistant] No active Quest Helper step or Clue Scroll destination found.");
            return;
        }
        submitWalk(() -> {
            log("[WalkAssistant] Walking to quest/clue destination: " + destination);
            Walker.walkTo(destination);
        });
    }

    private boolean isWalkActive()
    {
        Future<?> f = activeWalk.get();
        return f != null && !f.isDone();
    }

    private void cancelActiveWalk()
    {
        Walker.cancel();
        Future<?> f = activeWalk.getAndSet(null);
        if (f != null)
        {
            f.cancel(true);
        }
    }

    private void submitWalk(Runnable walkTask)
    {
        if (Walker.isWalking() || isWalkActive())
        {
            logWarn("[WalkAssistant] Walk already in progress, ignoring hotkey.");
            return;
        }
        final Future<?>[] holder = new Future<?>[1];
        Future<?> future = ThreadPool.submit(() -> {
            try
            {
                walkTask.run();
            }
            finally
            {
                activeWalk.compareAndSet(holder[0], null);
            }
        });
        holder[0] = future;
        activeWalk.set(future);
    }

    private WorldPoint resolveQuestOrClueDestination()
    {
        WorldPoint qhPoint = tryQuestHelper();
        if (qhPoint != null)
        {
            return qhPoint;
        }
        return tryClueScroll();
    }

    private WorldPoint tryQuestHelper()
    {
        try
        {
            Collection<Plugin> plugins = pluginManager.getPlugins();
            Optional<Plugin> qhPlugin = plugins.stream()
                .filter(p -> p.getClass().getName().contains("QuestHelperPlugin"))
                .findFirst();

            if (!qhPlugin.isPresent())
            {
                logWarn("[WalkAssistant] QuestHelperPlugin not found in plugin list.");
                return null;
            }

            Plugin instance = qhPlugin.get();
            log("[WalkAssistant] Found QuestHelper plugin: " + instance.getClass().getName());

            Field questManagerField = findField(instance.getClass(), "questManager");
            if (questManagerField == null)
            {
                logWarn("[WalkAssistant] 'questManager' field not found on " + instance.getClass().getName());
                return null;
            }
            questManagerField.setAccessible(true);
            Object questManager = questManagerField.get(instance);
            if (questManager == null)
            {
                logWarn("[WalkAssistant] questManager field is null.");
                return null;
            }
            log("[WalkAssistant] questManager type: " + questManager.getClass().getName());

            Method getSelectedQuest = findMethod(questManager.getClass(), "getSelectedQuest");
            if (getSelectedQuest == null)
            {
                logWarn("[WalkAssistant] 'getSelectedQuest' method not found on " + questManager.getClass().getName());
                return null;
            }
            getSelectedQuest.setAccessible(true);
            Object selectedQuest = getSelectedQuest.invoke(questManager);
            if (selectedQuest == null)
            {
                logWarn("[WalkAssistant] getSelectedQuest() returned null - no quest currently active in Quest Helper.");
                return null;
            }
            log("[WalkAssistant] selectedQuest type: " + selectedQuest.getClass().getName());

            boolean hasCurrentStep = false;
            Method getCurrentStep = findMethod(selectedQuest.getClass(), "getCurrentStep");
            if (getCurrentStep != null)
            {
                getCurrentStep.setAccessible(true);
                Object currentStep = getCurrentStep.invoke(selectedQuest);
                if (currentStep != null)
                {
                    hasCurrentStep = true;
                    log("[WalkAssistant] currentStep type: " + currentStep.getClass().getName());

                    Object activeStep = currentStep;
                    Method getActiveStep = findMethod(currentStep.getClass(), "getActiveStep");
                    if (getActiveStep != null)
                    {
                        getActiveStep.setAccessible(true);
                        Object unwrapped = getActiveStep.invoke(currentStep);
                        if (unwrapped != null)
                        {
                            activeStep = unwrapped;
                            log("[WalkAssistant] Unwrapped to active step: " + activeStep.getClass().getName());
                        }
                    }

                    WorldPoint destination = resolveWorldPointFromStep(activeStep);
                    if (destination != null)
                    {
                        log("[WalkAssistant] Resolved destination from active step: " + destination);
                        return destination;
                    }
                    logWarn("[WalkAssistant] No location on active step: " + activeStep.getClass().getName());
                }
                else
                {
                    log("[WalkAssistant] getCurrentStep() returned null (quest likely NOT_STARTED).");
                }
            }

            // Fallback: only use PanelDetails when there is no current step (quest NOT_STARTED).
            // If a step exists but has no location (e.g. dialogue step mid-quest), we should not
            // fall back to the quest start location as that would be misleading.
            if (!hasCurrentStep)
            {
                log("[WalkAssistant] Attempting PanelDetails fallback for start location...");
                WorldPoint fallback = tryQuestHelperPanelFallback(selectedQuest);
                if (fallback != null)
                {
                    log("[WalkAssistant] Resolved destination from PanelDetails fallback: " + fallback);
                    return fallback;
                }
            }

            logWarn("[WalkAssistant] All Quest Helper resolution strategies exhausted.");
        }
        catch (Exception e)
        {
            logWarn("[WalkAssistant] Quest Helper reflection failed: " + e.getMessage());
        }
        return null;
    }

    private WorldPoint resolveMapPoint(Object step)
    {
        try
        {
            Field mapPointField = findField(step.getClass(), "mapPoint");
            if (mapPointField == null)
            {
                return null;
            }
            mapPointField.setAccessible(true);
            Object mapPoint = mapPointField.get(step);
            if (mapPoint == null)
            {
                return null;
            }
            Method getWp = findMethod(mapPoint.getClass(), "getWorldPoint");
            if (getWp != null)
            {
                getWp.setAccessible(true);
                Object wp = getWp.invoke(mapPoint);
                if (wp instanceof WorldPoint)
                {
                    return (WorldPoint) wp;
                }
            }
        }
        catch (Exception e)
        {
            logWarn("[WalkAssistant] mapPoint resolution failed: " + e.getMessage());
        }
        return null;
    }

    private WorldPoint resolveWorldPointFromStep(Object step)
    {
        return resolveWorldPointFromStep(step, 0);
    }

    private WorldPoint resolveWorldPointFromStep(Object step, int depth)
    {
        if (step == null || depth > 3)
        {
            return null;
        }

        WorldPoint fromMapPoint = resolveMapPoint(step);
        if (fromMapPoint != null)
        {
            return fromMapPoint;
        }

        try
        {
            Field worldPointField = findField(step.getClass(), "worldPoint");
            if (worldPointField != null)
            {
                worldPointField.setAccessible(true);
                Object wpObj = worldPointField.get(step);
                if (wpObj instanceof WorldPoint)
                {
                    log("[WalkAssistant] Resolved via worldPoint field: " + wpObj);
                    return (WorldPoint) wpObj;
                }
                if (wpObj instanceof List)
                {
                    List<?> wpList = (List<?>) wpObj;
                    for (Object item : wpList)
                    {
                        if (item instanceof WorldPoint)
                        {
                            log("[WalkAssistant] Resolved via worldPoint list: " + item);
                            return (WorldPoint) item;
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            logWarn("[WalkAssistant] worldPoint field resolution failed: " + e.getMessage());
        }

        try
        {
            Field wmPointField = findField(step.getClass(), "worldMapPoint");
            if (wmPointField != null)
            {
                wmPointField.setAccessible(true);
                Object wmObj = wmPointField.get(step);
                if (wmObj != null)
                {
                    Method getWp = findMethod(wmObj.getClass(), "getWorldPoint");
                    if (getWp != null)
                    {
                        getWp.setAccessible(true);
                        Object wp = getWp.invoke(wmObj);
                        if (wp instanceof WorldPoint)
                        {
                            log("[WalkAssistant] Resolved via worldMapPoint field: " + wp);
                            return (WorldPoint) wp;
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            logWarn("[WalkAssistant] worldMapPoint resolution failed: " + e.getMessage());
        }

        try
        {
            Field stepsField = findField(step.getClass(), "steps");
            if (stepsField != null)
            {
                stepsField.setAccessible(true);
                Object stepsObj = stepsField.get(step);
                Collection<?> childSteps = null;

                if (stepsObj instanceof Map)
                {
                    childSteps = ((Map<?, ?>) stepsObj).values();
                    log("[WalkAssistant] ConditionalStep has " + childSteps.size() + " child steps (Map).");
                }
                else if (stepsObj instanceof Collection)
                {
                    childSteps = (Collection<?>) stepsObj;
                    log("[WalkAssistant] Step has " + childSteps.size() + " child steps (Collection).");
                }

                if (childSteps != null)
                {
                    for (Object child : childSteps)
                    {
                        WorldPoint childWp = resolveWorldPointFromStep(child, depth + 1);
                        if (childWp != null)
                        {
                            return childWp;
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            logWarn("[WalkAssistant] Child step resolution failed: " + e.getMessage());
        }

        return null;
    }

    private WorldPoint tryQuestHelperPanelFallback(Object selectedQuest)
    {
        try
        {
            Method getSteps = findMethod(selectedQuest.getClass(), "getSteps");
            if (getSteps == null)
            {
                logWarn("[WalkAssistant] 'getSteps' method not found on " + selectedQuest.getClass().getName());
                return null;
            }
            getSteps.setAccessible(true);
            Object panelListObj = getSteps.invoke(selectedQuest);
            if (!(panelListObj instanceof List))
            {
                logWarn("[WalkAssistant] getSteps() did not return a List, got: "
                    + (panelListObj == null ? "null" : panelListObj.getClass().getName()));
                return null;
            }

            List<?> panelList = (List<?>) panelListObj;
            if (panelList.isEmpty())
            {
                logWarn("[WalkAssistant] PanelDetails list is empty.");
                return null;
            }

            for (int pi = 0; pi < panelList.size(); pi++)
            {
                Object panel = panelList.get(pi);
                if (panel == null)
                {
                    continue;
                }
                log("[WalkAssistant] Checking PanelDetails[" + pi + "] type: " + panel.getClass().getName());

                Method getPanelSteps = findMethod(panel.getClass(), "getSteps");
                if (getPanelSteps == null)
                {
                    logWarn("[WalkAssistant] 'getSteps' not found on PanelDetails.");
                    continue;
                }
                getPanelSteps.setAccessible(true);
                Object stepsObj = getPanelSteps.invoke(panel);
                if (!(stepsObj instanceof List))
                {
                    continue;
                }

                List<?> steps = (List<?>) stepsObj;
                for (int si = 0; si < steps.size(); si++)
                {
                    Object step = steps.get(si);
                    if (step == null)
                    {
                        continue;
                    }
                    log("[WalkAssistant] Checking PanelDetails[" + pi + "].step[" + si + "] type: "
                        + step.getClass().getName());

                    WorldPoint wp = resolveWorldPointFromStep(step);
                    if (wp != null)
                    {
                        log("[WalkAssistant] Found start location in PanelDetails[" + pi
                            + "].step[" + si + "]: " + wp);
                        return wp;
                    }
                }
            }

            logWarn("[WalkAssistant] No step with a resolvable location found in any PanelDetails.");
        }
        catch (Exception e)
        {
            logWarn("[WalkAssistant] PanelDetails fallback failed: " + e.getMessage());
        }
        return null;
    }

    private WorldPoint tryClueScroll()
    {
        try
        {
            Collection<Plugin> plugins = pluginManager.getPlugins();
            Optional<Plugin> cluePlugin = plugins.stream()
                .filter(p -> p.getClass().getName().contains("ClueScrollPlugin"))
                .findFirst();

            if (!cluePlugin.isPresent())
            {
                return null;
            }

            Plugin instance = cluePlugin.get();
            Field clueField = findField(instance.getClass(), "clue");
            if (clueField == null)
            {
                return null;
            }
            clueField.setAccessible(true);
            Object clue = clueField.get(instance);
            if (clue == null)
            {
                return null;
            }

            Method getLocation = findMethod(clue.getClass(), "getLocation");
            if (getLocation != null)
            {
                getLocation.setAccessible(true);

                final Method locationMethod = getLocation;
                final Object clueInstance = clue;
                final Plugin pluginInstance = instance;
                Object result = Static.invoke(() -> {
                    try
                    {
                        if (locationMethod.getParameterCount() == 1)
                        {
                            return locationMethod.invoke(clueInstance, pluginInstance);
                        }
                        return locationMethod.invoke(clueInstance);
                    }
                    catch (Exception e)
                    {
                        return null;
                    }
                });

                if (result instanceof WorldPoint)
                {
                    log("[WalkAssistant] Resolved clue destination via getLocation(): " + result);
                    return (WorldPoint) result;
                }
            }

            Field locationField = findField(clue.getClass(), "location");
            if (locationField != null)
            {
                locationField.setAccessible(true);
                Object loc = locationField.get(clue);
                if (loc instanceof WorldPoint)
                {
                    log("[WalkAssistant] Resolved clue destination via 'location' field: " + loc);
                    return (WorldPoint) loc;
                }
            }

            Field npcLocationField = findField(clue.getClass(), "npcLocation");
            if (npcLocationField != null)
            {
                npcLocationField.setAccessible(true);
                Object npcLoc = npcLocationField.get(clue);
                if (npcLoc instanceof WorldPoint)
                {
                    log("[WalkAssistant] Resolved clue destination via 'npcLocation' field: " + npcLoc);
                    return (WorldPoint) npcLoc;
                }
            }

            logWarn("[WalkAssistant] No location could be resolved from clue type: " + clue.getClass().getName());
        }
        catch (Exception e)
        {
            logWarn("[WalkAssistant] ClueScroll reflection failed: " + e.getMessage());
        }
        return null;
    }

    private static Field findField(Class<?> clazz, String name)
    {
        while (clazz != null && clazz != Object.class)
        {
            try
            {
                return clazz.getDeclaredField(name);
            }
            catch (NoSuchFieldException ignored)
            {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> clazz, String name)
    {
        while (clazz != null && clazz != Object.class)
        {
            for (Method m : clazz.getDeclaredMethods())
            {
                if (m.getName().equals(name))
                {
                    return m;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
