package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

import java.util.function.Consumer;

/**
 * Crossings made by using an item on an object: Glarial's pebble on her tombstone and a rope across the Waterfall rocks.
 */
public final class ItemOnObjectCrossings
{
    private static final int DURATION = 1;
    private static final int OBJECT_RADIUS = 8;
    private static final WorldPoint GLARIALS_TOMB = new WorldPoint(2555, 9844, 0);
    private static final WorldPoint[] TOMBSTONE_SIDES =
            {
                    new WorldPoint(2557, 3444, 0),
                    new WorldPoint(2557, 3445, 0),
                    new WorldPoint(2558, 3443, 0),
                    new WorldPoint(2559, 3443, 0),
                    new WorldPoint(2560, 3444, 0),
                    new WorldPoint(2560, 3445, 0),
                    new WorldPoint(2558, 3446, 0),
                    new WorldPoint(2559, 3446, 0)
            };

    private ItemOnObjectCrossings()
    {
    }

    /**
     * Adds the way into Glarial's tomb from every side of her tombstone and the two rope crossings to Waterfall island.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        Requirements requirements = new Requirements();
        requirements.addRequirement(new WorldRequirement(true));

        for (WorldPoint side : TOMBSTONE_SIDES)
        {
            add.accept(crossing("Glarial's tombstone", side, GLARIALS_TOMB, ItemID.GLARIALS_PEBBLE_WATERFALL_QUEST, ObjectID.GLARIALS_TOMBSTONE_WATERFALL_QUEST, requirements));
        }

        add.accept(crossing("Crossing rock", new WorldPoint(2512, 3476, 0), new WorldPoint(2513, 3468, 0), ItemID.ROPE, ObjectID.CROSSING_ROCK_WATERFALL_QUEST, requirements));
        add.accept(crossing("Overhanging tree", new WorldPoint(2512, 3466, 0), new WorldPoint(2511, 3463, 0), ItemID.ROPE, ObjectID.OVERHANGING_TREE1_WATERFALL_QUEST, requirements));
    }

    private static CodeTransport crossing(String name, WorldPoint source, WorldPoint destination, int itemId, int objectId, Requirements requirements)
    {
        return new CodeTransport(name, source, destination, DURATION, requirements, () -> useItemOn(source, itemId, objectId));
    }

    private static StepHandler useItemOn(WorldPoint source, int itemId, int objectId)
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    ItemEx item = InventoryAPI.getItem(itemId);
                    if (item == null)
                    {
                        return 0;
                    }

                    TileObjectEx transport = new TileObjectQuery()
                            .withId(objectId)
                            .within(source, OBJECT_RADIUS)
                            .first();
                    if (transport != null)
                    {
                        InventoryAPI.useOn(item, transport);
                        return 1;
                    }
                    return 0;
                })
                .addDelay(1, 1)
                .build();
    }
}
