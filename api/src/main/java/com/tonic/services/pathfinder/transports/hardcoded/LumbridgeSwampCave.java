package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.requirements.OtherRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

import java.util.function.Consumer;

/**
 * The Lumbridge Swamp cave entrance, tying a rope to it first when it has none.
 */
public final class LumbridgeSwampCave
{
    private static final WorldPoint SOURCE = new WorldPoint(3169, 3173, 0);
    private static final WorldPoint DESTINATION = new WorldPoint(3167, 9573, 0);
    private static final int DURATION = 1;

    private LumbridgeSwampCave()
    {
    }

    /**
     * Adds the way down into the swamp caves.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        Requirements requirements = new Requirements();
        requirements.addRequirement(new OtherRequirement(() -> roped() || InventoryAPI.contains(ItemID.ROPE)));
        add.accept(new CodeTransport("Lumbridge Swamp cave", SOURCE, DESTINATION, DURATION, requirements, LumbridgeSwampCave::enter));
    }

    private static boolean roped()
    {
        return VarAPI.getVar(VarbitID.SWAMP_CAVES_ROPED_ENTRANCE) == 1;
    }

    private static StepHandler enter()
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    if (!roped() && InventoryAPI.contains(ItemID.ROPE))
                    {
                        ItemEx rope = InventoryAPI.getItem(ItemID.ROPE);
                        TileObjectEx object = new TileObjectQuery()
                                .withId(ObjectID.GOBLIN_CAVE_ENTRANCE)
                                .first();
                        InventoryAPI.useOn(rope, object);
                        return 1;
                    }
                    TileObjectEx object = new TileObjectQuery()
                            .withId(ObjectID.GOBLIN_CAVE_ENTRANCE)
                            .first();
                    TileObjectAPI.interact(object, 0);
                    return 2;
                })
                .add(1, () ->
                {
                    TileObjectEx object = new TileObjectQuery()
                            .withId(ObjectID.GOBLIN_CAVE_ENTRANCE)
                            .first();
                    TileObjectAPI.interact(object, 0);
                    return 2;
                })
                .addDelay(2, 2)
                .build();
    }
}
