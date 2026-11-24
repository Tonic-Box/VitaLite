package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.TileObjectAPI;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.requirements.QuestRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.data.SpiritTree;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;

import java.util.function.Consumer;

/**
 * The spirit tree network: every tree to every other, the Gnome Stronghold's left only once The Grand Tree is done.
 */
public final class SpiritTrees
{
    private static final String NAME = "Spirit tree";
    private static final int DURATION = 3;

    private SpiritTrees()
    {
    }

    /**
     * Adds a journey from every spirit tree to every other.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        for (SpiritTree tree : SpiritTree.values())
        {
            for (SpiritTree destination : SpiritTree.values())
            {
                if (tree == destination)
                {
                    continue;
                }

                Requirements requirements = new Requirements();
                requirements.addRequirements(destination.getRequirements().getAll());
                if (tree == SpiritTree.GNOME_STRONGHOLD)
                {
                    requirements.addRequirement(new QuestRequirement(Quest.THE_GRAND_TREE, QuestState.FINISHED));
                }

                add.accept(new CodeTransport(NAME, tree.getLocation(), destination.getLocation(), DURATION, requirements, true, () -> travel(destination)));
            }
        }
    }

    private static StepHandler travel(SpiritTree destination)
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    TileObjectEx current = new TileObjectQuery()
                            .withName("Spirit tree")
                            .first();
                    TileObjectAPI.interact(current, "Travel");
                    return 1;
                })
                .addDelayUntil(1, MenuOptions.MENU_TIMEOUT, () -> MenuOptions.menuSlot(destination.name()) != -1, () ->
                {
                })
                .add(2, () -> MenuOptions.resumeMenu(destination.name(), 3))
                .addDelay(3, 4)
                .build();
    }
}
