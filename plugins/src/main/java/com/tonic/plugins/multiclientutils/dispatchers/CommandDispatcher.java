package com.tonic.plugins.multiclientutils.dispatchers;

import com.tonic.Logger;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.game.WorldsAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.plugins.multiclientutils.model.MultiMessage;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.PlayerQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.CatFacts;
import com.tonic.services.pathfinder.Walker;
import com.tonic.util.MessageUtil;
import com.tonic.util.ThreadPool;
import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CommandDispatcher
{
    @Getter
    private static final Map<String,Instant> players = new HashMap<>();

    public static void processSelf(MultiMessage message)
    {
        int id;
        int action;
        WorldPoint worldPoint;
        switch (message.getCommand())
        {
            case "PATHFIND":
                WorldPoint dest = WorldPointUtil.fromCompressed(message.getInt(0));
                int world = message.getInt(1);
                ThreadPool.submit(() -> {
                    WorldsAPI.hop(world).execute();
                    Delays.tick();
                    Walker.walkTo(dest);
                });
                break;
            case "WALK":
                worldPoint = WorldPointUtil.fromCompressed(message.getInt(0));
                MovementAPI.walkToWorldPoint(worldPoint);
                break;
            case "NPC":
                id = message.getInt(0);
                action = message.getInt(1);
                NpcEx npc = new NpcQuery()
                        .withIndex(id)
                        .first();
                if(npc == null)
                    break;
                NpcAPI.interact(npc, action);
                break;
            case "PLAYER":
                id = message.getInt(0);
                action = message.getInt(1);
                PlayerEx player = new PlayerQuery()
                        .keepIf(p -> p.getIndex() == id)
                        .first();
                if(player == null)
                    break;
                PlayerAPI.interact(player, action);
                break;
            case "OBJECT":
                id = message.getInt(0);
                action = message.getInt(1);
                worldPoint = WorldPointUtil.fromCompressed(message.getInt(2));
                TileObjectEx object = new TileObjectQuery()
                        .withId(id)
                        .within(worldPoint, 1)
                        .first();
                if(object == null)
                    break;
                TileObjectAPI.interact(object, action);
                break;
        }
    }

    public static void process(MultiMessage message)
    {
        PlayerEx sender = PlayerAPI.search()
                .withName(message.getSender())
                .first();
        switch (message.getCommand())
        {
            case "DESPAWN":
                players.remove(message.getSender());
                return;
            case "PING":
                players.put(message.getSender(), Instant.now());
                return;
            case "PATHFIND":
                WorldPoint dest = WorldPointUtil.fromCompressed(message.getInt(0));
                int world = message.getInt(1);
                ThreadPool.submit(() -> {
                    WorldsAPI.hop(world).execute();
                    Delays.tick();
                    Walker.walkTo(dest);
                });
                return;
            case "CATFACTS":
                String fact = CatFacts.get(60);
                MessageUtil.sendPublicChatMessage(fact);
                return;
        }

        if(sender == null)
        {
            Logger.warn("Player '" + message.getSender() + "' is unavailable.");
            return;
        }

        int id;
        int action;
        WorldPoint worldPoint;
        switch (message.getCommand())
        {
            case "FOLLOW":
                PlayerAPI.interact(sender, 2);
                return;
            case "DD":
                MovementAPI.walkToWorldPoint(sender.getWorldPoint());
                return;
            case "SCATTER":
                int x = sender.getWorldPoint().getX() - 6;
                int y = sender.getWorldPoint().getY() - 6;
                x += ThreadLocalRandom.current().nextInt(0, 13);
                y += ThreadLocalRandom.current().nextInt(0, 13);
                MovementAPI.walkToWorldPoint(x, y);
                return;
            case "WALK":
                worldPoint = WorldPointUtil.fromCompressed(message.getInt(0));
                MovementAPI.walkToWorldPoint(worldPoint);
                return;
            case "NPC":
                id = message.getInt(0);
                action = message.getInt(1);
                NpcEx npc = new NpcQuery()
                        .withIndex(id)
                        .first();
                if(npc == null)
                    break;
                NpcAPI.interact(npc, action);
                break;
            case "PLAYER":
                id = message.getInt(0);
                action = message.getInt(1);
                PlayerEx player = new PlayerQuery()
                        .keepIf(p -> p.getIndex() == id)
                        .first();
                if(player == null)
                    break;
                PlayerAPI.interact(player, action);
                break;
            case "OBJECT":
                id = message.getInt(0);
                action = message.getInt(1);
                worldPoint = WorldPointUtil.fromCompressed(message.getInt(2));
                TileObjectEx object = new TileObjectQuery()
                        .withId(id)
                        .within(worldPoint, 1)
                        .first();
                if(object == null)
                    break;
                TileObjectAPI.interact(object, action);
                break;
            default:
                Logger.error("[ExtendedMenu] Unrecognized command '" + message.getCommand() + "'");
        }
    }
}
