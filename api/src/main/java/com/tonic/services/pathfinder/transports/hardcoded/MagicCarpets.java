package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.handlers.GenericHandlerBuilder;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.NpcQuery;
import com.tonic.services.pathfinder.requirements.OtherRequirement;
import com.tonic.services.pathfinder.requirements.QuestRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.TextUtil;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The desert's magic carpets: two hubs, the Shantay Pass and south Pollnivneach, each flying to its spokes and back.
 */
public final class MagicCarpets
{
    private static final String NAME = "Rug Merchant";
    private static final int FARE = 200;
    private static final int ONE_DISCOUNT_FARE = 100;
    private static final int BOTH_DISCOUNTS_FARE = 75;
    private static final String TRAVEL = "Travel";
    private static final String DECLINE = "don't";
    private static final String[] ACCEPTS = {"take me", "let's go", "sounds good", "will do", "yes"};
    private static final int SEARCH_RADIUS = 8;
    private static final int TAKE_OFF_TICKS = 20;
    private static final int LEFT_THE_STAND_TILES = 3;
    private static final int LANDING_SLACK_TICKS = 20;
    private static final Route[] ROUTES =
            {
                    new Route(Stand.SHANTAY_PASS, Stand.UZER, 67, Quest.THE_GOLEM),
                    new Route(Stand.UZER, Stand.SHANTAY_PASS, 63, Quest.THE_GOLEM),
                    new Route(Stand.SHANTAY_PASS, Stand.BEDABIN_CAMP, 68, null),
                    new Route(Stand.BEDABIN_CAMP, Stand.SHANTAY_PASS, 68, null),
                    new Route(Stand.SHANTAY_PASS, Stand.NORTH_POLLNIVNEACH, 55, null),
                    new Route(Stand.NORTH_POLLNIVNEACH, Stand.SHANTAY_PASS, 41, null),
                    new Route(Stand.SOUTH_POLLNIVNEACH, Stand.NARDAH, 36, null),
                    new Route(Stand.NARDAH, Stand.SOUTH_POLLNIVNEACH, 36, null),
                    new Route(Stand.SOUTH_POLLNIVNEACH, Stand.SOPHANEM, 72, Quest.ICTHLARINS_LITTLE_HELPER),
                    new Route(Stand.SOPHANEM, Stand.SOUTH_POLLNIVNEACH, 72, Quest.ICTHLARINS_LITTLE_HELPER),
                    new Route(Stand.SOUTH_POLLNIVNEACH, Stand.MENAPHOS, 87, Quest.ICTHLARINS_LITTLE_HELPER),
                    new Route(Stand.MENAPHOS, Stand.SOUTH_POLLNIVNEACH, 85, Quest.ICTHLARINS_LITTLE_HELPER)
            };

    private MagicCarpets()
    {
    }

    /**
     * Adds a ride for every route of the carpet network.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        for (Route route : ROUTES)
        {
            BooleanSupplier arrived = new Transport(NAME, route.from.merchantId, false, route.from.location, route.to.location, 1, null, route.ticks, null)::arrived;
            add.accept(new CodeTransport(NAME, route.from.location, route.to.location, route.ticks, route.requirements(), () -> ride(route, arrived)));
        }
    }

    private static StepHandler ride(Route route, BooleanSupplier arrived)
    {
        return GenericHandlerBuilder.get()
                .add(() ->
                {
                    NpcEx merchant = new NpcQuery().withIds(route.from.merchantId).within(route.from.location, SEARCH_RADIUS).nearest();
                    if (merchant != null)
                    {
                        NpcAPI.interact(merchant, TRAVEL);
                    }
                })
                .addDelayUntil(TAKE_OFF_TICKS, () -> answered(route.to) && liftedOff(route.from, arrived), () ->
                {
                })
                .addDelayUntil(route.ticks + LANDING_SLACK_TICKS, arrived, () ->
                {
                })
                .build();
    }

    private static boolean answered(Stand to)
    {
        List<String> options = DialogueAPI.getOptions();
        if (!options.isEmpty())
        {
            int choice = choice(options, to);
            if (choice > 0)
            {
                DialogueAPI.selectOption(choice);
            }
            return false;
        }
        if (DialogueAPI.dialoguePresent())
        {
            DialogueAPI.continueDialogue();
            return false;
        }
        return true;
    }

    private static boolean liftedOff(Stand from, BooleanSupplier arrived)
    {
        WorldPoint at = PlayerEx.getLocal().getWorldPoint();
        return arrived.getAsBoolean() || at != null && at.distanceTo(from.location) > LEFT_THE_STAND_TILES;
    }

    private static int fare()
    {
        if (VarAPI.getVar(VarbitID.DESERT_DIARY_HARD_COMPLETE) == 1)
        {
            return 0;
        }
        boolean ring = EquipmentAPI.isEquipped(ItemID.RING_OF_CHAROS_UNLOCKED);
        boolean rogueTrader = VarAPI.getVar(VarbitID.ROGUETRADER_STARTED) > 0;
        if (ring && rogueTrader)
        {
            return BOTH_DISCOUNTS_FARE;
        }
        return ring || rogueTrader ? ONE_DISCOUNT_FARE : FARE;
    }

    private static int choice(List<String> options, Stand to)
    {
        String wanted = to.placeName.toLowerCase(Locale.ROOT);
        for (int i = 0; i < options.size(); i++)
        {
            String option = plain(options.get(i));
            if (!option.contains(DECLINE) && option.contains(wanted))
            {
                return i + 1;
            }
        }
        for (int i = 0; i < options.size(); i++)
        {
            String option = plain(options.get(i));
            if (option.contains(DECLINE))
            {
                continue;
            }
            for (String accept : ACCEPTS)
            {
                if (option.contains(accept))
                {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private static String plain(String option)
    {
        return option == null ? "" : TextUtil.sanitize(option).toLowerCase(Locale.ROOT);
    }

    private enum Stand
    {
        SHANTAY_PASS(new WorldPoint(3311, 3109, 0), NpcID.MAGIC_CARPET_SELLER1, "Shantay"),
        UZER(new WorldPoint(3467, 3110, 0), NpcID.MAGIC_CARPET_SELLER5, "Uzer"),
        BEDABIN_CAMP(new WorldPoint(3182, 3043, 0), NpcID.MAGIC_CARPET_SELLER2, "Bedabin"),
        NORTH_POLLNIVNEACH(new WorldPoint(3350, 3001, 0), NpcID.MAGIC_CARPET_SELLER4, "Pollnivneach"),
        SOUTH_POLLNIVNEACH(new WorldPoint(3348, 2942, 0), NpcID.MAGIC_CARPET_SELLER3, "Pollnivneach"),
        NARDAH(new WorldPoint(3401, 2918, 0), NpcID.MAGIC_CARPET_SELLER8, "Nardah"),
        SOPHANEM(new WorldPoint(3287, 2813, 0), NpcID.MAGIC_CARPET_SELLER6, "Sophanem"),
        MENAPHOS(new WorldPoint(3243, 2813, 0), NpcID.MAGIC_CARPET_SELLER7, "Menaphos");

        private final WorldPoint location;
        private final int merchantId;
        private final String placeName;

        Stand(WorldPoint location, int merchantId, String placeName)
        {
            this.location = location;
            this.merchantId = merchantId;
            this.placeName = placeName;
        }
    }

    private static final class Route
    {
        private final Stand from;
        private final Stand to;
        private final int ticks;
        private final Quest quest;

        Route(Stand from, Stand to, int ticks, Quest quest)
        {
            this.from = from;
            this.to = to;
            this.ticks = ticks;
            this.quest = quest;
        }

        Requirements requirements()
        {
            Requirements requirements = new Requirements();
            requirements.addRequirement(new WorldRequirement(true));
            requirements.addRequirement(new OtherRequirement(() -> InventoryAPI.count(ItemID.COINS) >= fare()));
            if (quest != null)
            {
                requirements.addRequirement(new QuestRequirement(quest, QuestState.FINISHED));
            }
            return requirements;
        }
    }
}
