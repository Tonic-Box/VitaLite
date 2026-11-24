package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.data.FairyRing;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.gameval.InterfaceID;
import org.apache.commons.lang3.ArrayUtils;

import java.util.function.Consumer;

/**
 * The fairy ring network: every ring to every other, Zanaris by its own option and the rest by dialling the code.
 */
public final class FairyRings
{
    private static final String NAME = "Fairy ring";
    private static final int DURATION = 7;

    private FairyRings()
    {
    }

    /**
     * Adds a ride from every fairy ring to every other.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        for (FairyRing ring : FairyRing.values())
        {
            for (FairyRing destination : FairyRing.values())
            {
                if (ring == destination)
                {
                    continue;
                }

                Requirements requirements = new Requirements();
                requirements.addRequirements(ring.getRequirements().getAll());
                requirements.addRequirements(destination.getRequirements().getAll());
                requirements.addRequirement(new WorldRequirement(true));

                if (destination == FairyRing.ZANARIS)
                {
                    add.accept(new CodeTransport(NAME, ring.getLocation(), destination.getLocation(), DURATION, requirements, true, FairyRings::toZanaris));
                    continue;
                }

                add.accept(new CodeTransport(NAME, ring.getLocation(), destination.getLocation(), DURATION, requirements, true, () -> dial(destination)));
            }
        }
    }

    private static StepHandler toZanaris()
    {
        return HandlerBuilder.get()
                .add(0, () -> wieldFairyStaff(1))
                .add(1, () ->
                {
                    TileObjectEx current = new TileObjectQuery()
                            .withName("Fairy ring")
                            .first();
                    TileObjectAPI.interact(current, "Zanaris");
                    return 2;
                })
                .addDelay(2, 7)
                .build();
    }

    private static StepHandler dial(FairyRing destination)
    {
        return HandlerBuilder.get()
                .add(0, () -> wieldFairyStaff(1))
                .add(1, () ->
                {
                    TileObjectEx current = new TileObjectQuery()
                            .withName("Fairy ring")
                            .nearest();
                    TileObjectAPI.interact(current, "Configure");
                    return 2;
                })
                .addDelayUntil(2, () -> WidgetAPI.get(InterfaceID.Fairyrings.CONFIRM) != null)
                .addDelayUntil(3, 20, destination::travel, () ->
                {
                })
                .addDelay(4, 7)
                .add(5, () -> MovementAPI.walkToWorldPoint(destination.getLocation()))
                .addDelay(6, 1)
                .build();
    }

    private static int wieldFairyStaff(int next)
    {
        if (FairyRing.needsStaff() && !EquipmentAPI.isEquipped(i -> ArrayUtils.contains(FairyRing.STAFFS, i.getId())))
        {
            ItemEx staff = InventoryAPI.getItem(i -> ArrayUtils.contains(FairyRing.STAFFS, i.getId()));
            if (staff != null)
            {
                EquipmentAPI.equip(staff.getId());
            }
        }
        return next;
    }
}
