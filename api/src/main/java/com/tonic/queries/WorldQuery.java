package com.tonic.queries;

import com.tonic.Static;
import com.tonic.queries.abstractions.AbstractQuery;
import com.tonic.util.TextUtil;
import net.runelite.client.game.WorldService;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldType;
import org.apache.commons.lang3.ArrayUtils;
import java.util.Comparator;
import java.util.Objects;

/**
 * A query builder for filtering and sorting game worlds.
 */
public class WorldQuery extends AbstractQuery<World, WorldQuery>
{
    /**
     * Initializes the WorldQuery with the list of available worlds from the WorldService.
     */
    public WorldQuery() {
        super(Objects.requireNonNull(Static.getInjector().getInstance(WorldService.class).getWorlds()).getWorlds());
    }

    /**
     * Filters the worlds to include only free-to-play worlds.
     *
     * @return WorldQuery
     */
    public WorldQuery isF2p()
    {
        return keepIf(w -> !w.getTypes().contains(WorldType.MEMBERS));
    }

    /**
     * Filters the worlds to include only player-vs-player worlds.
     *
     * @return WorldQuery
     */
    public WorldQuery isP2p()
    {
        return keepIf(w -> w.getTypes().contains(WorldType.MEMBERS));
    }

    /**
     * Filters the worlds to include only members worlds.
     *
     * @return WorldQuery
     */
    public WorldQuery withId(int... id)
    {
        return removeIf(w -> !ArrayUtils.contains(id, w.getId()));
    }

    /**
     * Filters the worlds to include only those with the specified activity.
     *
     * @param activity The activity to filter by.
     * @return WorldQuery
     */
    public WorldQuery withActivity(String activity)
    {
        return keepIf(w -> w.getActivity() != null && TextUtil.sanitize(w.getActivity().toLowerCase()).contains(activity.toLowerCase()));
    }

    /**
     * Filters the worlds to exclude those with the specified activity.
     *
     * @param activity The activity to exclude.
     * @return WorldQuery
     */
    public WorldQuery withOutActivity(String activity)
    {
        return removeIf(world -> TextUtil.sanitize(world.getActivity().toLowerCase()).contains(activity.toLowerCase()));
    }

    /**
     * Filters the worlds to include only those with the specified types.
     *
     * @param types The types to filter by.
     * @return WorldQuery
     */
    public WorldQuery withTypes(WorldType... types)
    {
        return keepIf(w -> w.getTypes() != null && w.getTypes().stream().anyMatch(t -> ArrayUtils.contains(types, t)));
    }

    /**
     * Filters the worlds to include only those in the specified regions.
     *
     * @param region The regions to filter by.
     * @return WorldQuery
     */
    public WorldQuery withRegion(WorldRegion... region)
    {
        return keepIf(w -> w.getRegion() != null && ArrayUtils.contains(region, w.getRegion()));
    }

    /**
     * Filters the worlds to include only those with a player count within the specified range.
     *
     * @param min The minimum player count.
     * @param max The maximum player count.
     * @return WorldQuery
     */
    public WorldQuery withPlayerCount(int min, int max)
    {
        return keepIf(w -> w.getPlayers() >= min && w.getPlayers() <= max);
    }

    /**
     * Filters the worlds to include only those with the specified player count.
     *
     * @param count The exact player count to filter by.
     * @return WorldQuery
     */
    public WorldQuery withPlayerCount(int count)
    {
        return keepIf(w -> w.getPlayers() == count);
    }

    /**
     * Sorts the worlds by their ID in ascending order.
     *
     * @return WorldQuery
     */
    public WorldQuery sortByIdAsc()
    {
        return sort(Comparator.comparingInt(World::getId));
    }

    /**
     * Sorts the worlds by their ID in descending order.
     *
     * @return WorldQuery
     */
    public WorldQuery sortByIdDesc()
    {
        return sort(Comparator.comparingInt(World::getId).reversed());
    }

    /**
     * Sorts the worlds by their player count in ascending order.
     *
     * @return WorldQuery
     */
    public WorldQuery sortByPlayerCountAsc()
    {
        return sort(Comparator.comparingInt(World::getPlayers));
    }

    /**
     * Sorts the worlds by their player count in descending order.
     *
     * @return WorldQuery
     */
    public WorldQuery sortByPlayerCountDesc()
    {
        return sort(Comparator.comparingInt(World::getPlayers).reversed());
    }

    /**
     * Filters the worlds to include only those with the specified skill total activity.
     *
     * @return WorldQuery
     */
    public WorldQuery skillTotalWorlds(int total)
    {
        return withActivity(total + " skill total");
    }

    /**
     * Filters the worlds to exclude those with the skill total activity.
     *
     * @return WorldQuery
     */
    public WorldQuery notSkillTotalWorlds()
    {
        return withOutActivity("skill total");
    }

    /**
     * Filters the worlds to exclude those with the PvP activity.
     *
     * @return WorldQuery
     */
    public WorldQuery notPvp()
    {
        return withOutActivity("PvP World");
    }

    /**
     * Filters the worlds to include only main game worlds (excluding special game modes).
     *
     * @return WorldQuery
     */
    public WorldQuery isMainGame()
    {
        return withOutActivity("skill total").withOutActivity("Fresh").withOutActivity("Deadman").withOutActivity("PvP").withOutActivity("Beta").withOutActivity("Leagues");
    }
}
