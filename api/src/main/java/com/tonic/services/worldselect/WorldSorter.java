package com.tonic.services.worldselect;

import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

/**
 * The world select's sort: keys tried in order, each ascending or descending; picking a key moves it to the front and leaves the rest in their order behind it, as the client's own does.
 */
final class WorldSorter
{
    /**
     * What worlds can be sorted by.
     */
    enum Key
    {
        WORLD,
        PLAYERS,
        REGION,
        MEMBERS,
        ACTIVITY,
        PING
    }

    private static final int PLAYERS_OFFLINE_ASCENDING = 2001;
    private static final String NO_ACTIVITY = "-";

    private final List<Key> order = new ArrayList<>();
    private final Map<Key, Boolean> ascending = new EnumMap<>(Key.class);

    WorldSorter()
    {
        reset();
    }

    /**
     * Goes back to world number ascending, then players, region, members, activity and ping, all ascending.
     */
    void reset()
    {
        order.clear();
        for (Key key : Key.values())
        {
            order.add(key);
            ascending.put(key, true);
        }
    }

    /**
     * Sorts by a key first: flips its direction when it already comes first, otherwise moves it to the front ascending.
     *
     * @param key the key picked
     */
    void pick(Key key)
    {
        if (order.get(0) == key)
        {
            ascending.put(key, !ascending.get(key));
            return;
        }
        order.remove(key);
        order.add(0, key);
        ascending.put(key, true);
    }

    /**
     * @return the key sorted by first
     */
    Key primary()
    {
        return order.get(0);
    }

    /**
     * @param key a key
     * @return whether it sorts ascending
     */
    boolean isAscending(Key key)
    {
        return ascending.get(key);
    }

    /**
     * @param pings the ping to a world by its number, or -1 when not known
     * @return the order worlds are listed in
     */
    Comparator<World> comparator(IntUnaryOperator pings)
    {
        Comparator<World> comparator = null;
        for (Key key : order)
        {
            Comparator<World> next = compare(key, ascending.get(key), pings);
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        return comparator;
    }

    private static Comparator<World> compare(Key key, boolean up, IntUnaryOperator pings)
    {
        switch (key)
        {
            case WORLD:
                return direction(Comparator.comparingInt(World::getId), up);
            case PLAYERS:
                return direction(Comparator.comparingInt(world -> world.getPlayers() < 0 && up ? PLAYERS_OFFLINE_ASCENDING : world.getPlayers()), up);
            case REGION:
                return direction(Comparator.comparingInt(world -> world.getRegion() == null ? Integer.MAX_VALUE : world.getRegion().getId()), up);
            case MEMBERS:
                return direction(Comparator.comparingInt(world -> world.getTypes().contains(WorldType.MEMBERS) ? 1 : 0), up);
            case ACTIVITY:
                return lastWhenMissing(world -> !hasActivity(world), direction(Comparator.comparing(world -> world.getActivity().toLowerCase()), up));
            case PING:
                return lastWhenMissing(world -> pings.applyAsInt(world.getId()) < 0, direction(Comparator.comparingInt(world -> pings.applyAsInt(world.getId())), up));
            default:
                throw new IllegalArgumentException("key " + key + " is not sortable");
        }
    }

    private static Comparator<World> direction(Comparator<World> comparator, boolean up)
    {
        return up ? comparator : comparator.reversed();
    }

    private static Comparator<World> lastWhenMissing(Predicate<World> missing, Comparator<World> present)
    {
        return (a, b) ->
        {
            boolean aMissing = missing.test(a);
            boolean bMissing = missing.test(b);
            if (aMissing || bMissing)
            {
                return Boolean.compare(aMissing, bMissing);
            }
            return present.compare(a, b);
        };
    }

    /**
     * @param world a world
     * @return whether it has an activity to show, the client writing "-" for none
     */
    static boolean hasActivity(World world)
    {
        String activity = world.getActivity();
        return activity != null && !activity.isEmpty() && !NO_ACTIVITY.equals(activity);
    }

    /**
     * @return the order and directions, saved as "KEY:A" or "KEY:D" entries joined by commas
     */
    String save()
    {
        StringBuilder text = new StringBuilder();
        for (Key key : order)
        {
            if (text.length() > 0)
            {
                text.append(',');
            }
            text.append(key.name()).append(':').append(ascending.get(key) ? 'A' : 'D');
        }
        return text.toString();
    }

    /**
     * Restores a saved order; unknown entries are skipped and missing keys keep their default place after the saved ones.
     *
     * @param text what save gave, or null for the default
     */
    void load(String text)
    {
        reset();
        if (text == null || text.isEmpty())
        {
            return;
        }
        List<Key> saved = new ArrayList<>();
        for (String entry : text.split(","))
        {
            String[] parts = entry.split(":");
            if (parts.length != 2)
            {
                continue;
            }
            Key key = keyNamed(parts[0]);
            if (key == null || saved.contains(key))
            {
                continue;
            }
            saved.add(key);
            ascending.put(key, !"D".equals(parts[1]));
        }
        order.removeAll(saved);
        order.addAll(0, saved);
    }

    private static Key keyNamed(String name)
    {
        for (Key key : Key.values())
        {
            if (key.name().equals(name))
            {
                return key;
            }
        }
        return null;
    }
}
