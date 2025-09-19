package com.tonic.api.game;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.queries.WorldQuery;
import com.tonic.services.ClickManager;
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
     * Hop to a random members world (not skill total, not pvp, main game)
     */
    public static void hopRandomMembers()
    {
        World world = new WorldQuery().isP2p().notSkillTotalWorlds().isMainGame().notPvp().random();
        hop(world);
    }

    /**
     * Hop to a random free to play world (not skill total, not pvp, main game)
     */
    public static void hopRandomF2p()
    {
        World world = new WorldQuery().isF2p().notSkillTotalWorlds().isMainGame().notPvp().random();
        hop(world);
    }

    /**
     * Hop to a specific world by its ID
     * @param worldId The ID of the world to hop to
     */
    public static void hop(int worldId)
    {
        WorldResult worldResult = Static.getInjector().getInstance(WorldService.class).getWorlds();
        if (worldResult == null)
            return;
        // Don't try to hop if the world doesn't exist
        World world = worldResult.findWorld(worldId);
        if (world == null)
        {
            return;
        }

        hop(world);
    }

    /**
     * Hop to a specific world
     * @param world The world to hop to
     */
    public static void hop(World world)
    {
        Client client = Static.getClient();
        TClient tClient = Static.getClient();
        Static.invoke(() -> {
            if (client.getWidget(InterfaceID.Worldswitcher.BUTTONS) == null) {
                ClickManager.click();
                tClient.getPacketWriter().widgetActionPacket(1, 11927555, -1, -1);
            }
            if (client.getWidget(InterfaceID.Objectbox.UNIVERSE) != null) {
                ClickManager.click();
                tClient.getPacketWriter().resumePauseWidgetPacket(12648448, 1);
            }
            final net.runelite.api.World rsWorld = client.createWorld();
            rsWorld.setActivity(world.getActivity());
            rsWorld.setAddress(world.getAddress());
            rsWorld.setId(world.getId());
            rsWorld.setPlayerCount(world.getPlayers());
            rsWorld.setLocation(world.getLocation());
            rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

            if (client.getGameState() == GameState.LOGIN_SCREEN)
            {
                client.changeWorld(rsWorld);
            }
            client.hopToWorld(rsWorld);
        });
    }

    public static boolean inMembersWorld() {
        return getCurrentWorld().getTypes().contains(WorldType.MEMBERS);
    }
}
