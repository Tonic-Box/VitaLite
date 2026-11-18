package com.tonic.services;

import com.tonic.Static;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.events.WorldViewLoaded;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.client.eventbus.Subscribe;

/**
 * Marks the SceneCache out of date whenever the scenery changes.
 */
public final class SceneCacheEvents
{
    private static SceneCacheEvents instance;

    private SceneCacheEvents()
    {
    }

    static void init()
    {
        if (instance != null)
        {
            return;
        }
        instance = new SceneCacheEvents();
        Static.getRuneLite().getEventBus().register(instance);
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        SceneCache.invalidate();
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        SceneCache.invalidate();
    }

    @Subscribe
    public void onWallObjectSpawned(WallObjectSpawned event)
    {
        SceneCache.invalidate();
    }

    @Subscribe
    public void onWallObjectDespawned(WallObjectDespawned event)
    {
        SceneCache.invalidate();
    }

    @Subscribe
    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
    {
        SceneCache.invalidate();
    }

    @Subscribe
    public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
    {
        SceneCache.invalidate();
    }

    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event)
    {
        SceneCache.invalidate();
    }

    @Subscribe
    public void onGroundObjectDespawned(GroundObjectDespawned event)
    {
        SceneCache.invalidate();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        SceneCache.invalidate();
    }

    @Subscribe
    public void onWorldViewLoaded(WorldViewLoaded event)
    {
        SceneCache.invalidate();
    }

    @Subscribe
    public void onWorldViewUnloaded(WorldViewUnloaded event)
    {
        SceneCache.invalidate();
    }
}
