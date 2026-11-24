package com.tonic.services.worldselect;

import com.tonic.Static;
import com.tonic.util.ClientConfig;
import lombok.Getter;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * What the world select shows and how: the worlds, the filters, search, favourites, sort and selection, the filters, sort and favourites saved in VitaLite's settings. Every change builds a new immutable listing for the painter.
 */
final class WorldSelectModel
{
    /**
     * One built listing of the worlds, read by the painter without locking.
     */
    @Getter
    static final class Listing
    {
        private final boolean loaded;
        private final List<World> favourites;
        private final List<World> others;
        private final Map<GameMode, Integer> modeCounts;
        private final Map<WorldRegion, Integer> regionCounts;

        Listing(boolean loaded, List<World> favourites, List<World> others, Map<GameMode, Integer> modeCounts, Map<WorldRegion, Integer> regionCounts)
        {
            this.loaded = loaded;
            this.favourites = favourites;
            this.others = others;
            this.modeCounts = modeCounts;
            this.regionCounts = regionCounts;
        }

        /**
         * @return how many worlds are listed
         */
        int size()
        {
            return favourites.size() + others.size();
        }
    }

    private final IntUnaryOperator pings;
    private final WorldSorter sorter = new WorldSorter();
    private final Set<GameMode> modes = EnumSet.noneOf(GameMode.class);
    private final Set<WorldRegion> regions = EnumSet.noneOf(WorldRegion.class);
    private final Set<Integer> favourites = new LinkedHashSet<>();

    private WorldResult source;
    private List<World> worlds = Collections.emptyList();
    private boolean showMembers = true;
    private boolean showFree = true;
    private boolean compact;
    private String search = "";
    private int selected = -1;
    private int current = -1;

    private volatile Listing listing = new Listing(false, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());

    /**
     * @param pings the ping to a world by its number, or -1 when not known
     */
    WorldSelectModel(IntUnaryOperator pings)
    {
        this.pings = pings;
    }

    /**
     * @return the latest listing
     */
    Listing listing()
    {
        return listing;
    }

    /**
     * Loads the saved filters, sort and favourites and selects the world the client is on, as each opening does.
     *
     * @param currentWorld the client's world
     */
    synchronized void opened(int currentWorld)
    {
        ClientConfig config = Static.getVitaConfig();
        modes.clear();
        for (String name : split(config.getWorldSelectModes()))
        {
            GameMode mode = enumNamed(GameMode.class, name);
            if (mode != null)
            {
                modes.add(mode);
            }
        }
        regions.clear();
        for (String name : split(config.getWorldSelectRegions()))
        {
            WorldRegion region = enumNamed(WorldRegion.class, name);
            if (region != null)
            {
                regions.add(region);
            }
        }
        favourites.clear();
        for (String id : split(config.getWorldSelectFavourites()))
        {
            try
            {
                favourites.add(Integer.parseInt(id));
            }
            catch (NumberFormatException ignored)
            {
            }
        }
        showMembers = config.shouldWorldSelectShowMembers();
        showFree = config.shouldWorldSelectShowFree();
        compact = config.isWorldSelectCompact();
        sorter.load(config.getWorldSelectSort());
        search = "";
        current = currentWorld;
        selected = currentWorld;
        rebuild();
    }

    /**
     * Takes a new world list from RuneLite, when it is not the one already shown.
     *
     * @param result RuneLite's latest world list, or null while it has none
     */
    synchronized void offer(WorldResult result)
    {
        if (result == null || result == source || result.getWorlds() == null)
        {
            return;
        }
        source = result;
        worlds = new ArrayList<>(result.getWorlds());
        rebuild();
    }

    /**
     * @param mode a game mode to show or stop showing; with none ticked every mode shows
     */
    synchronized void toggleMode(GameMode mode)
    {
        toggle(modes, mode);
        changed();
    }

    /**
     * @param region a region to show or stop showing; with none ticked every region shows
     */
    synchronized void toggleRegion(WorldRegion region)
    {
        toggle(regions, region);
        changed();
    }

    /**
     * Shows or hides members worlds.
     */
    synchronized void toggleMembers()
    {
        showMembers = !showMembers;
        changed();
    }

    /**
     * Shows or hides free worlds.
     */
    synchronized void toggleFree()
    {
        showFree = !showFree;
        changed();
    }

    /**
     * Switches between rows and the compact grid.
     */
    synchronized void toggleCompact()
    {
        compact = !compact;
        changed();
    }

    /**
     * @param key the column picked, sorted by first or flipped when it already is
     */
    synchronized void pickSort(WorldSorter.Key key)
    {
        sorter.pick(key);
        changed();
    }

    /**
     * @param world a world to add to or take off the favourites
     */
    synchronized void toggleFavourite(int world)
    {
        toggle(favourites, world);
        changed();
    }

    /**
     * @param text what to search worlds for: a number the world's starts with, or text in its activity, region or mode
     */
    synchronized void setSearch(String text)
    {
        search = text;
        rebuild();
    }

    /**
     * Clears the filters and search and goes back to the default sort; favourites stay.
     */
    synchronized void reset()
    {
        modes.clear();
        regions.clear();
        showMembers = true;
        showFree = true;
        search = "";
        sorter.reset();
        changed();
    }

    /**
     * @param world the world selected
     */
    synchronized void select(int world)
    {
        selected = world;
    }

    synchronized String getSearch()
    {
        return search;
    }

    synchronized int getSelected()
    {
        return selected;
    }

    synchronized int getCurrent()
    {
        return current;
    }

    synchronized boolean isCompact()
    {
        return compact;
    }

    synchronized boolean showsMembers()
    {
        return showMembers;
    }

    synchronized boolean showsFree()
    {
        return showFree;
    }

    synchronized boolean isTicked(GameMode mode)
    {
        return modes.contains(mode);
    }

    synchronized boolean isTicked(WorldRegion region)
    {
        return regions.contains(region);
    }

    synchronized boolean isFavourite(int world)
    {
        return favourites.contains(world);
    }

    synchronized boolean hasFilters()
    {
        return !modes.isEmpty() || !regions.isEmpty() || !showMembers || !showFree || !search.isEmpty();
    }

    synchronized WorldSorter.Key sortKey()
    {
        return sorter.primary();
    }

    synchronized boolean sortAscending()
    {
        return sorter.isAscending(sorter.primary());
    }

    /**
     * @param id a world number
     * @return the world, or null when RuneLite's list has none by that number
     */
    synchronized World world(int id)
    {
        for (World world : worlds)
        {
            if (world.getId() == id)
            {
                return world;
            }
        }
        return null;
    }

    /**
     * Sorts and filters the worlds again, as after their pings change.
     */
    synchronized void rebuild()
    {
        Map<GameMode, Integer> modeCounts = new EnumMap<>(GameMode.class);
        Map<WorldRegion, Integer> regionCounts = new EnumMap<>(WorldRegion.class);
        List<World> shownFavourites = new ArrayList<>();
        List<World> shownOthers = new ArrayList<>();
        String query = search.trim().toLowerCase(Locale.ROOT);
        for (World world : worlds)
        {
            GameMode mode = GameMode.of(world);
            modeCounts.merge(mode, 1, Integer::sum);
            if (world.getRegion() != null)
            {
                regionCounts.merge(world.getRegion(), 1, Integer::sum);
            }
            if (!shows(world, mode, query))
            {
                continue;
            }
            (favourites.contains(world.getId()) ? shownFavourites : shownOthers).add(world);
        }
        shownFavourites.sort(sorter.comparator(pings));
        shownOthers.sort(sorter.comparator(pings));
        listing = new Listing(source != null, Collections.unmodifiableList(shownFavourites), Collections.unmodifiableList(shownOthers), Collections.unmodifiableMap(modeCounts), Collections.unmodifiableMap(regionCounts));
    }

    private boolean shows(World world, GameMode mode, String query)
    {
        boolean members = world.getTypes().contains(WorldType.MEMBERS);
        if (members ? !showMembers : !showFree)
        {
            return false;
        }
        if (!modes.isEmpty() && !modes.contains(mode))
        {
            return false;
        }
        if (!regions.isEmpty() && !regions.contains(world.getRegion()))
        {
            return false;
        }
        return query.isEmpty() || matches(world, mode, query);
    }

    private static boolean matches(World world, GameMode mode, String query)
    {
        if (String.valueOf(world.getId()).startsWith(query))
        {
            return true;
        }
        if (WorldSorter.hasActivity(world) && world.getActivity().toLowerCase(Locale.ROOT).contains(query))
        {
            return true;
        }
        if (world.getRegion() != null && Regions.name(world.getRegion()).toLowerCase(Locale.ROOT).contains(query))
        {
            return true;
        }
        return mode.getDisplayName().toLowerCase(Locale.ROOT).contains(query);
    }

    private void changed()
    {
        rebuild();
        ClientConfig config = Static.getVitaConfig();
        config.setWorldSelectModes(join(modes));
        config.setWorldSelectRegions(join(regions));
        config.setWorldSelectFavourites(join(favourites));
        config.setWorldSelectShowMembers(showMembers);
        config.setWorldSelectShowFree(showFree);
        config.setWorldSelectCompact(compact);
        config.setWorldSelectSort(sorter.save());
    }

    private static <T> void toggle(Set<T> set, T value)
    {
        if (!set.remove(value))
        {
            set.add(value);
        }
    }

    private static String join(Set<?> values)
    {
        StringBuilder text = new StringBuilder();
        for (Object value : values)
        {
            if (text.length() > 0)
            {
                text.append(',');
            }
            text.append(value instanceof Enum ? ((Enum<?>) value).name() : value);
        }
        return text.toString();
    }

    private static String[] split(String text)
    {
        return text == null || text.isEmpty() ? new String[0] : text.split(",");
    }

    private static <E extends Enum<E>> E enumNamed(Class<E> type, String name)
    {
        for (E value : type.getEnumConstants())
        {
            if (value.name().equals(name))
            {
                return value;
            }
        }
        return null;
    }
}
