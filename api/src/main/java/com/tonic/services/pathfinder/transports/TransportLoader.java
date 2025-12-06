package com.tonic.services.pathfinder.transports;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.*;
import com.tonic.api.handlers.GenericHandlerBuilder;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.util.DialogueNode;
import com.tonic.api.game.WorldsAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.Walker;
import com.tonic.util.Distance;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.services.pathfinder.model.TransportDto;
import com.tonic.services.pathfinder.requirements.*;
import com.tonic.services.pathfinder.teleports.MovementConstants;
import com.tonic.services.pathfinder.transports.data.*;
import com.tonic.util.WorldPointUtil;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
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
        ALL_STATIC_TRANSPORTS.clear();
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
        refreshTransports(true);
    }

    public static void refreshTransports(boolean filter)
    {
        boolean lock = Static.invoke(() ->
        {
            List<Transport> filteredStatic = new ArrayList<>();
            for (ArrayList<Transport> list : ALL_STATIC_TRANSPORTS.valueCollection()) {
                for(var transport : list)
                {
                    if(transport.getRequirements() == null || transport.getRequirements().fulfilled() || !filter)
                    {
                        filteredStatic.add(transport);
                    }
                }
            }

            List<Transport> transports = new ArrayList<>();

            int gold = InventoryAPI.getItem(995) != null ? InventoryAPI.getItem(995).getQuantity() : 0;

            if (WorldsAPI.inMembersWorld() || !filter)
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
                if (gold >= 10_000 || !filter)
                {
                    transports.add(npcTransport(new WorldPoint(1782, 3458, 0), new WorldPoint(1778, 3417, 0), 7483, "Travel"));
                }

                if(QuestAPI.isCompleted(Quest.CHILDREN_OF_THE_SUN))
                {
                    transports.add(npcTransport(new WorldPoint(3280, 3412, 0), new WorldPoint(1700, 3141, 0), "Primio", "Travel"));
                    transports.add(npcTransport(new WorldPoint(1703, 3140, 0), new WorldPoint(3280, 3412, 0), "Primio", "Travel"));
                }

                transports.add(npcTransport(new WorldPoint(1779, 3418, 0), new WorldPoint(1784, 3458, 0), 7484, "Travel"));

                // Port sarim
                if (VarAPI.getVar(VarbitID.ZEAH_PLAYERHASVISITED) == 0 || !filter) // First time talking to Veos
                {
                    if (VarAPI.getVar(VarbitID.CLUEQUEST) >= 7 || !filter)
                    {
                        transports.add(npcDialogTransport(new WorldPoint(3055, 3245, 0),
                                new WorldPoint(1824, 3691, 0),
                                8484,
                                "Can you take me to Great Kourend?"));
                    }
                    else
                    {
                        transports.add(npcDialogTransport(new WorldPoint(3055, 3245, 0),
                                new WorldPoint(3055, 3245, 0),
                                8484,
                                "That's great, can you take me there please?"));
                    }
                }
                else if (QuestAPI.hasState(Quest.A_KINGDOM_DIVIDED, QuestState.IN_PROGRESS, QuestState.FINISHED) || !filter) // Veos is replaced during/after quest
                {
                    transports.add(npcBoatTransport(new WorldPoint(3055, 3245, 0),
                            new WorldPoint(1824, 3695, 1),
                            "Cabin Boy Herbert",
                            "Port Piscarilius", 4));
                    transports.add(npcBoatTransport(new WorldPoint(3055, 3245, 0),
                            new WorldPoint(1504, 3399, 0),
                            "Cabin Boy Herbert",
                            "Land's End", 4));
                }
                else // Has talked to Veos before
                {
                    transports.add(npcTransport(new WorldPoint(3055, 3245, 0),
                            new WorldPoint(1824, 3695, 1),
                            "Veos",
                            "Port Piscarilius"));
                }

                if (QuestAPI.getState(Quest.LUNAR_DIPLOMACY) != QuestState.NOT_STARTED || !filter)
                {
                    transports.add(npcTransport(new WorldPoint(2222, 3796, 2), new WorldPoint(2130, 3899, 2), NpcID.CAPTAIN_BENTLEY_6650, "Travel"));
                    transports.add(npcTransport(new WorldPoint(2130, 3899, 2), new WorldPoint(2222, 3796, 2), NpcID.CAPTAIN_BENTLEY_6650, "Travel"));
                }

                if(QuestAPI.isCompleted(Quest.PANDEMONIUM) && InventoryAPI.count(ItemID.COINS_995) >= 30)
                {
                    transports.add(npcTransport(new WorldPoint(3027, 3217, 0), new WorldPoint(3065, 3002, 0), "Captain Tobias", "The Pandemonium"));
                    transports.add(npcTransport(new WorldPoint(3064, 3002, 0), new WorldPoint(3029, 3217, 0), "Seaman Morris", "Port Sarim"));
                    transports.add(npcTransport(new WorldPoint(3064, 3002, 0), new WorldPoint(2956, 3146, 0), "Seaman Morris", "Musa Point"));
                }

                if (QuestAPI.isCompleted(Quest.THE_LOST_TRIBE) || !filter)
                {
                    transports.add(npcTransport(new WorldPoint(3229, 9610, 0), new WorldPoint(3316, 9613, 0), "Kazgar",
                            "Mines"));
                    transports.add(npcTransport(new WorldPoint(3316, 9613, 0), new WorldPoint(3229, 9610, 0), "Mistag",
                            "Cellar"));
                }

                // Tree Gnome Village
                if (QuestAPI.getState(Quest.TREE_GNOME_VILLAGE) != QuestState.NOT_STARTED || !filter)
                {
                    transports.add(npcTransport(new WorldPoint(2504, 3192, 0), new WorldPoint(2515, 3159, 0), 4968, "Follow"));
                    transports.add(npcTransport(new WorldPoint(2515, 3159, 0), new WorldPoint(2504, 3192, 0), 4968, "Follow"));
                }

                // Gnome Battlefield
                if (VarAPI.getVarp(VarPlayerID.TREEQUEST) >= 5 || !filter)
                {
                    transports.add(objectDialogTransport(new WorldPoint(2509, 3252, 0),
                            new WorldPoint(2509, 3254, 0), 2185,
                            "Climb-over"));
                }
                // Eagles peak cave
                if (VarAPI.getVarp(934) >= 15 || !filter)
                {
                    // Entrance
                    transports.add(objectTransport(new WorldPoint(2328, 3496, 0), new WorldPoint(1994, 4983, 3), 19790,
                            "Enter"));
                    transports.add(objectTransport(new WorldPoint(1994, 4983, 3), new WorldPoint(2328, 3496, 0), 19891,
                            "Exit"));
                }

                // Waterbirth island
                if (QuestAPI.isCompleted(Quest.THE_FREMENNIK_TRIALS) || gold >= 1000 || !filter)
                {
                    transports.add(npcTransport(new WorldPoint(2544, 3760, 0), new WorldPoint(2620, 3682, 0), 10407, "Rellekka"));
                    transports.add(npcTransport(new WorldPoint(2620, 3682, 0), new WorldPoint(2547, 3759, 0), 5937, "Waterbirth Island"));
                }

                // Pirates cove
                transports.add(npcTransport(new WorldPoint(2620, 3692, 0), new WorldPoint(2213, 3794, 0), NpcID.LOKAR_SEARUNNER, "Pirate's Cove"));
                transports.add(npcTransport(new WorldPoint(2213, 3794, 0), new WorldPoint(2620, 3692, 0), NpcID.LOKAR_SEARUNNER_9306, "Rellekka"));

                // Corsair's Cove
                if (SkillAPI.getBoostedLevel(Skill.AGILITY) >= 10 || !filter)
                {
                    transports.add(objectTransport(new WorldPoint(2546, 2871, 0), new WorldPoint(2546, 2873, 0), 31757,
                            "Climb"));
                    transports.add(objectTransport(new WorldPoint(2546, 2873, 0), new WorldPoint(2546, 2871, 0), 31757,
                            "Climb"));
                }

                // Lumbridge castle dining room, ignore if RFD is in progress.
                if (QuestAPI.getState(Quest.RECIPE_FOR_DISASTER) != QuestState.IN_PROGRESS || !filter)
                {

                    transports.add(objectTransport(new WorldPoint(3213, 3221, 0), new WorldPoint(3212, 3221, 0), 12349, "Open"));
                    transports.add(objectTransport(new WorldPoint(3212, 3221, 0), new WorldPoint(3213, 3221, 0), 12349, "Open"));
                    transports.add(objectTransport(new WorldPoint(3213, 3222, 0), new WorldPoint(3212, 3222, 0), 12350, "Open"));
                    transports.add(objectTransport(new WorldPoint(3212, 3222, 0), new WorldPoint(3213, 3222, 0), 12350, "Open"));
                    transports.add(objectTransport(new WorldPoint(3207, 3218, 0), new WorldPoint(3207, 3217, 0), 12348, "Open"));
                    transports.add(objectTransport(new WorldPoint(3207, 3217, 0), new WorldPoint(3207, 3218, 0), 12348, "Open"));
                }

                // Digsite gate
                if (VarAPI.getVar(VarbitID.VM_KUDOS) >= 153 || !filter)
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

                // Al Kharid to and from Ruins of Unkah
                transports.add(npcTransport(new WorldPoint(3272, 3144, 0), new WorldPoint(3148, 2842, 0), NpcID.FERRYMAN_SATHWOOD, "Ferry"));
                transports.add(npcTransport(new WorldPoint(3148, 2842, 0), new WorldPoint(3272, 3144, 0), NpcID.FERRYMAN_NATHWOOD, "Ferry"));

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
//                if (QuestAPI.isCompleted(Quest.A_KINGDOM_DIVIDED) || !filter) // Veos is replaced during/after quest
//                {
//                    transports.add(npcBoatTransport(new WorldPoint(1824, 3691, 0), new WorldPoint(3055, 3245, 0), 10932, "Port Sarim", 4));
//                    transports.add(npcBoatTransport(new WorldPoint(1824, 3691, 0), new WorldPoint(1504, 3399, 0), 10932, "Land's End", 4));
//                }
//                else
//                {
//                    transports.add(npcBoatTransport(new WorldPoint(1824, 3691, 0), new WorldPoint(3055, 3245, 0), 10727, "Port Sarim", 4));
//                }

                // Land's End
                transports.add(npcBoatTransport(new WorldPoint(1504, 3399, 0), new WorldPoint(3055, 3245, 0), 7471, "Port Sarim", 4));
                transports.add(npcBoatTransport(new WorldPoint(1504, 3399, 0), new WorldPoint(1824, 3691, 0), 7471, "Port Piscarilius", 4));

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
                    if (VarAPI.getVar(entry.getKey()) == 1 || !filter)
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
                if (QuestAPI.isCompleted(Quest.THE_CORSAIR_CURSE) || !filter)
                {
                    transports.add(npcTransport(new WorldPoint(2910, 3226, 0), new WorldPoint(2578, 2837, 1), NpcID.CABIN_BOY_COLIN_7967, "Travel"));
                    transports.add(npcTransport(new WorldPoint(2574, 2835, 1), new WorldPoint(2909, 3230, 1), NpcID.CABIN_BOY_COLIN_7967, "Travel"));
                }
                else if (VarAPI.getVar(VarbitID.CORSCURS_PROGRESS) >= 15 || !filter)
                {
                    transports.add(npcTransport(new WorldPoint(2910, 3226, 0), new WorldPoint(2578, 2837, 1), NpcID.CAPTAIN_TOCK_7958, "Travel"));
                    transports.add(npcTransport(new WorldPoint(2574, 2835, 1), new WorldPoint(2909, 3230, 1), NpcID.CAPTAIN_TOCK_7958, "Travel"));
                }

                // Draynor Jail
                transports.add(lockingDoorTransport(new WorldPoint(3123, 3244, 0), new WorldPoint(3123, 3243, 0), ObjectID.PRISON_GATE_2881));
                transports.add(lockingDoorTransport(new WorldPoint(3123, 3243, 0), new WorldPoint(3123, 3244, 0), ObjectID.PRISON_GATE_2881));

                if (InventoryAPI.contains(SLASH_ITEMS) || EquipmentAPI.isEquipped(i -> ArrayUtils.contains(SLASH_ITEMS, i.getId())) || !filter)
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
            hardcodedBullshit(LAST_TRANSPORT_LIST);

            addManholes(LAST_TRANSPORT_LIST);
            if(WorldsAPI.inMembersWorld() || !filter)
            {
                zannerisDoor(LAST_TRANSPORT_LIST);
                //veos(LAST_TRANSPORT_LIST);
                barnaby(LAST_TRANSPORT_LIST);
                charterShip(LAST_TRANSPORT_LIST);
                spiritTrees(LAST_TRANSPORT_LIST);
                kourendMinecartNetwork(LAST_TRANSPORT_LIST);
                gnomeGliders(LAST_TRANSPORT_LIST);
                fairyRings(LAST_TRANSPORT_LIST);
                dwarvenCarts(LAST_TRANSPORT_LIST);
                canoes(LAST_TRANSPORT_LIST);
            }
            if(VarAPI.getVar(279) == 1 || InventoryAPI.contains(ItemID.ROPE) || !filter)
            {
                computeIfAbsent(LAST_TRANSPORT_LIST, lumbyCave());
            }
            if(InventoryAPI.count(ItemID.COINS_995) > 10 || InventoryAPI.contains(ItemID.SHANTAY_PASS) || !filter)
            {
                computeIfAbsent(LAST_TRANSPORT_LIST, shantyPass());
            }

            for (Transport transport : transports)
            {
                computeIfAbsent(LAST_TRANSPORT_LIST, transport);
            }
            for (Transport transport : filteredStatic) {
                computeIfAbsent(LAST_TRANSPORT_LIST, transport);
            }

            if(filter)
            {
                LAST_TRANSPORT_LIST.forEachValue(list -> list.removeIf(t -> t.getRequirements() != null && !t.getRequirements().fulfilled()));
                LAST_TRANSPORT_LIST.forEachValue(list -> {
                    list.removeIf(t -> t.getRequirements() != null && !t.getRequirements().fulfilled());
                    return true;
                });
                LAST_TRANSPORT_LIST.retainEntries((key, value) ->  !value.isEmpty());
            }

            return true;
        });
    }

    private static void canoes(final TIntObjectHashMap<ArrayList<Transport>> transports)
    {
        for(Transport transport : CanoeStation.getTravelMatrix())
        {
            computeIfAbsent(transports, transport.getSource(), transport);
        }
    }

    private static void dwarvenCarts(final TIntObjectHashMap<ArrayList<Transport>> transports)
    {
        for(DwarvenCart cart : DwarvenCart.values())
        {
            var handler = cart.rideBack();
            Transport transport = new Transport(WorldPointUtil.compress(cart.getLocation()), WorldPointUtil.compress(cart.getDestination()), 6, 1, 22, handler, cart.getRequirements(), -1);
            computeIfAbsent(transports, WorldPointUtil.compress(cart.getLocation()), transport);
        }

        for(DwarvenCart cart : DwarvenCart.values())
        {
            var handler = cart.rideThere();
            Transport transport = new Transport(WorldPointUtil.compress(DwarvenCart.KELDEGRIM_WORLDPOINT), WorldPointUtil.compress(cart.getLocation()), 6, 1, 21, handler, cart.getRequirements(), -1);
            computeIfAbsent(transports, WorldPointUtil.compress(DwarvenCart.KELDEGRIM_WORLDPOINT), transport);
        }
    }

    private static void fairyRings(final TIntObjectHashMap<ArrayList<Transport>> transports)
    {
        for(FairyRing ring : FairyRing.values())
        {
            for(FairyRing destination : FairyRing.values())
            {
                if(ring == destination)
                {
                    continue;
                }

                if (destination == FairyRing.ZANARIS)
                {
                    HandlerBuilder builder = HandlerBuilder.get()
                            .add(0, () -> {
                                TileObjectEx current = new TileObjectQuery()
                                        .withName("Fairy ring")
                                        .first();
                                TileObjectAPI.interact(current, "Zanaris");
                                return 1;
                            })
                            .addDelay(1, 7);

                    Requirements merged = new Requirements();
                    merged.addRequirements(ring.getRequirements().getAll());
                    merged.addRequirements(destination.getRequirements().getAll());

                    Transport transport = new Transport(WorldPointUtil.compress(ring.getLocation()), WorldPointUtil.compress(destination.getLocation()), 6, 1, 7, builder.build(), merged, -1);
                    computeIfAbsent(transports, WorldPointUtil.compress(ring.getLocation()), transport);
                    continue;
                }

                HandlerBuilder builder = HandlerBuilder.get()
                        .add(0, () -> {
                            TileObjectEx current = new TileObjectQuery()
                                    .withName("Fairy ring")
                                    .nearest();
                            TileObjectAPI.interact(current, "Configure");
                            return 1;
                        })
                        .addDelayUntil(2, () -> WidgetAPI.get(InterfaceID.Fairyrings.CONFIRM) != null)
                        .addDelayUntil(3, () -> !destination.travel())
                        .addDelay(4, 7)
                        .add(5, () -> MovementAPI.walkToWorldPoint(destination.getLocation()))
                        .addDelay(6, 1);

                Requirements merged = new Requirements();
                merged.addRequirements(ring.getRequirements().getAll());
                merged.addRequirements(destination.getRequirements().getAll());

                Transport transport = new Transport(WorldPointUtil.compress(ring.getLocation()), WorldPointUtil.compress(destination.getLocation()), 6, 1, 7, builder.build(), merged, -1);
                computeIfAbsent(transports, WorldPointUtil.compress(ring.getLocation()), transport);
            }
        }
    }

    private static void gnomeGliders(final TIntObjectHashMap<ArrayList<Transport>> transports)
    {
        for(GnomeGlider glider : GnomeGlider.values())
        {
            if(glider == GnomeGlider.DIG_SITE)
            {
                continue;
            }
            for(GnomeGlider destination : GnomeGlider.values())
            {
                if(glider == destination)
                {
                    continue;
                }

                HandlerBuilder builder = HandlerBuilder.get()
                        .add(0, () -> {
                            NpcEx npc = new NpcQuery().withName(glider.getNpcName()).first();
                            NpcAPI.interact(npc, "Glider");
                            return 1;
                        })
                        .addDelayUntil(1, () -> WidgetAPI.get(138, 0) != null)
                        .add(2, () -> {
                            WidgetAPI.interact(1, destination.getIndex(), -1, -1);
                            return 3;
                        })
                        .addDelay(3, 4);

                Transport transport = new Transport(WorldPointUtil.compress(glider.getLocation()), WorldPointUtil.compress(destination.getLocation()), 6, 1, 4, builder.build(), destination.getRequirements(), -1);
                computeIfAbsent(transports, WorldPointUtil.compress(glider.getLocation()), transport);
            }
        }
    }

    private static void kourendMinecartNetwork(final TIntObjectHashMap<ArrayList<Transport>> transports)
    {
        for(MinecartNetwork minecart : MinecartNetwork.values())
        {
            for(MinecartNetwork destination : MinecartNetwork.values())
            {
                if(minecart == destination)
                {
                    continue;
                }

                HandlerBuilder builder = HandlerBuilder.get()
                        .add(0, () -> {
                            NpcEx npc = new NpcQuery().withName(minecart.getNpcName()).first();
                            NpcAPI.interact(npc, "Travel");
                            return 1;
                        })
                        .addDelayUntil(1, () -> PlayerEx.getLocal().isIdle())
                        .add(2, () -> 3)
                        .add(3, () -> {
                            DialogueAPI.resumePause(12255235, destination.getIndex());
                            return 4;
                        })
                        .addDelay(4, 5);

                Transport transport = new Transport(WorldPointUtil.compress(minecart.getLocation()), WorldPointUtil.compress(destination.getLocation()), 6, 1, 5, builder.build(), MinecartNetwork.getRequirements(), -1);
                computeIfAbsent(transports, WorldPointUtil.compress(minecart.getLocation()), transport);
            }
        }
    }

    private static void spiritTrees(final TIntObjectHashMap<ArrayList<Transport>> transports)
    {
        for(SpiritTree tree : SpiritTree.values())
        {
            for(SpiritTree destination : SpiritTree.values())
            {
                if(tree == destination)
                {
                    continue;
                }

                Requirements req = new Requirements();
                req.addRequirements(destination.getRequirements().getAll());

                // Gnome Stronghold can be travelled to, but not from, if The Grand Tree is unfinished
                if (tree == SpiritTree.GNOME_STRONGHOLD)
                {
                    req.addRequirement(new QuestRequirement(Quest.THE_GRAND_TREE, QuestState.FINISHED));
                }

                HandlerBuilder builder = HandlerBuilder.get()
                        .add(0, () -> {
                            TileObjectEx current = new TileObjectQuery()
                                    .withName("Spirit tree")
                                    .first();
                            TileObjectAPI.interact(current, "Travel");
                            return 1;
                        })
                        .addDelayUntil(1, () -> WidgetAPI.get(12255235) != null)
                        .add(2, () -> {
                            DialogueAPI.resumePause(12255235, destination.getIndex());
                            return 3;
                        })
                        .addDelay(3, 4);

                Transport transport = new Transport(WorldPointUtil.compress(tree.getLocation()), WorldPointUtil.compress(destination.getLocation()), 6, 1, 3, builder.build(), req, -1);
                computeIfAbsent(transports, WorldPointUtil.compress(tree.getLocation()), transport);
            }
        }
    }

    private static void charterShip(final TIntObjectHashMap<ArrayList<Transport>> transports)
    {
        DialogueNode node = DialogueNode.get()
                .node("Yes, and don't");
        CharterShip charterShip;
        for(CharterMap map : CharterMap.values())
        {
            charterShip = map.getCharterShip();
            for(CharterShip destination : map.getDestinations()) {
                HandlerBuilder builder = HandlerBuilder.get()
                        .add(0, () -> {
                            NpcEx npc = new NpcQuery().withName("Trader Crewmember").sortNearest().first();
                            NpcAPI.interact(npc, "Charter");
                            return 1;
                        })
                        .addDelayUntil(1, () -> {
                            Client client = Static.getClient();
                            return client.getWidget(InterfaceID.SailingMenu.UNIVERSE) != null;
                        })
                        .add(2, () -> {
                            WidgetAPI.interact(1, InterfaceID.CharteringMenuSide.LIST_CONTENT, destination.getIndex(), -1);
                            return 3;
                        })
                        .add(3, () -> 4)
                        .addDelayUntil(4, () -> !node.processStep())
                        .addDelay(5, 8);

                Transport transport = new Transport(
                        WorldPointUtil.compress(charterShip.getLocation()),
                        WorldPointUtil.compress(destination.getArival()),
                        6, 1, 8,
                        builder.build(),
                        destination.getRequirements(),
                        -1
                );
                computeIfAbsent(transports, WorldPointUtil.compress(charterShip.getLocation()), transport);
            }
        }
    }

    private static void barnaby(final TIntObjectHashMap<ArrayList<Transport>> transports)
    {
        BarnabyShip barnabyShip;
        for(BarnabyMap map : BarnabyMap.values())
        {
            barnabyShip = map.getBarnabyShip();
            for(BarnabyShip destination : map.getDestinations()) {
                HandlerBuilder builder = HandlerBuilder.get()
                        .add(0, () -> {
                            NpcEx npc = new NpcQuery().withName("Captain Barnaby").first();
                            NpcAPI.interact(npc, destination.getOption());
                        })
                        .addDelayUntil(1, () -> !MovementAPI.isMoving())
                        .addDelay(2, 7);
                Transport transport = new Transport(WorldPointUtil.compress(barnabyShip.getLocation()), WorldPointUtil.compress(destination.getArival()), 6, 1, 7, builder.build(), map.getRequirements(), -1);
                computeIfAbsent(transports, WorldPointUtil.compress(barnabyShip.getLocation()), transport);
            }
        }
    }

//    private static void veos(final TIntObjectHashMap<ArrayList<Transport>> transports)
//    {
//        //sarim -> Port Piscarilius
//        WorldPoint source = new WorldPoint(3054, 3246, 0);
//        WorldPoint destination = new WorldPoint(1824, 3695, 1);
//
//        DialogueNode node = DialogueNode.get()
//                .node("Take me there please")
//                .node("take me", " Port ");
//
//        HandlerBuilder builder = HandlerBuilder.get()
//                .add(0, () -> {
//                    NPC npc = new NpcQuery()
//                            .withNames(
//                                    NpcLocations.VEOS_PORT_SARIM.getName(),
//                                    NpcLocations.CABIN_BOY_HERBERT.getName()
//                            )
//                            .first();
//                    if(npc == null)
//                    {
//                        return 0;
//                    }
//                    NpcAPI.interact(npc, "Talk-to");
//                    return 1;
//                })
//                .addDelayUntil(1, DialogueAPI::dialoguePresent)
//                .addDelayUntil(2, () -> !node.processStep())
//                .addDelay(3, 4);
//
//        Transport transport = new Transport(source, destination, 2, 2, builder.build(), 4, -1);
//        computeIfAbsent(transports, WorldPointUtil.compress(source), transport);
//
//        //sarim -> Lands End
//        WorldPoint source2 = new WorldPoint(3054, 3246, 0);
//        WorldPoint destination2 = new WorldPoint(1504, 3395, 1);
//
//        DialogueNode node2 = DialogueNode.get()
//                .node("Take me there please")
//                .node("take me", " Land");
//
//        HandlerBuilder builder2 = HandlerBuilder.get()
//                .add(0, () -> NpcLocations.VEOS_PORT_SARIM.interact("Talk-to"))
//                .addDelayUntil(1, DialogueAPI::dialoguePresent)
//                .addDelayUntil(2, () -> !node2.processStep())
//                .addDelay(3, 4);
//
//        Requirements requirements = new Requirements();
//        requirements.addRequirement(new VarRequirement(Comparison.EQUAL, VarType.VARBIT, VarbitID.ZEAH_PLAYERHASVISITED, 1));
//
//        Transport transport2 = new Transport(WorldPointUtil.compress(source2), WorldPointUtil.compress(destination2), 2, 2, 4, builder2.build(), requirements, -1);
//        computeIfAbsent(transports, WorldPointUtil.compress(source2), transport2);
//    }

    private static void zannerisDoor(final TIntObjectHashMap<ArrayList<Transport>> transports)
    {
        WorldPoint source = WorldPointUtil.fromCompressed(51924097);
        WorldPoint destination = WorldPointUtil.fromCompressed(73255316);

        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    TileObjectEx object = new TileObjectQuery()
                            .withId(2406)
                            .sortNearest()
                            .first();
                    if(object != null)
                    {
                        TileObjectAPI.interact(object, "Open");
                        return 1;
                    }
                    return 2;
                })
                .add(1, () -> 2)
                .add(2, () -> 3)
                .add(3, () -> 4)
                .add(4, () -> {
                    MovementAPI.walkToWorldPoint(WorldPointUtil.fromCompressed(73255316));
                    return 5;
                });

        LongTransport transport = new LongTransport(source, destination, 2, 2, builder.build(), new Requirements(), 0);

        computeIfAbsent(transports, WorldPointUtil.compress(source), transport);
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
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    TileObjectEx openDoor = new TileObjectQuery()
                            .withId(openDoorId)
                            .within(source, 1)
                            .first();

                    if (openDoor != null)
                    {
                        TileObjectAPI.interact(openDoor, "Open");
                        return 1;
                    }
                    return 0;
                })
                .addDelayUntil(1, () -> {
                    WorldPoint worldPoint = PlayerEx.getLocal().getWorldPoint();
                    return Distance.pathDistanceTo(worldPoint, destination) < 5;
                });

        return new Transport(source, destination, 0, 0, builder.build(), -1);
    }

    public static Transport trapDoorTransport(
            WorldPoint source,
            WorldPoint destination,
            int closedId,
            int openedId
    )
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    TileObjectEx closedTrapDoor = new TileObjectQuery()
                            .withId(closedId)
                            .within(source, 5)
                            .first();
                    if (closedTrapDoor != null)
                    {
                        TileObjectAPI.interact(closedTrapDoor, 0);
                        return 1;
                    }

                    TileObjectEx openedTrapdoor = new TileObjectQuery()
                            .withId(openedId)
                            .within(source, 5)
                            .first();
                    if (openedTrapdoor != null)
                    {
                        TileObjectAPI.interact(openedTrapdoor, 0);
                        return 2;
                    }
                    return 0;
                })
                .add(1, () -> {
                    TileObjectEx openedTrapdoor = new TileObjectQuery()
                            .withId(openedId)
                            .within(source, 5)
                            .first();
                    if (openedTrapdoor != null)
                    {
                        TileObjectAPI.interact(openedTrapdoor, 0);
                        return 2;
                    }
                    return 0;
                })
                .addDelay(2, 1);
        return new Transport(source, destination, Integer.MAX_VALUE, 0, builder.build(), -1);
    }

    public static Transport itemUseTransport(
            WorldPoint source,
            WorldPoint destination,
            int itemId,
            int objId
    )
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    ItemEx item = InventoryAPI.getItem(itemId);
                    if (item == null)
                    {
                        return 0;
                    }

                    TileObjectEx transport = new TileObjectQuery()
                            .withId(objId)
                            .within(source, 8)
                            .first();
                    if (transport != null)
                    {
                        InventoryAPI.useOn(item, transport);
                        return 1;
                    }
                    return 0;
                })
                .addDelay(1, 1);
        return new Transport(source, destination, Integer.MAX_VALUE, 0, builder.build(), -1);
    }

    public static Transport npcTransport(
            WorldPoint source,
            WorldPoint destination,
            int npcId,
            String action
    )
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    NpcEx npc = new NpcQuery()
                            .withIds(npcId)
                            .within(source, 10)
                            .first();
                    if (npc != null)
                    {
                        NpcAPI.interact(npc, action);
                        return 1;
                    }
                    return 0;
                })
                .addDelayUntil(1, () -> {
                    WorldPoint worldPoint = PlayerEx.getLocal().getWorldPoint();
                    return Distance.pathDistanceTo(worldPoint, destination) < 5;
                });
        return new Transport(source, destination, 10, 0, builder.build(), -1);
    }

    public static Transport npcTransport(
            WorldPoint source,
            WorldPoint destination,
            String npcName,
            String action
    )
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    NpcEx npc = new NpcQuery()
                            .withName(npcName)
                            .within(source, 10)
                            .first();
                    if (npc != null)
                    {
                        NpcAPI.interact(npc, action);
                        return 1;
                    }
                    return 0;
                })
                .addDelayUntil(1, () -> {
                    WorldPoint worldPoint = PlayerEx.getLocal().getWorldPoint();
                    return Distance.pathDistanceTo(worldPoint, destination) < 5;
                });
        return new Transport(source, destination, 10, 0, builder.build(), -1);
    }

    public static Transport npcBoatTransport(
            WorldPoint source,
            WorldPoint destination,
            int npcId,
            String action,
            int delay
    )
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    NpcEx npc = new NpcQuery()
                            .withIds(npcId)
                            .within(source, 10)
                            .first();
                    if (npc != null)
                    {
                        NpcAPI.interact(npc, action);
                        return 1;
                    }
                    return 0;
                })
                .addDelayUntil(1, () -> !MovementAPI.isMoving())
                .addDelay(2, delay)
                .add(3, () -> {
                    Client client = Static.getClient();
                    WorldPoint worldPoint = PlayerEx.getLocal().getWorldPoint();
                    if(worldPoint.getPlane() != 1)
                    {
                        return 99;
                    }
                    TileObjectEx plank = new TileObjectQuery()
                            .withAction("Cross")
                            .nearest();

                    if(plank == null)
                    {
                        return 99;
                    }
                    TileObjectAPI.interact(plank, "Cross");
                    return 4;
                })
                .addDelay(4, 1);
        return new LongTransport(source, destination, 10, 0, builder.build());
    }

    public static Transport npcBoatTransport(
            WorldPoint source,
            WorldPoint destination,
            String npcName,
            String action,
            int delay
    )
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    NpcEx npc = new NpcQuery()
                            .withName(npcName)
                            .within(source, 10)
                            .first();
                    if (npc != null)
                    {
                        NpcAPI.interact(npc, action);
                        return 1;
                    }
                    return 0;
                })
                .addDelayUntil(1, () -> !MovementAPI.isMoving())
                .addDelay(2, delay)
                .add(3, () -> {
                    WorldPoint worldPoint = PlayerEx.getLocal().getWorldPoint();
                    if(worldPoint.getPlane() != 1)
                    {
                        return 99;
                    }
                    TileObjectEx plank = new TileObjectQuery()
                            .withAction("Cross")
                            .nearest();

                    if(plank == null)
                    {
                        return 99;
                    }
                    TileObjectAPI.interact(plank, "Cross");
                    return 4;
                })
                .addDelay(4, 1);
        return new LongTransport(source, destination, 10, 0, builder.build());
    }

    public static Transport npcDialogTransport(
            WorldPoint source,
            WorldPoint destination,
            int npcId,
            String... chatOptions
    )
    {
        DialogueNode node = DialogueNode.get()
                .node((Object[])chatOptions);
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    NpcEx npc = new NpcQuery()
                            .withIds(npcId)
                            .within(source, 10)
                            .first();
                    if (npc != null)
                    {
                        NpcAPI.interact(npc, 0);
                        return chatOptions != null && chatOptions.length > 0 ? 1 : 3;
                    }
                    return 0;
                })
                .addDelayUntil(1, DialogueAPI::dialoguePresent)
                .addDelayUntil(2, () -> !node.processStep())
                .addDelay(3, 1);
        return new LongTransport(source, destination, 10, 0, builder.build());
    }

    public static Transport objectTransport(
            WorldPoint source,
            WorldPoint destination,
            int objId,
            String actions
    )
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    TileObjectEx first = new TileObjectQuery()
                            .atLocation(source)
                            .withId(objId)
                            .first();

                    if (first == null)
                    {
                        first = new TileObjectQuery()
                                .within(source, 5)
                                .withId(objId)
                                .first();
                    }

                    if (first == null)
                    {
                        return 2;
                    }

                    TileObjectAPI.interact(first, actions);
                    return  1;
                })
                .addDelayUntil(1, () -> {
                    WorldPoint worldPoint = PlayerEx.getLocal().getWorldPoint();
                    return Distance.pathDistanceTo(worldPoint, destination) < 5;
                });
        return new Transport(source, destination, Integer.MAX_VALUE, 0, builder.build(), -1);
    }

    public static Transport objectTransport(
            WorldPoint source,
            WorldPoint destination,
            int objId,
            String action,
            Requirements requirements
    )
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    Client client = Static.getClient();
                    WorldView wv = client.getTopLevelWorldView();
                    WorldPoint localSource =
                            WorldPoint.toLocalInstance(wv, source).stream().findFirst().orElse(source);
                    TileObjectEx first = new TileObjectQuery().atLocation(localSource).withId(objId).first();
                    if (first != null)
                    {
                        TileObjectAPI.interact(first, action);
                        return 1;
                    }
                    TileObjectEx obj = new TileObjectQuery()
                            .withId(objId)
                            .within(localSource, 5)
                            .sortNearest()
                            .first();
                    if (obj != null)
                    {
                        TileObjectAPI.interact(obj, action);
                        return 1;
                    }
                    return 2;
                })
                .add(1, () -> {
                    WorldPoint worldPoint = PlayerEx.getLocal().getWorldPoint();
                    return Distance.pathDistanceTo(worldPoint, destination) < 10 && SceneAPI.isReachable(destination) ? 2 : 0;
                });
        return new Transport(source, destination, Integer.MAX_VALUE, 0, builder.build(), requirements, objId);
    }

    public static Transport objectDialogTransport(
            WorldPoint source,
            WorldPoint destination,
            int objId,
            String action,
            String... chatOptions
    )
    {
        DialogueNode node = DialogueNode.get()
                .node((Object[])chatOptions);
        GenericHandlerBuilder builder = GenericHandlerBuilder.get()
                .add(() -> {
                    TileObjectEx obj = new TileObjectQuery()
                            .withId(objId)
                            .within(source, 5)
                            .sortNearest()
                            .first();
                    if (obj != null)
                    {
                        TileObjectAPI.interact(obj, action);
                    }
                })
                .addDelayUntil(() -> {
                    if(SceneAPI.isReachable(destination) && Distance.pathDistanceTo(PlayerEx.getLocal().getWorldPoint(), destination) < 5)
                    {
                        return true;
                    }
                    if(DialogueAPI.dialoguePresent())
                    {
                        node.processStep();
                    }
                    return false;
                });

        return new LongTransport(source, destination, Integer.MAX_VALUE, 0, builder.build());
    }

    public static Transport slashWebTransport(
            WorldPoint source,
            WorldPoint destination
    )
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    TileObjectEx web = new TileObjectQuery()
                            .withNameContains("Web")
                            .within(source, 5)
                            .withAction("Slash")
                            .first();
                    if (web != null)
                    {
                        TileObjectAPI.interact(web, "Slash");
                        return 1;
                    }
                    return 2;
                })
                .addDelayUntil(1, () -> {
                    WorldPoint worldPoint = PlayerEx.getLocal().getWorldPoint();
                    return Distance.pathDistanceTo(worldPoint, destination) < 3;
                });
        return new Transport(source, destination, Integer.MAX_VALUE, 0, builder.build(), -1);
    }

    private static void addManholes(final TIntObjectHashMap<ArrayList<Transport>> transports)
    {
        //varrock sewers
        manhole(
                transports,
                new WorldPoint(3236, 3458, 0),
                new WorldPoint(3237, 9858, 0),
                882,
                881
        );

        //edgvile dungeon
        manhole(
                transports,
                new WorldPoint(3096, 3468, 0),
                new WorldPoint(3096, 9867, 0),
                1581,
                1579
        );

        manhole(
                transports,
                new WorldPoint(3654, 3519, 0),
                new WorldPoint(3669, 9888, 3),
                16114,
                16113
        );
    }

    private static void manhole(final TIntObjectHashMap<ArrayList<Transport>> transports, WorldPoint source, WorldPoint destination, int objectIdOpen, int objectIdClosed)
    {
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    TileObjectEx object = new TileObjectQuery()
                            .withId(objectIdClosed)
                            .within(source, 5)
                            .first();
                    if(object != null)
                    {
                        TileObjectAPI.interact(object, "Open");
                        return 1;
                    }

                    TileObjectEx object2 = new TileObjectQuery()
                            .withId(objectIdOpen)
                            .within(source, 5)
                            .first();
                    TileObjectAPI.interact(object2, "Climb");
                    return 2;
                })
                .add(1, () -> {
                    TileObjectEx object2 = new TileObjectQuery()
                            .withId(objectIdOpen)
                            .within(source, 5)
                            .first();
                    TileObjectAPI.interact(object2, "Climb");
                    return 2;
                })
                .addDelay(2, 1);

        LongTransport transport = new LongTransport(source, destination, 2, 2, builder.build(), new Requirements(), 0);

        computeIfAbsent(transports, WorldPointUtil.compress(source), transport);
    }

    public static LongTransport shantyPass() {
        WorldPoint source = new WorldPoint(3303, 3124, 0);
        WorldPoint destination = new WorldPoint(3303, 3115, 0);

        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    if (!InventoryAPI.contains(ItemID.SHANTAY_PASS)) {
                        NpcEx npc = new NpcQuery()
                                .withIds(NpcID.SHANTAY)
                                .within(source, 10)
                                .first();
                        NpcAPI.interact(npc, "Buy-pass");
                        return 1;
                    }
                    TileObjectEx object = new TileObjectQuery()
                            .withId(ObjectID.SHANTAY_PASS)
                            .first();
                    TileObjectAPI.interact(object, 0);
                    return 2;
                })
                .add(1, () -> {
                    TileObjectEx object = new TileObjectQuery()
                            .withId(ObjectID.SHANTAY_PASS)
                            .first();
                    TileObjectAPI.interact(object, 0);
                })
                .add(2, () -> {
                    if(WidgetAPI.isVisible(InterfaceID.CwsWarning10.WARN1) && WidgetAPI.getText(InterfaceID.CwsWarning10.WARN1).equalsIgnoreCase("Proceed regardless"))
                    {
                        WidgetAPI.interact(1, InterfaceID.CwsWarning10.WARN1, -1, -1);
                    }
                })
                .addDelay(3, 2);

        return new LongTransport(source, destination, 2, 2, builder.build(), new Requirements(), 2);
    }

    private static LongTransport lumbyCave()
    {
        WorldPoint source = new WorldPoint(3169, 3173, 0);
        WorldPoint destination = new WorldPoint(3167, 9573, 0);

        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    if(VarAPI.getVar(279) != 1 && InventoryAPI.contains(ItemID.ROPE))
                    {
                        ItemEx rope = InventoryAPI.getItem(ItemID.ROPE);
                        TileObjectEx object = new TileObjectQuery()
                                .withId(ObjectID.DARK_HOLE)
                                .first();
                        InventoryAPI.useOn(rope, object);
                        return 1;
                    }
                    TileObjectEx object = new TileObjectQuery()
                            .withId(ObjectID.DARK_HOLE)
                            .first();
                    TileObjectAPI.interact(object, 0);
                    return 2;
                })
                .add(1, () -> {
                    TileObjectEx object = new TileObjectQuery()
                            .withId(ObjectID.DARK_HOLE)
                            .first();
                    TileObjectAPI.interact(object, 0);
                    return 2;
                })
                .addDelay(2, 2);
        return new LongTransport(source, destination, 2, 2, builder.build(), new Requirements(), 1);
    }

    private static Requirements getGoldReq(int amount) {
        Requirements requirements = new Requirements();
        requirements.getItemRequirements().add(new ItemRequirement(false, amount, 995));
        return requirements;
    }

    private static void hardcodedBullshit(final TIntObjectHashMap<ArrayList<Transport>> transports) {
        //*

        //crabclaw island
        addNpcTransport(transports, 10, getGoldReq(10000), new WorldPoint(1782, 3458, 0), new WorldPoint(1778, 3417, 0), "Sandicrahb", "Travel");
        addNpcTransport(transports, 10, new WorldPoint(1779, 3418, 0), new WorldPoint(1784, 3458, 0), "Sandicrahb", "Travel");

        //lunar
        Requirements requirements3 = new Requirements();
        requirements3.getQuestRequirements().add(new QuestRequirement(Quest.LUNAR_DIPLOMACY,
                new HashSet<>() {{
                    add(QuestState.IN_PROGRESS);
                    add(QuestState.FINISHED);
                }}));
        addNpcTransport(transports, 10, requirements3, new WorldPoint(2222, 3796, 2), new WorldPoint(2130, 3899, 2), "Bentley", "Travel");
        addNpcTransport(transports, 10, requirements3, new WorldPoint(2130, 3899, 2), new WorldPoint(2222, 3796, 2), "Bentley", "Travel");

        //the lost tribe
        Requirements requirements4 = new Requirements();
        requirements4.getQuestRequirements().add(new QuestRequirement(Quest.THE_LOST_TRIBE,
                new HashSet<>() {{
                    add(QuestState.FINISHED);
                }}));
        addNpcTransport(transports, 6, requirements4, new WorldPoint(3229, 9610, 0), new WorldPoint(3316, 9613, 0), "Kazgar", "Mines");
        addNpcTransport(transports, 6, requirements4, new WorldPoint(3316, 9613, 0), new WorldPoint(3229, 9610, 0), "Mistag", "Cellar");

        //elkoy tree gnome village
        Requirements requirements5 = new Requirements();
        requirements5.getQuestRequirements().add(new QuestRequirement(Quest.TREE_GNOME_VILLAGE,
                new HashSet<>() {{
                    add(QuestState.IN_PROGRESS);
                    add(QuestState.FINISHED);
                }}));
        addNpcTransport(transports, 6, requirements5, new WorldPoint(2504, 3192, 0), new WorldPoint(2515, 3159, 0), "Elkoy", "Follow");
        addNpcTransport(transports, 6, requirements5, new WorldPoint(2515, 3159, 0), new WorldPoint(2504, 3192, 0), "Elkoy", "Follow");

        //Captain Barnaby


        // Eagles peak cave
        Requirements requirements = new Requirements();
        requirements.getVarRequirements().add(new VarRequirement(Comparison.GREATER_THAN_EQUAL, VarType.VARP, 934, 15));
        addObjectTransport(transports, 2, requirements, new WorldPoint(2328, 3496, 0), new WorldPoint(1994, 4983, 3), 19790, "Enter");
        addObjectTransport(transports, 2, requirements, new WorldPoint(1994, 4983, 3), new WorldPoint(2328, 3496, 0), 19891, "Exit");

        // Waterbirth island
        Requirements req = getGoldReq(1000);
        addNpcTransport(transports, 10, req, new WorldPoint(2544, 3760, 0), new WorldPoint(2620, 3682, 0), "Jarvald", "Rellekka");
        addNpcTransport(transports, 10, req, new WorldPoint(2620, 3682, 0), new WorldPoint(2547, 3759, 0), "Jarvald", "Waterbirth Island");

        // Digsite gate
        Requirements requirements2 = new Requirements();
        requirements.getVarRequirements().add(new VarRequirement(Comparison.GREATER_THAN_EQUAL, VarType.VARP, 3637, 153));
        addObjectTransport(transports, 2, requirements2, new WorldPoint(3295, 3429, 0), new WorldPoint(3296, 3429, 0), 24561, "Open");
        addObjectTransport(transports, 2, requirements2, new WorldPoint(3296, 3429, 0), new WorldPoint(3295, 3429, 0), 24561, "Open");
        addObjectTransport(transports, 2, requirements2, new WorldPoint(3295, 3428, 0), new WorldPoint(3296, 3428, 0), 24561, "Open");
        addObjectTransport(transports, 2, requirements2, new WorldPoint(3296, 3428, 0), new WorldPoint(3295, 3428, 0), 24561, "Open");

        //sarim
        if (QuestAPI.isCompleted(Quest.PIRATES_TREASURE))
        {
            addNpcTransport(transports, 10, getGoldReq(30), new WorldPoint(3027, 3217, 0), new WorldPoint(2956, 3146, 0),
                    "Captain Tobias", "Travel");
            addNpcTransport(transports, 10, getGoldReq(30), new WorldPoint(2956, 3146, 0), new WorldPoint(3029, 3217, 0),
                    "Customs officer", "Travel");
        }
        else
        {
            addNpcTransport(transports, 10, getGoldReq(30), new WorldPoint(3027, 3217, 0), new WorldPoint(2956, 3146, 0),
                    "Captain Tobias", "Travel", "Yes");
            addNpcTransport(transports, 10, getGoldReq(30), new WorldPoint(2956, 3146, 0), new WorldPoint(3029, 3217, 0),
                    "Customs officer", "Travel", "Can I journey", "Search away", "Ok");
        }
    }

    private static void addNpcTransport(final TIntObjectHashMap<ArrayList<Transport>> transports, int delay, WorldPoint source, WorldPoint destination, String npcName, String option) {
        addNpcTransport(transports, delay, source, destination, npcName, option, new String[]{});
    }

    private static void addNpcTransport(final TIntObjectHashMap<ArrayList<Transport>> transports, int delay, WorldPoint source, WorldPoint destination, String npcName, String option, String... dialogueOptions) {
        Transport transport = LongTransport.npcDialogTransport(delay, new Requirements(), npcName, option, 10, source, destination, dialogueOptions);
        computeIfAbsent(transports, WorldPointUtil.compress(source), transport);
    }

    private static void addNpcTransport(final TIntObjectHashMap<ArrayList<Transport>> transports, int delay, Requirements requirements, WorldPoint source, WorldPoint destination, String npcName, String option) {
        addNpcTransport(transports, delay, requirements, source, destination, npcName, option, new String[]{});
    }

    private static void addNpcTransport(final TIntObjectHashMap<ArrayList<Transport>> transports, int delay, Requirements requirements, WorldPoint source, WorldPoint destination, String npcName, String option, String... dialogueOptions) {
        LongTransport transport = LongTransport.npcDialogTransport(delay, requirements, npcName, option, 10, source, destination, dialogueOptions);
        computeIfAbsent(transports, WorldPointUtil.compress(source), transport);
    }

    private static void addObjectTransport(final TIntObjectHashMap<ArrayList<Transport>> transports, int delay, WorldPoint source, WorldPoint destination, int objectID, String action) {
        addObjectTransport(transports, delay, source, destination, objectID, action, new String[]{});
    }

    private static void addObjectTransport(final TIntObjectHashMap<ArrayList<Transport>> transports, int delay, WorldPoint source, WorldPoint destination, int objectID, String action, String... options) {
        Transport transport = LongTransport.addObjectTransport(delay, new Requirements(), source, destination, objectID, action, options);
        computeIfAbsent(transports, WorldPointUtil.compress(source), transport);
    }

    private static void addObjectTransport(final TIntObjectHashMap<ArrayList<Transport>> transports, int delay, Requirements requirements, WorldPoint source, WorldPoint destination, int objectID, String action) {
        addObjectTransport(transports, delay, requirements, source, destination, objectID, action, new String[]{});
    }

    private static void addObjectTransport(final TIntObjectHashMap<ArrayList<Transport>> transports, int delay, Requirements requirements, WorldPoint source, WorldPoint destination, int objectID, String action, String... options) {
        Transport transport = LongTransport.addObjectTransport(delay, requirements, source, destination, objectID, action, options);
        computeIfAbsent(transports, WorldPointUtil.compress(source), transport);
    }
}
