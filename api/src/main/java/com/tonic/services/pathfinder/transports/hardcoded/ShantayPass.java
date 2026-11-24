package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.requirements.OtherRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

import java.util.function.Consumer;

/**
 * The Shantay Pass into the desert, buying a pass from Shantay first when the player holds none.
 */
public final class ShantayPass
{
    private static final WorldPoint SOURCE = new WorldPoint(3303, 3124, 0);
    private static final WorldPoint DESTINATION = new WorldPoint(3303, 3115, 0);
    private static final int DURATION = 2;

    private ShantayPass()
    {
    }

    /**
     * Adds the way south through the pass.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        Requirements requirements = new Requirements();
        requirements.addRequirement(new OtherRequirement(() -> InventoryAPI.count(ItemID.COINS) > 10 || InventoryAPI.contains(ItemID.SHANTAY_PASS)));
        add.accept(new CodeTransport("Shantay Pass", SOURCE, DESTINATION, DURATION, requirements, ShantayPass::pass));
    }

    private static StepHandler pass()
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    if (!InventoryAPI.contains(ItemID.SHANTAY_PASS))
                    {
                        NpcEx npc = new NpcQuery()
                                .withIds(NpcID.SHANTAY)
                                .within(SOURCE, 10)
                                .first();
                        NpcAPI.interact(npc, "Buy-pass");
                        return 1;
                    }
                    TileObjectEx object = new TileObjectQuery()
                            .withId(ObjectID.SHANTAY_PASS_HENGE_DOORWAY)
                            .first();
                    TileObjectAPI.interact(object, 0);
                    return 2;
                })
                .add(1, () ->
                {
                    TileObjectEx object = new TileObjectQuery()
                            .withId(ObjectID.SHANTAY_PASS_HENGE_DOORWAY)
                            .first();
                    TileObjectAPI.interact(object, 0);
                })
                .add(2, () ->
                {
                    if (WidgetAPI.isVisible(InterfaceID.CwsWarning10.WARN1) && WidgetAPI.getText(InterfaceID.CwsWarning10.WARN1).equalsIgnoreCase("Proceed regardless"))
                    {
                        WidgetAPI.interact(1, InterfaceID.CwsWarning10.WARN1, -1, -1);
                    }
                })
                .addDelay(3, 2)
                .build();
    }
}
