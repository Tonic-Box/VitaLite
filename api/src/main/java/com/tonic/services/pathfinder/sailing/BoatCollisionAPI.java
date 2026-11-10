package com.tonic.services.pathfinder.sailing;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The player's boat and the tiles its structure and deck cover, on the boat itself or rounded onto the main world
 */
public class BoatCollisionAPI
{
    private static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_FULL | CollisionDataFlag.BLOCK_MOVEMENT_FLOOR | CollisionDataFlag.BLOCK_MOVEMENT_OBJECT;
    private static final int PADDING = 0x00FFFFFF;

    private static final Map<WorldEntity, Collection<WorldPoint>> boatCollisionCache = new HashMap<>();
    private static final Map<WorldEntity, Collection<WorldPoint>> boatDeckCache = new HashMap<>();
    private static int lastGameTick = -1;

    /**
     * The boat the player is on
     * @return the boat, or null when not on one
     */
    public static WorldEntity getPlayerBoat()
    {
        return Static.invoke(() ->
        {
            Client client = Static.getClient();
            Player player = client.getLocalPlayer();
            if (player == null || player.getWorldView().isTopLevel())
            {
                return null;
            }
            return client.getTopLevelWorldView().worldEntities().byIndex(player.getLocalLocation().getWorldView());
        });
    }

    /**
     * The boat's structure tiles, rounded onto main world tiles
     * @param boat the boat
     * @return the tiles, empty when the boat has no loaded deck
     */
    public static Collection<WorldPoint> getBoatCollisionInMainWorld(WorldEntity boat)
    {
        return Static.invoke(() -> toMainWorld(boat, structureTiles(boat)));
    }

    /**
     * The structure tiles of the player's boat, rounded onto main world tiles and cached for the tick
     * @return the tiles, empty when not on a boat
     */
    public static Collection<WorldPoint> getPlayerBoatCollision()
    {
        return Static.invoke(() ->
        {
            WorldEntity boat = getPlayerBoat();
            return boat == null ? Collections.emptyList() : getBoatCollisionCached(boat);
        });
    }

    /**
     * The boat's structure tiles, rounded onto main world tiles and cached for the tick
     * @param boat the boat
     * @return the tiles
     */
    public static Collection<WorldPoint> getBoatCollisionCached(WorldEntity boat)
    {
        return Static.invoke(() ->
        {
            expireCache();
            return boatCollisionCache.computeIfAbsent(boat, BoatCollisionAPI::getBoatCollisionInMainWorld);
        });
    }

    /**
     * The boat's walkable deck tiles, rounded onto main world tiles
     * @param boat the boat
     * @return the tiles, empty when the boat has no loaded deck
     */
    public static Collection<WorldPoint> getBoatDeckInMainWorld(WorldEntity boat)
    {
        return Static.invoke(() -> toMainWorld(boat, deckTiles(boat)));
    }

    /**
     * The walkable deck tiles of the player's boat, rounded onto main world tiles and cached for the tick
     * @return the tiles, empty when not on a boat
     */
    public static Collection<WorldPoint> getPlayerBoatDeck()
    {
        return Static.invoke(() ->
        {
            WorldEntity boat = getPlayerBoat();
            return boat == null ? Collections.emptyList() : getBoatDeckCached(boat);
        });
    }

    /**
     * The boat's walkable deck tiles, rounded onto main world tiles and cached for the tick
     * @param boat the boat
     * @return the tiles
     */
    public static Collection<WorldPoint> getBoatDeckCached(WorldEntity boat)
    {
        return Static.invoke(() ->
        {
            expireCache();
            return boatDeckCache.computeIfAbsent(boat, BoatCollisionAPI::getBoatDeckInMainWorld);
        });
    }

    /**
     * The boat's walkable deck tiles on the boat itself, which project at the boat's real position and angle
     * @param boat the boat
     * @return the tiles in the boat's world view, empty when the boat has no loaded deck
     */
    public static List<LocalPoint> getBoatDeckTiles(WorldEntity boat)
    {
        return Static.invoke(() -> deckTiles(boat));
    }

    /**
     * The walkable deck tiles of the player's boat on the boat itself
     * @return the tiles in the boat's world view, empty when not on a boat
     */
    public static List<LocalPoint> getPlayerBoatDeckTiles()
    {
        return Static.invoke(() -> deckTiles(getPlayerBoat()));
    }

    /**
     * The boat's position in the main world
     * @param boat the boat
     * @return the position, or null when the boat is null
     */
    public static LocalPoint getBoatPosition(WorldEntity boat)
    {
        return Static.invoke(() -> boat == null ? null : boat.getLocalLocation());
    }

    /**
     * The boat's tile in the main world
     * @param boat the boat
     * @return the tile, or null when the boat is null
     */
    public static WorldPoint getBoatWorldPoint(WorldEntity boat)
    {
        return Static.invoke(() ->
        {
            LocalPoint local = getBoatPosition(boat);
            return local == null ? null : WorldPoint.fromLocal(Static.getClient(), local);
        });
    }

    /**
     * The tile of the player's boat in the main world
     * @return the tile, or null when not on a boat
     */
    public static WorldPoint getPlayerBoatWorldPoint()
    {
        return Static.invoke(() ->
        {
            WorldEntity boat = getPlayerBoat();
            return boat == null ? null : getBoatWorldPoint(boat);
        });
    }

    private static void expireCache()
    {
        Client client = Static.getClient();
        int tick = client.getTickCount();
        if (tick != lastGameTick)
        {
            boatCollisionCache.clear();
            boatDeckCache.clear();
            lastGameTick = tick;
        }
    }

    private static List<LocalPoint> structureTiles(WorldEntity boat)
    {
        Deck deck = Deck.of(boat);
        if (deck == null)
        {
            return Collections.emptyList();
        }

        List<LocalPoint> tiles = new ArrayList<>();
        for (int x = 0; x < deck.sizeX; x++)
        {
            for (int y = 0; y < deck.sizeY; y++)
            {
                if (deck.blocked[x][y])
                {
                    tiles.add(LocalPoint.fromScene(x, y, deck.view));
                }
            }
        }
        return tiles;
    }

    private static List<LocalPoint> deckTiles(WorldEntity boat)
    {
        Deck deck = Deck.of(boat);
        if (deck == null)
        {
            return Collections.emptyList();
        }

        boolean[][] outside = deck.outside();

        List<LocalPoint> tiles = new ArrayList<>();
        for (int x = 0; x < deck.sizeX; x++)
        {
            for (int y = 0; y < deck.sizeY; y++)
            {
                if (!outside[x][y] && !deck.blocked[x][y] && !deck.padding[x][y])
                {
                    tiles.add(LocalPoint.fromScene(x, y, deck.view));
                }
            }
        }
        return tiles;
    }

    private static Collection<WorldPoint> toMainWorld(WorldEntity boat, List<LocalPoint> tiles)
    {
        Client client = Static.getClient();
        List<WorldPoint> points = new ArrayList<>(tiles.size());
        for (LocalPoint tile : tiles)
        {
            LocalPoint main = boat.transformToMainWorld(tile);
            if (main != null)
            {
                points.add(WorldPoint.fromLocal(client, main));
            }
        }
        return points;
    }

    private static final class Deck
    {
        private final WorldView view;
        private final int sizeX;
        private final int sizeY;
        private final boolean[][] blocked;
        private final boolean[][] padding;

        private Deck(WorldView view, int sizeX, int sizeY, boolean[][] blocked, boolean[][] padding)
        {
            this.view = view;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.blocked = blocked;
            this.padding = padding;
        }

        private static Deck of(WorldEntity boat)
        {
            WorldView view = boat == null ? null : boat.getWorldView();
            CollisionData[] maps = view == null ? null : view.getCollisionMaps();
            int plane = view == null ? -1 : view.getPlane();
            if (maps == null || plane < 0 || plane >= maps.length || maps[plane] == null)
            {
                return null;
            }

            int[][] flags = maps[plane].getFlags();
            int sizeX = Math.min(view.getSizeX(), flags.length - 1);
            int sizeY = Math.min(view.getSizeY(), flags[0].length - 1);
            boolean[][] blocked = new boolean[sizeX][sizeY];
            boolean[][] padding = new boolean[sizeX][sizeY];
            for (int x = 0; x < sizeX; x++)
            {
                for (int y = 0; y < sizeY; y++)
                {
                    int flag = flags[x + 1][y + 1];
                    padding[x][y] = flag == PADDING;
                    blocked[x][y] = !padding[x][y] && (flag & BLOCKED) != 0;
                }
            }
            return new Deck(view, sizeX, sizeY, blocked, padding);
        }

        private boolean[][] outside()
        {
            boolean[][] outside = new boolean[sizeX][sizeY];
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            for (int x = 0; x < sizeX; x++)
            {
                for (int y = 0; y < sizeY; y++)
                {
                    if ((x == 0 || y == 0 || x == sizeX - 1 || y == sizeY - 1) && open(x, y))
                    {
                        outside[x][y] = true;
                        queue.add(new int[]{x, y});
                    }
                }
            }

            while (!queue.isEmpty())
            {
                int[] tile = queue.poll();
                spread(outside, queue, tile[0] - 1, tile[1]);
                spread(outside, queue, tile[0] + 1, tile[1]);
                spread(outside, queue, tile[0], tile[1] - 1);
                spread(outside, queue, tile[0], tile[1] + 1);
            }
            return outside;
        }

        private void spread(boolean[][] outside, ArrayDeque<int[]> queue, int x, int y)
        {
            if (x >= 0 && y >= 0 && x < sizeX && y < sizeY && !outside[x][y] && open(x, y))
            {
                outside[x][y] = true;
                queue.add(new int[]{x, y});
            }
        }

        private boolean open(int x, int y)
        {
            return !blocked[x][y];
        }
    }
}
