package com.tonic.api.game;

import com.tonic.Static;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.queries.WorldQuery;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.game.WorldService;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

/**
 * Worlds related API
 */
public class WorldsAPI
{
    /**
     * Get the current world the client is logged into
     * @return The current world
     */
    public static World getCurrentWorld()
    {
        Client client = Static.getClient();
        return new WorldQuery().withId(client.getWorld()).first();
    }

    /**
     * @param includeCurrentWorld Whether to include the current world or not
     * @return A query filtering out *generally unwanted* worlds. This means skill totals, non main game worlds, and pvp are filtered
     */
    public static WorldQuery createDefaultQuery(boolean includeCurrentWorld)
    {
        Client client = Static.getClient();
        return new WorldQuery()
            .notSkillTotalWorlds()
            .isMainGame()
            .notPvp()
            .keepIf(w -> includeCurrentWorld || w.getId() != client.getWorld());
    }

    /**
     * Hop to a random members world (not skill total, not pvp, main game)
     */
    public static StepHandler hopRandomMembers()
    {
        World world = createDefaultQuery(false).isP2p().random();
        return hop(world);
    }

    /**
     * Hop to a random free to play world (not skill total, not pvp, main game)
     */
    public static StepHandler hopRandomF2p()
    {
        World world = createDefaultQuery(false).isF2p().random();
        return hop(world);
    }

    /**
     * Hops to the next members world with an ID higher than the current and not skill total, not pvp, main game.
     * If you're on the highest world already, it hops back to the first world in the list
     */
    public static StepHandler hopNextMembers()
    {
        World world = createDefaultQuery(false).isP2p().next();
        return hop(world);
    }

    /**
     * Hops to the next members world with an ID lower than the current and not skill total, not pvp, main game.
     * If you're on the lowest world already, it hops back to the last world in the list
     */
    public static StepHandler hopPreviousMembers()
    {
        World world = createDefaultQuery(false).isP2p().previous();
        return hop(world);
    }

    /**
     * Hops to the next free to play world with an ID higher than the current and not skill total, not pvp, main game.
     * If you're on the highest world already, it hops back to the first world in the list
     */
    public static StepHandler hopNextF2p()
    {
        World world = createDefaultQuery(false).isF2p().next();
        return hop(world);
    }

    /**
     * Hops to the next free to play world with an ID lower than the current and not skill total, not pvp, main game.
     * If you're on the lowest world already, it hops back to the last world in the list
     */
    public static StepHandler hopPreviousF2p()
    {
        World world = createDefaultQuery(false).isF2p().previous();
        return hop(world);
    }

    /**
     * Hop to a specific world by its ID
     * @param worldId The ID of the world to hop to
     */
    public static StepHandler hop(int worldId)
    {
        WorldResult worldResult = Static.getInjector().getInstance(WorldService.class).getWorlds();
        if (worldResult == null)
            return HandlerBuilder.blank();
        // Don't try to hop if the world doesn't exist
        World world = worldResult.findWorld(worldId);
        if (world == null)
        {
            return HandlerBuilder.blank();
        }
        return hop(world);
    }

    /**
     * Hop to a specific world
     * @param world The world to hop to
     */
    public static StepHandler hop(World world)
    {
        Client client = Static.getClient();
        return HandlerBuilder.get()
                .add(0, () -> {
                    if (client.getGameState() == GameState.LOGIN_SCREEN)
                    {
                        final net.runelite.api.World rsWorld = client.createWorld();
                        rsWorld.setActivity(world.getActivity());
                        rsWorld.setAddress(world.getAddress());
                        rsWorld.setId(world.getId());
                        rsWorld.setPlayerCount(world.getPlayers());
                        rsWorld.setLocation(world.getLocation());
                        rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

                        client.changeWorld(rsWorld);
                        client.hopToWorld(rsWorld);
                        return HandlerBuilder.END_EXECUTION;
                    }
                    if (client.getWidget(InterfaceID.Worldswitcher.BUTTONS) == null) {
                        WidgetAPI.interact(1, InterfaceID.Logout.WORLD_SWITCHER, -1);
                        return 1;
                    }
                    WidgetAPI.interact(1, InterfaceID.Worldswitcher.BUTTONS, world.getId(), -1);
                    return 2;
                })
                .add(1, () -> {
                    WidgetAPI.interact(1, InterfaceID.Worldswitcher.BUTTONS, world.getId(), -1);
                    return 2;
                })
                .add(2, () -> {
                    if (client.getWidget(InterfaceID.Objectbox.UNIVERSE) != null) {
                        DialogueAPI.resumePause(InterfaceID.Objectbox.UNIVERSE, 1);
                        return HandlerBuilder.END_EXECUTION;
                    }
                    return 3;
                })
                .addDelay(3, 1)
                .build();
    }

    /**
     * Check if the current world is a members world
     * @return True if the current world is a members world, false otherwise
     */
    public static boolean inMembersWorld() {
        return getCurrentWorld().getTypes().contains(WorldType.MEMBERS);
    }
}
