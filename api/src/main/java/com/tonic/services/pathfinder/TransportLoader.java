package com.tonic.services.pathfinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.Skills;
import com.tonic.api.game.VarAPI;
import com.tonic.api.game.WorldsAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.ItemEx;
import com.tonic.data.Quests;
import com.tonic.data.TileObjectEx;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.model.TransportDto;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.teleports.MovementConstants;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.tonic.services.pathfinder.teleports.MovementConstants.SLASH_ITEMS;
import static com.tonic.services.pathfinder.teleports.MovementConstants.SLASH_WEB_POINTS;

public class TransportLoader
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final TIntObjectHashMap<ArrayList<Transport>> ALL_STATIC_TRANSPORTS = new TIntObjectHashMap<>();
    private static final TIntObjectHashMap<ArrayList<Transport>> LAST_TRANSPORT_LIST = new TIntObjectHashMap<>();
    private static List<Transport> TEMP_TRANSPORTS;

    public static void init()
    {
        try (InputStream stream = Walker.class.getResourceAsStream("transports.json"))
        {
            if (stream == null)
            {
                System.err.println("transports.json not found!");
                return;
            }

            TransportDto[] json = GSON.fromJson(new String(stream.readAllBytes()), TransportDto[].class);

            List<Transport> list = Arrays.stream(json)
                    .map(TransportDto::toTransport)
                    .collect(Collectors.toList());
            for(Transport transport : list)
            {
                computeIfAbsent(ALL_STATIC_TRANSPORTS, transport);
            }
        }
        catch (IOException e)
        {
            System.err.println("Failed to load transports");
            e.printStackTrace();
        }

        System.out.println("Loaded " + ALL_STATIC_TRANSPORTS.size() + " transports");
    }

    public static TIntObjectHashMap<ArrayList<Transport>> getTransports()
    {
        return LAST_TRANSPORT_LIST;
    }

    private static void computeIfAbsent(final TIntObjectHashMap<ArrayList<Transport>> transports, Transport transport)
    {
        computeIfAbsent(transports, transport.getSource(), transport);
    }

    private static void computeIfAbsent(final TIntObjectHashMap<ArrayList<Transport>> transports, int key, Transport transport)
    {
        ArrayList<Transport> list = transports.get(key);
        if (list == null) {
            list = new ArrayList<>();
            transports.put(key, list);
        }
        list.add(transport);
    }

    public static void refreshTransports()
    {
        boolean lock = Static.invoke(() ->
        {
            List<Transport> filteredStatic = new ArrayList<>();
            for (ArrayList<Transport> list : ALL_STATIC_TRANSPORTS.valueCollection()) {
                for(var transport : list)
                {
                    if(transport.getRequirements().fulfilled())
                    {
                        filteredStatic.add(transport);
                    }
                }
            }

            List<Transport> transports = new ArrayList<>();

            int gold = InventoryAPI.getItem(995) != null ? InventoryAPI.getItem(995).getQuantity() : 0;

            if (gold >= 30)
            {
                if (Quests.isFinished(Quest.PIRATES_TREASURE))
                {
                    transports.add(npcTransport(new WorldPoint(3027, 3218, 0), new WorldPoint(2956, 3143, 1), 3644, "Pay-fare"));
                    transports.add(npcTransport(new WorldPoint(2954, 3147, 0), new WorldPoint(3032, 3217, 1), 3648, "Pay-Fare"));
                }
                else
                {
                    transports.add(npcDialogTransport(new WorldPoint(3027, 3218, 0), new WorldPoint(2956, 3143, 1), 3644, "Yes please."));
                    transports.add(npcDialogTransport(new WorldPoint(2954, 3147, 0), new WorldPoint(3032, 3217, 1), 3648, "Can I journey on this ship?", "Search away, I have nothing to hide.", "Ok"));
                }
            }

            if (WorldsAPI.inMembersWorld())
            {
                //Shamans
                transports.add(objectTransport(new WorldPoint(1312, 3685, 0), new WorldPoint(1312, 10086, 0), 34405, "Enter"));

                //Doors for shamans
                transports.add(objectTransport(new WorldPoint(1293, 10090, 0), new WorldPoint(1293, 10093, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1293, 10093, 0), new WorldPoint(1293, 10091, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1296, 10096, 0), new WorldPoint(1298, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1298, 10096, 0), new WorldPoint(1296, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1307, 10096, 0), new WorldPoint(1309, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1309, 10096, 0), new WorldPoint(1307, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1316, 10096, 0), new WorldPoint(1318, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1318, 10096, 0), new WorldPoint(1316, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1324, 10096, 0), new WorldPoint(1326, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1326, 10096, 0), new WorldPoint(1324, 10096, 0), 34642, "Pass"));

                // Crabclaw island
                if (gold >= 10_000)
                {
                    transports.add(npcTransport(new WorldPoint(1782, 3458, 0), new WorldPoint(1778, 3417, 0), 7483, "Travel"));
                }

                transports.add(npcTransport(new WorldPoint(1779, 3418, 0), new WorldPoint(1784, 3458, 0), 7484, "Travel"));

                // Port sarim
                if (VarAPI.getVar(VarbitID.ZEAH_PLAYERHASVISITED) == 0) // First time talking to Veos
                {
                    if (VarAPI.getVar(VarbitID.CLUEQUEST) >= 7)
                    {
                        transports.add(npcDialogTransport(new WorldPoint(3054, 3245, 0),
                                new WorldPoint(1824, 3691, 0),
                                8484,
                                "Can you take me to Great Kourend?"));
                    }
                    else
                    {
                        transports.add(npcDialogTransport(new WorldPoint(3054, 3245, 0),
                                new WorldPoint(1824, 3691, 0),
                                8484,
                                "That's great, can you take me there please?"));
                    }
                }
                else if (Quests.isFinished(Quest.A_KINGDOM_DIVIDED)) // Veos is replaced during/after quest
                {
                    transports.add(npcTransport(new WorldPoint(3053, 3245, 0),
                            new WorldPoint(1824, 3695, 1),
                            "Cabin Boy Herbert",
                            "Port Piscarilius"));
                    transports.add(npcTransport(new WorldPoint(3053, 3245, 0),
                            new WorldPoint(1504, 3395, 1),
                            "Cabin Boy Herbert",
                            "Land's End"));
                }
                else // Has talked to Veos before
                {
                    transports.add(npcTransport(new WorldPoint(3054, 3245, 0),
                            new WorldPoint(1824, 3695, 1),
                            "Veos",
                            "Port Piscarilius"));
                }

                // Charter Ships
//                if (Static.getUnethicaliteConfig().useCharterShips())
//                {
//                    transports.addAll(CharterShipLocation.getCharterShips(gold));
//                }

                if (Quests.getState(Quest.LUNAR_DIPLOMACY) != QuestState.NOT_STARTED)
                {
                    transports.add(npcTransport(new WorldPoint(2222, 3796, 2), new WorldPoint(2130, 3899, 2), NpcID.CAPTAIN_BENTLEY_6650, "Travel"));
                    transports.add(npcTransport(new WorldPoint(2130, 3899, 2), new WorldPoint(2222, 3796, 2), NpcID.CAPTAIN_BENTLEY_6650, "Travel"));
                }

                // Spirit Trees
//                if (Quests.isFinished(Quest.TREE_GNOME_VILLAGE))
//                {
//                    for (var source : SPIRIT_TREES)
//                    {
//                        if (source.location.equals("Gnome Stronghold") && !Quests.isFinished(Quest.THE_GRAND_TREE))
//                        {
//                            continue;
//                        }
//                        for (var target : SPIRIT_TREES)
//                        {
//                            if (source == target)
//                            {
//                                continue;
//                            }
//
//                            transports.add(spritTreeTransport(source.position, target.position, target.location));
//                        }
//                    }
//                }

                if (Quests.isFinished(Quest.THE_LOST_TRIBE))
                {
                    transports.add(npcTransport(new WorldPoint(3229, 9610, 0), new WorldPoint(3316, 9613, 0), "Kazgar",
                            "Mines"));
                    transports.add(npcTransport(new WorldPoint(3316, 9613, 0), new WorldPoint(3229, 9610, 0), "Mistag",
                            "Cellar"));
                }

                // Tree Gnome Village
                if (Quests.getState(Quest.TREE_GNOME_VILLAGE) != QuestState.NOT_STARTED)
                {
                    transports.add(npcTransport(new WorldPoint(2504, 3192, 0), new WorldPoint(2515, 3159, 0), 4968, "Follow"));
                    transports.add(npcTransport(new WorldPoint(2515, 3159, 0), new WorldPoint(2504, 3192, 0), 4968, "Follow"));
                }

                // Gnome Battlefield
                if (VarAPI.getVarp(VarPlayerID.TREEQUEST) >= 5)
                {
                    transports.add(objectDialogTransport(new WorldPoint(2509, 3252, 0),
                            new WorldPoint(2509, 3254, 0), 2185,
                            "Climb-over"));
                }
                // Eagles peak cave
                if (VarAPI.getVarp(934) >= 15)
                {
                    // Entrance
                    transports.add(objectTransport(new WorldPoint(2328, 3496, 0), new WorldPoint(1994, 4983, 3), 19790,
                            "Enter"));
                    transports.add(objectTransport(new WorldPoint(1994, 4983, 3), new WorldPoint(2328, 3496, 0), 19891,
                            "Exit"));
                }

                // Waterbirth island
                if (Quests.isFinished(Quest.THE_FREMENNIK_TRIALS) || gold >= 1000)
                {
                    transports.add(npcTransport(new WorldPoint(2544, 3760, 0), new WorldPoint(2620, 3682, 0), 10407, "Rellekka"));
                    transports.add(npcTransport(new WorldPoint(2620, 3682, 0), new WorldPoint(2547, 3759, 0), 5937, "Waterbirth Island"));
                }

                // Pirates cove
                transports.add(npcTransport(new WorldPoint(2620, 3692, 0), new WorldPoint(2213, 3794, 0), NpcID.LOKAR_SEARUNNER, "Pirate's Cove"));
                transports.add(npcTransport(new WorldPoint(2213, 3794, 0), new WorldPoint(2620, 3692, 0), NpcID.LOKAR_SEARUNNER_9306, "Rellekka"));

                // Corsair's Cove
                if (Skills.getBoostedLevel(Skill.AGILITY) >= 10)
                {
                    transports.add(objectTransport(new WorldPoint(2546, 2871, 0), new WorldPoint(2546, 2873, 0), 31757,
                            "Climb"));
                    transports.add(objectTransport(new WorldPoint(2546, 2873, 0), new WorldPoint(2546, 2871, 0), 31757,
                            "Climb"));
                }

                // Lumbridge castle dining room, ignore if RFD is in progress.
                if (Quests.getState(Quest.RECIPE_FOR_DISASTER) != QuestState.IN_PROGRESS)
                {

                    transports.add(objectTransport(new WorldPoint(3213, 3221, 0), new WorldPoint(3212, 3221, 0), 12349, "Open"));
                    transports.add(objectTransport(new WorldPoint(3212, 3221, 0), new WorldPoint(3213, 3221, 0), 12349, "Open"));
                    transports.add(objectTransport(new WorldPoint(3213, 3222, 0), new WorldPoint(3212, 3222, 0), 12350, "Open"));
                    transports.add(objectTransport(new WorldPoint(3212, 3222, 0), new WorldPoint(3213, 3222, 0), 12350, "Open"));
                    transports.add(objectTransport(new WorldPoint(3207, 3218, 0), new WorldPoint(3207, 3217, 0), 12348, "Open"));
                    transports.add(objectTransport(new WorldPoint(3207, 3217, 0), new WorldPoint(3207, 3218, 0), 12348, "Open"));
                }

                // Digsite gate
                if (VarAPI.getVar(VarbitID.VM_KUDOS) >= 153)
                {
                    transports.add(objectTransport(new WorldPoint(3295, 3429, 0), new WorldPoint(3296, 3429, 0), 24561,
                            "Open"));
                    transports.add(objectTransport(new WorldPoint(3296, 3429, 0), new WorldPoint(3295, 3429, 0), 24561,
                            "Open"));
                    transports.add(objectTransport(new WorldPoint(3295, 3428, 0), new WorldPoint(3296, 3428, 0), 24561,
                            "Open"));
                    transports.add(objectTransport(new WorldPoint(3296, 3428, 0), new WorldPoint(3295, 3428, 0), 24561,
                            "Open"));
                }

                // Fairy Rings
//                if (EquipmentAPI.isEquipped(ItemID.DRAMEN_STAFF) || EquipmentAPI.isEquipped(ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF)
//                        && Quests.getState(Quest.FAIRYTALE_II__CURE_A_QUEEN) != QuestState.NOT_STARTED)
//                {
//                    for (FairyRingLocation sourceRing : FairyRingLocation.values())
//                    {
//                        for (FairyRingLocation destRing : FairyRingLocation.values())
//                        {
//                            if (sourceRing != destRing)
//                            {
//                                transports.add(fairyRingTransport(sourceRing, destRing));
//                            }
//                        }
//                    }
//                }

                // Al Kharid to and from Ruins of Unkah
                transports.add(npcTransport(new WorldPoint(3272, 3144, 0), new WorldPoint(3148, 2842, 0), NpcID.FERRYMAN_SATHWOOD, "Ferry"));
                transports.add(npcTransport(new WorldPoint(3148, 2842, 0), new WorldPoint(3272, 3144, 0), NpcID.FERRYMAN_NATHWOOD, "Ferry"));

                // Gnome Gliders
//                if (Quests.isFinished(Quest.THE_GRAND_TREE))
//                {
//                    for (var source : GnomeGliderLocation.values())
//                    {
//                        for (var target : GnomeGliderLocation.values())
//                        {
//                            if (source.getWorldPoint() == GnomeGliderLocation.LEMANTO_ANDRA.getWorldPoint())
//                            {
//                                continue;
//                            }
//                            if ((source.getWorldPoint() == GnomeGliderLocation.LEMANTOLLY_UNDRI.getWorldPoint() ||
//                                    target.getWorldPoint() == GnomeGliderLocation.LEMANTOLLY_UNDRI.getWorldPoint()) &&
//                                    !Quests.isFinished(Quest.ONE_SMALL_FAVOUR))
//                            {
//                                continue;
//                            }
//                            if ((source.getWorldPoint() == GnomeGliderLocation.OOKOOKOLLY_UNDRI.getWorldPoint() ||
//                                    target.getWorldPoint() == GnomeGliderLocation.OOKOOKOLLY_UNDRI.getWorldPoint()) &&
//                                    !Quests.isFinished(Quest.MONKEY_MADNESS_II))
//                            {
//                                continue;
//                            }
//
//                            if (source != target)
//                            {
//                                transports.add(gnomeGliderTransport(source.getWorldPoint(), target.getWorldPoint(), target.getWidgetID()));
//                            }
//                        }
//                    }
//                }
//            }

                // Entrana
                transports.add(npcTransport(new WorldPoint(3041, 3237, 0), new WorldPoint(2834, 3331, 1), 1166, "Take-boat"));
                transports.add(npcTransport(new WorldPoint(2834, 3335, 0), new WorldPoint(3048, 3231, 1), 1170, "Take-boat"));
                transports.add(npcDialogTransport(new WorldPoint(2821, 3374, 0),
                        new WorldPoint(2822, 9774, 0),
                        1164,
                        "Well that is a risk I will have to take."));

                // Fossil Island
                transports.add(npcTransport(new WorldPoint(3362, 3445, 0),
                        new WorldPoint(3724, 3808, 0),
                        8012,
                        "Quick-Travel"));

                transports.add(objectDialogTransport(new WorldPoint(3724, 3808, 0),
                        new WorldPoint(3362, 3445, 0),
                        30914,
                        "Travel",
                        "Row to the barge and travel to the Digsite."));

                // Magic Mushtrees
//            for (var source : MUSHTREES)
//            {
//                for (var target : MUSHTREES)
//                {
//                    if (source.position != target.position)
//                    {
//                        transports.add(mushtreeTransport(source.position, target.position, target.widget));
//                    }
//                }
//            }
                // Tower of Life
                transports.add(trapDoorTransport(new WorldPoint(2648, 3213, 0), new WorldPoint(3038, 4376, 0), ObjectID.TRAPDOOR_21921, ObjectID.TRAPDOOR_21922));
                transports.add(objectTransport(new WorldPoint(3038, 4376, 0), new WorldPoint(2649, 3212, 0), ObjectID.LADDER_17974, "Climb-up"));

                // Gnome stronghold
                transports.add(objectDialogTransport(new WorldPoint(2460, 3382, 0), new WorldPoint(2461, 3385, 0), 190, "Open", "Sorry, I'm a bit busy."));
                transports.add(objectDialogTransport(new WorldPoint(2461, 3382, 0), new WorldPoint(2461, 3385, 0), 190, "Open", "Sorry, I'm a bit busy."));
                transports.add(objectDialogTransport(new WorldPoint(2462, 3382, 0), new WorldPoint(2461, 3385, 0), 190, "Open", "Sorry, I'm a bit busy."));

                // Paterdomus
                transports.add(trapDoorTransport(new WorldPoint(3405, 3506, 0), new WorldPoint(3405, 9906, 0), 1579, 1581));
                transports.add(trapDoorTransport(new WorldPoint(3423, 3485, 0), new WorldPoint(3440, 9887, 0), 3432, 3433));
                transports.add(trapDoorTransport(new WorldPoint(3422, 3484, 0), new WorldPoint(3440, 9887, 0), 3432, 3433));

                // Port Piscarilius
                if (Quests.isFinished(Quest.A_KINGDOM_DIVIDED)) // Veos is replaced during/after quest
                {
                    transports.add(npcTransport(new WorldPoint(1826, 3691, 0), new WorldPoint(3055, 3242, 1), 10932, "Port Sarim"));
                    transports.add(npcTransport(new WorldPoint(1826, 3691, 0), new WorldPoint(1504, 3395, 1), 10932, "Land's End"));
                }
                else
                {
                    transports.add(npcTransport(new WorldPoint(1824, 3691, 0), new WorldPoint(3055, 3242, 1), 10727, "Port Sarim"));
                }

                // Land's End
                transports.add(npcTransport(new WorldPoint(1504, 3401, 0), new WorldPoint(3055, 3242, 1), 7471, "Port Sarim"));
                transports.add(npcTransport(new WorldPoint(1504, 3401, 0), new WorldPoint(1824, 3695, 1), 7471, "Port Piscarilius"));

                // Glarial's tomb
                transports.add(itemUseTransport(new WorldPoint(2557, 3444, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
                transports.add(itemUseTransport(new WorldPoint(2557, 3445, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
                transports.add(itemUseTransport(new WorldPoint(2558, 3443, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
                transports.add(itemUseTransport(new WorldPoint(2559, 3443, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
                transports.add(itemUseTransport(new WorldPoint(2560, 3444, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
                transports.add(itemUseTransport(new WorldPoint(2560, 3445, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
                transports.add(itemUseTransport(new WorldPoint(2558, 3446, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
                transports.add(itemUseTransport(new WorldPoint(2559, 3446, 0), new WorldPoint(2555, 9844, 0), 294, 1992));

                // Waterfall Island
                transports.add(itemUseTransport(new WorldPoint(2512, 3476, 0), new WorldPoint(2513, 3468, 0), 954, 1996));
                transports.add(itemUseTransport(new WorldPoint(2512, 3466, 0), new WorldPoint(2511, 3463, 0), 954, 2020));

                // Edgeville Dungeon
                transports.add(trapDoorTransport(new WorldPoint(3096, 3468, 0), new WorldPoint(3096, 9867, 0), 1579, 1581));

                // Varrock Castle manhole
                transports.add(trapDoorTransport(new WorldPoint(3237, 3459, 0), new WorldPoint(3237, 9859, 0), 881, 882));

                // Draynor manor basement
                for (var entry : MovementConstants.DRAYNOR_MANOR_BASEMENT_DOORS.entrySet())
                {
                    if (VarAPI.getVar(entry.getKey()) == 1)
                    {
                        var points = entry.getValue();
                        transports.add(lockingDoorTransport(points.getLeft(), points.getRight(), 11450));
                        transports.add(lockingDoorTransport(points.getRight(), points.getLeft(), 11450));
                    }
                }

                // Corsair Cove, Captain Tock's ship's gangplank
                transports.add(objectTransport(new WorldPoint(2578, 2837, 1), new WorldPoint(2578, 2840, 0), 31756, "Cross"));
                transports.add(objectTransport(new WorldPoint(2578, 2840, 0), new WorldPoint(2578, 2837, 1), 31756, "Cross"));

                // Corsair Cove, Ithoi the Navigator's hut stairs
                transports.add(objectTransport(new WorldPoint(2532, 2833, 0), new WorldPoint(2529, 2835, 1), 31735, "Climb"));
                transports.add(objectTransport(new WorldPoint(2529, 2835, 1), new WorldPoint(2532, 2833, 0), 31735, "Climb"));

                // Corsair Cove, Dungeon hole to Ogress Warriors/Vine ladder
                transports.add(objectTransport(new WorldPoint(2523, 2860, 0), new WorldPoint(2012, 9004, 1), 31791, "Enter"));
                transports.add(objectTransport(new WorldPoint(2012, 9004, 1), new WorldPoint(2523, 2860, 0), 31790, "Climb"));

                // Rimmington docks to and from Corsair Cove using Captain Tock's ship
                if (Quests.isFinished(Quest.THE_CORSAIR_CURSE))
                {
                    transports.add(npcTransport(new WorldPoint(2910, 3226, 0), new WorldPoint(2578, 2837, 1), NpcID.CABIN_BOY_COLIN_7967, "Travel"));
                    transports.add(npcTransport(new WorldPoint(2574, 2835, 1), new WorldPoint(2909, 3230, 1), NpcID.CABIN_BOY_COLIN_7967, "Travel"));
                }
                else if (VarAPI.getVar(VarbitID.CORSCURS_PROGRESS) >= 15)
                {
                    transports.add(npcTransport(new WorldPoint(2910, 3226, 0), new WorldPoint(2578, 2837, 1), NpcID.CAPTAIN_TOCK_7958, "Travel"));
                    transports.add(npcTransport(new WorldPoint(2574, 2835, 1), new WorldPoint(2909, 3230, 1), NpcID.CAPTAIN_TOCK_7958, "Travel"));
                }

                // Draynor Jail
                transports.add(lockingDoorTransport(new WorldPoint(3123, 3244, 0), new WorldPoint(3123, 3243, 0), ObjectID.PRISON_GATE_2881));
                transports.add(lockingDoorTransport(new WorldPoint(3123, 3243, 0), new WorldPoint(3123, 3244, 0), ObjectID.PRISON_GATE_2881));

                if (InventoryAPI.contains(SLASH_ITEMS) || EquipmentAPI.isEquipped(i -> ArrayUtils.contains(SLASH_ITEMS, i.getId())))
                {
                    for (Pair<WorldPoint, WorldPoint> pair : SLASH_WEB_POINTS)
                    {
                        transports.add(slashWebTransport(pair.getLeft(), pair.getRight()));
                        transports.add(slashWebTransport(pair.getRight(), pair.getLeft()));
                    }
                }
//            if (TEMP_TRANSPORTS != null)
//            {
//                LAST_TRANSPORT_LIST.addAll(TEMP_TRANSPORTS);
//            }
            }

            LAST_TRANSPORT_LIST.clear();
            for (Transport transport : transports)
            {
                computeIfAbsent(LAST_TRANSPORT_LIST, transport);
            }
            for (Transport transport : filteredStatic) {
                computeIfAbsent(LAST_TRANSPORT_LIST, transport);
            }

            System.out.println("Refreshed transports, found " + LAST_TRANSPORT_LIST.size() + " transport nodes");
            return true;
        });
    }

    public static void updateTempTransports(List<Transport> transports)
    {
        TEMP_TRANSPORTS = transports;
        refreshTransports();
    }

    public static void clearTempTransports()
    {
        TEMP_TRANSPORTS = null;
        refreshTransports();
    }

    public static Transport lockingDoorTransport(
            WorldPoint source,
            WorldPoint destination,
            int openDoorId
    )
    {
        return new Transport(source, destination, 0, 0, () ->
        {
            TileObjectEx openDoor = new TileObjectQuery<>()
                    .withId(openDoorId)
                    .within(source, 1)
                    .first();

            if (openDoor != null)
            {
                TileObjectAPI.interact(openDoor, "Open");
            }
        });
    }

    public static Transport trapDoorTransport(
            WorldPoint source,
            WorldPoint destination,
            int closedId,
            int openedId
    )
    {
        return new Transport(source, destination, Integer.MAX_VALUE, 0, () ->
        {
            TileObjectEx closedTrapDoor = new TileObjectQuery<>()
                    .withId(closedId)
                    .within(source, 5)
                    .first();
            if (closedTrapDoor != null)
            {
                TileObjectAPI.interact(closedTrapDoor, 0);
                return;
            }

            TileObjectEx openedTrapdoor = new TileObjectQuery<>()
                    .withId(openedId)
                    .within(source, 5)
                    .first();
            if (openedTrapdoor != null)
            {
                TileObjectAPI.interact(openedTrapdoor, 0);
                return;
            }
        });
    }

//    public static Transport fairyRingTransport(
//            FairyRingLocation source,
//            FairyRingLocation destination
//    )
//    {
//        return new Transport(source.getLocation(), destination.getLocation(), Integer.MAX_VALUE, 0, () ->
//        {
//            log.debug("Looking for fairy ring at {} to {}", source.getLocation(), destination.getLocation());
//            TileObject ring = TileObjects.getFirstSurrounding(source.getLocation(), 5, "Fairy ring");
//
//            if (ring == null)
//            {
//                log.debug("Fairy ring at {} is null", source.getLocation());
//                return;
//            }
//
//            if (destination == FairyRingLocation.ZANARIS)
//            {
//                ring.interact("Zanaris");
//                return;
//            }
//
//            if (ring.hasAction(a -> a != null && a.contains(destination.getCode())))
//            {
//                ring.interact(a -> a != null && a.contains(destination.getCode()));
//                return;
//            }
//
//            if (Widgets.isVisible(Widgets.get(WidgetInfo.FAIRY_RING)))
//            {
//                destination.travel();
//                return;
//            }
//
//            ring.interact("Configure");
//        });
//    }

    public static Transport itemUseTransport(
            WorldPoint source,
            WorldPoint destination,
            int itemId,
            int objId
    )
    {
        return new Transport(source, destination, Integer.MAX_VALUE, 0, () ->
        {
            ItemEx item = InventoryAPI.getItem(itemId);
            if (item == null)
            {
                return;
            }

            TileObjectEx transport = new TileObjectQuery<>()
                    .withId(objId)
                    .within(source, 8)
                    .first();
            if (transport != null)
            {
                InventoryAPI.useOn(item, transport);
            }
        });
    }

    public static Transport npcTransport(
            WorldPoint source,
            WorldPoint destination,
            int npcId,
            String action
    )
    {
        return new Transport(source, destination, 10, 0, () ->
        {
            NPC npc = new NpcQuery()
                    .withIds(npcId)
                    .within(source, 10)
                    .first();
            if (npc != null)
            {
                NpcAPI.interact(npc, action);
            }
        });
    }

    public static Transport npcTransport(
            WorldPoint source,
            WorldPoint destination,
            String npcName,
            String action
    )
    {
        return new Transport(source, destination, 10, 0, () ->
        {
            NPC npc = new NpcQuery()
                    .withName(npcName)
                    .within(source, 10)
                    .first();
            if (npc != null)
            {
                NpcAPI.interact(npc, action);
            }
        });
    }

    public static Transport npcDialogTransport(
            WorldPoint source,
            WorldPoint destination,
            int npcId,
            String... chatOptions
    )
    {
        List<Runnable> actions = new ArrayList<>();
        actions.add(() -> {
            NPC npc = new NpcQuery()
                    .withIds(npcId)
                    .within(source, 10)
                    .first();
            if (npc != null)
            {
                NpcAPI.interact(npc, 0);
            }
        });
        for(String option : chatOptions)
        {
            actions.add(() -> {
                while(DialogueAPI.continueDialogue()) Delays.tick(); DialogueAPI.selectOption(option);
            });
        }
        return new LongTransport(source, destination, 10, 0, actions);
    }

    public static Transport objectTransport(
            WorldPoint source,
            WorldPoint destination,
            int objId,
            String actions
    )
    {
        return new Transport(source, destination, Integer.MAX_VALUE, 0, () ->
        {
            TileObjectEx first = new TileObjectQuery<>()
                    .atLocation(source)
                    .withId(objId)
                    .first();

            if (first == null)
            {
                first = new TileObjectQuery<>()
                        .within(source, 5)
                        .withId(objId)
                        .first();
            }

            if (first == null)
            {
                return;
            }

            TileObjectAPI.interact(first, actions);
        });
    }

    public static Transport objectTransport(
            WorldPoint source,
            WorldPoint destination,
            int objId,
            String actions,
            Requirements requirements
    )
    {
        return new Transport(source, destination, Integer.MAX_VALUE, 0, () ->
        {
            Client client = Static.getClient();
            WorldView wv = client.getTopLevelWorldView();
            WorldPoint localSource =
                    WorldPoint.toLocalInstance(wv, source).stream().findFirst().orElse(source);
            TileObjectEx first = new TileObjectQuery<>().atLocation(localSource).withId(objId).first();
            if (first != null)
            {
                TileObjectAPI.interact(first, actions);
                return;
            }
            TileObjectEx obj = new TileObjectQuery<>()
                    .withId(objId)
                    .within(localSource, 5)
                    .sortNearest()
                    .first();
            if (obj != null)
            {
                TileObjectAPI.interact(obj, actions);
            }
        }, requirements);
    }

    public static Transport objectDialogTransport(
            WorldPoint source,
            WorldPoint destination,
            int objId,
            String action,
            String... chatOptions
    )
    {
        List<Runnable> actions = new ArrayList<>();
        actions.add(() -> {
            TileObjectEx obj = new TileObjectQuery<>()
                    .withId(objId)
                    .within(source, 5)
                    .sortNearest()
                    .first();
            if (obj != null)
            {
                TileObjectAPI.interact(obj, action);
            }
        });

        for(String option : chatOptions)
        {
            actions.add(() -> {
                while(DialogueAPI.continueDialogue()) Delays.tick(); DialogueAPI.selectOption(option);
            });
        }

        return new LongTransport(source, destination, Integer.MAX_VALUE, 0, actions);
    }

    public static Transport slashWebTransport(
            WorldPoint source,
            WorldPoint destination
    )
    {
        return new Transport(source, destination, Integer.MAX_VALUE, 0, () ->
        {
            TileObjectEx web = new TileObjectQuery<>()
                    .withNameContains("Web")
                    .within(source, 5)
                    .withAction("Slash")
                    .first();
            if (web != null)
            {
                TileObjectAPI.interact(web, "Slash");
            }
        });
    }
}
