package com.tonic.services;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.profiler.frames.FrameProfiler;
import com.tonic.services.profiler.server.ProfilerServer;
import com.tonic.services.streamer.Streamer;
import net.runelite.api.Client;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ClientTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.DrawManager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Feeds the profiler's frame timing: marks each client frame, times the part of it spent drawing, counts the engine cycles run in it, and reports the renderer the frames were drawn with.
 */
public final class FrameProfiling
{
    private static final int RENDER = FrameProfiler.timer("client.render");
    private static final int CYCLES = FrameProfiler.counter("client.cycles");

    private static long renderBegan;

    private FrameProfiling()
    {
    }

    /**
     * Starts marking frames, and starts the profiler's API when the client was launched with -Dvita.profiler.api=true.
     */
    public static void install()
    {
        EventBus eventBus = Static.getInjector().getInstance(EventBus.class);
        eventBus.register(BeforeRender.class, event -> beforeRender(), 0);
        eventBus.register(ClientTick.class, event -> FrameProfiler.count(CYCLES, 1), 0);
        Static.getInjector().getInstance(DrawManager.class).registerEveryFrameListener(FrameProfiling::drawn);
        FrameProfiler.setContext(FrameProfiling::context);
        if (Boolean.getBoolean("vita.profiler.api"))
        {
            try
            {
                ProfilerServer.getInstance().start();
            }
            catch (IOException e)
            {
                Logger.error("Profiler API could not start: " + e.getMessage());
            }
        }
    }

    private static void beforeRender()
    {
        FrameProfiler.frame();
        renderBegan = FrameProfiler.begin();
    }

    private static void drawn()
    {
        FrameProfiler.end(RENDER, renderBegan);
        renderBegan = 0;
    }

    private static Map<String, Object> context()
    {
        Client client = Static.getClient();
        Map<String, Object> context = new LinkedHashMap<>();
        if (client == null)
        {
            return context;
        }
        context.put("gpu", client.isGpu());
        context.put("streamer", Streamer.active);
        context.put("canvasWidth", client.getCanvasWidth());
        context.put("canvasHeight", client.getCanvasHeight());
        context.put("renderScalePercent", SceneScale.percent());
        context.put("stretched", client.isStretchedEnabled());
        context.put("gameState", String.valueOf(client.getGameState()));
        return context;
    }
}
