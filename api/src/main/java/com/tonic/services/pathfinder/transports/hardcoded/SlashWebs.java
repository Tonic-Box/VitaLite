package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.requirements.OtherRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.Distance;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Consumer;

import static com.tonic.services.pathfinder.teleports.MovementConstants.SLASH_ITEMS;
import static com.tonic.services.pathfinder.teleports.MovementConstants.SLASH_WEB_POINTS;

/**
 * The webs across Wilderness and dungeon passages, slashed with a blade before walking through, both ways.
 */
public final class SlashWebs
{
    private static final String NAME = "Slash web";
    private static final int DURATION = 1;
    private static final int WEB_RADIUS = 5;

    private SlashWebs()
    {
    }

    /**
     * Adds both directions through every web.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        Requirements requirements = new Requirements();
        requirements.addRequirement(new OtherRequirement(SlashWebs::holdsBlade));
        for (Pair<WorldPoint, WorldPoint> pair : SLASH_WEB_POINTS)
        {
            WorldPoint left = pair.getLeft();
            WorldPoint right = pair.getRight();
            add.accept(new CodeTransport(NAME, left, right, DURATION, requirements, () -> slash(left, right)));
            add.accept(new CodeTransport(NAME, right, left, DURATION, requirements, () -> slash(right, left)));
        }
    }

    private static boolean holdsBlade()
    {
        return InventoryAPI.contains(SLASH_ITEMS) || EquipmentAPI.isEquipped(i -> i != null && ArrayUtils.contains(SLASH_ITEMS, i.getId()));
    }

    private static TileObjectEx web(WorldPoint source)
    {
        return new TileObjectQuery()
                .withNameContains("Web")
                .within(source, WEB_RADIUS)
                .withAction("Slash")
                .first();
    }

    private static StepHandler slash(WorldPoint source, WorldPoint destination)
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    TileObjectEx web = web(source);
                    if (web != null)
                    {
                        TileObjectAPI.interact(web, "Slash");
                        return 1;
                    }

                    MovementAPI.walkToWorldPoint(destination);
                    return 4;
                })
                .addDelay(1, 2)
                .addDelayUntil(2, () ->
                {
                    if (web(source) == null)
                    {
                        return true;
                    }
                    return PlayerEx.getLocal().isIdle() && !MovementAPI.isMoving();
                })
                .add(3, () ->
                {
                    if (web(source) != null)
                    {
                        return 0;
                    }

                    MovementAPI.walkToWorldPoint(destination);
                    return 4;
                })
                .addDelayUntil(4, () ->
                {
                    WorldPoint worldPoint = PlayerEx.getLocal().getWorldPoint();
                    return Distance.pathDistanceTo(worldPoint, destination) < 2;
                })
                .build();
    }
}
