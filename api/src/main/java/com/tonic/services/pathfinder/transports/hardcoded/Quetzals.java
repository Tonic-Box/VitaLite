package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.queries.NpcQuery;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.data.QuetzalLanding;
import com.tonic.util.TextUtil;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import java.util.function.Consumer;

/**
 * The quetzal network: Renu flies from every landing to every other.
 */
public final class Quetzals
{
    private static final String NAME = "Quetzal";
    private static final int DURATION = 5;
    private static final int MENU_TIMEOUT = 20;

    private Quetzals()
    {
    }

    /**
     * Adds a flight from every quetzal landing to every other.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        for (QuetzalLanding landing : QuetzalLanding.values())
        {
            for (QuetzalLanding destination : QuetzalLanding.values())
            {
                if (landing == destination)
                {
                    continue;
                }

                Requirements requirements = new Requirements();
                requirements.addRequirements(landing.getRequirements().getAll());
                requirements.addRequirements(destination.getRequirements().getAll());

                add.accept(new CodeTransport(NAME, landing.getLocation(), destination.getLocation(), DURATION, requirements, () -> fly(destination)));
            }
        }
    }

    private static StepHandler fly(QuetzalLanding destination)
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    NpcEx renu = new NpcQuery().withName("Renu").withAction("Travel").nearest();
                    if (renu == null)
                    {
                        return HandlerBuilder.END_EXECUTION;
                    }
                    NpcAPI.interact(renu, "Travel");
                    return 1;
                })
                .addDelayUntil(1, MENU_TIMEOUT, () -> quetzalIcon(destination.getName()) != -1, () ->
                {
                })
                .add(2, () ->
                {
                    int icon = quetzalIcon(destination.getName());
                    if (icon == -1)
                    {
                        return HandlerBuilder.END_EXECUTION;
                    }
                    WidgetAPI.interact(1, InterfaceID.QuetzalMenu.ICONS, icon, -1);
                    return 3;
                })
                .addDelay(3, 5)
                .build();
    }

    private static int quetzalIcon(String destination)
    {
        return Static.invoke(() ->
        {
            Widget icons = WidgetAPI.get(InterfaceID.QuetzalMenu.ICONS);
            Widget[] children = icons == null ? null : icons.getChildren();
            if (children == null)
            {
                return -1;
            }
            for (Widget child : children)
            {
                String[] actions = child == null ? null : child.getActions();
                if (actions != null && actions.length > 0 && actions[0] != null && TextUtil.sanitize(actions[0]).equalsIgnoreCase(destination))
                {
                    return child.getIndex();
                }
            }
            return -1;
        });
    }
}
