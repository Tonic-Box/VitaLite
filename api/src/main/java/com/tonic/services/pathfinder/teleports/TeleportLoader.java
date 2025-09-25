package com.tonic.services.pathfinder.teleports;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.game.GameAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.ItemEx;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.PacketInteractionType;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.ArrayUtils;
import java.util.*;

public class TeleportLoader {
    private static final List<Teleport> LAST_TELEPORT_LIST = new ArrayList<>();

    public static List<Teleport> buildTeleports() {
        List<Teleport> teleports = new ArrayList<>();
        teleports.addAll(LAST_TELEPORT_LIST);
        teleports.addAll(buildTimedTeleports());
        return teleports;
    }

    private static List<Teleport> buildTimedTeleports() {
        return Static.invoke(() -> {
            List<Teleport> teleports = new ArrayList<>();

            // TODO: if teleblocked return here

            //var spellTeles = getTeleportSpells();
            //teleports.addAll(spellTeles);

            // TODO: remove this when equipped items supported
            if (InventoryAPI.isEmpty()) {
                return teleports;
            }

            Client client = Static.getClient();

            for (TeleportItem tele : TeleportItem.values()) {
                if (tele.canUse() && tele.getDestination().distanceTo(client.getLocalPlayer().getWorldLocation()) > 20) {
                    switch (tele) {
                        case ROYAL_SEED_POD:
                            if (GameAPI.getWildyLevel() <= 30) {
                                teleports.add(itemTeleport(tele));
                            }
                        default:
                            if (GameAPI.getWildyLevel() <= 20) {
                                teleports.add(itemTeleport(tele));
                            }
                    }
                }
            }

            // TODO: fix this to support tele items to 30 wild
            if (GameAPI.getWildyLevel() > 20) {
                return teleports;
            }

            if (InventoryAPI.getItem(i -> ArrayUtils.contains(MovementConstants.SLAYER_RING, i.getId())) != null) {
                teleports.add(new Teleport(new WorldPoint(2432, 3423, 0), 2,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Teleport", "Stronghold Slayer Cave", MovementConstants.SLAYER_RING));
                        }}));
                teleports.add(new Teleport(new WorldPoint(3422, 3537, 0), 2,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Teleport", "Slayer Tower", MovementConstants.SLAYER_RING));
                        }}));
                teleports.add(new Teleport(new WorldPoint(2802, 10000, 0), 2,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Teleport", "Fremennik Slayer Dungeon", MovementConstants.SLAYER_RING));
                        }}));
                teleports.add(new Teleport(new WorldPoint(3185, 4601, 0), 2,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Teleport", "Tarn's Lair", MovementConstants.SLAYER_RING));
                        }}));
            }
            if (InventoryAPI.getItem(i -> ArrayUtils.contains(MovementConstants.AMULET_OF_GLORY, i.getId())) != null) {
                teleports.add(new Teleport(new WorldPoint(3087, 3496, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Edgeville", MovementConstants.AMULET_OF_GLORY));
                        }}));
                teleports.add(new Teleport(new WorldPoint(2918, 3176, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Karamja", MovementConstants.AMULET_OF_GLORY));
                        }}));
                teleports.add(new Teleport(new WorldPoint(3105, 3251, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Draynor Village", MovementConstants.AMULET_OF_GLORY));
                        }}));
                teleports.add(new Teleport(new WorldPoint(3293, 3163, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Al Kharid", MovementConstants.AMULET_OF_GLORY));
                        }}));
            }
            if (InventoryAPI.getItem(i -> ArrayUtils.contains(MovementConstants.GAMES_NECKLACE, i.getId())) != null) {
                teleports.add(new Teleport(new WorldPoint(2898, 3552, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Burthorpe", MovementConstants.GAMES_NECKLACE));
                        }}));
                teleports.add(new Teleport(new WorldPoint(2521, 3571, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Barbarian Outpost", MovementConstants.GAMES_NECKLACE));
                        }}));
                teleports.add(new Teleport(new WorldPoint(2965, 4382, 2), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Corporeal Beast", MovementConstants.GAMES_NECKLACE));
                        }}));
                teleports.add(new Teleport(new WorldPoint(3245, 9500, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Tears of Guthix", MovementConstants.GAMES_NECKLACE));
                        }}));
                teleports.add(new Teleport(new WorldPoint(1625, 3937, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Wintertodt Camp", MovementConstants.GAMES_NECKLACE));
                        }}));
            }
            if (InventoryAPI.getItem(i -> ArrayUtils.contains(MovementConstants.RING_OF_WEALTH, i.getId())) != null) {
                teleports.add(new Teleport(new WorldPoint(2535, 3862, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Miscellania", MovementConstants.RING_OF_WEALTH));
                        }}));
                teleports.add(new Teleport(new WorldPoint(3162, 3480, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Grand Exchange", MovementConstants.RING_OF_WEALTH));
                        }}));
                teleports.add(new Teleport(new WorldPoint(2995, 3375, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Falador Park", MovementConstants.RING_OF_WEALTH));
                        }}));
                teleports.add(new Teleport(new WorldPoint(2831, 10165, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Dondakan", MovementConstants.RING_OF_WEALTH));
                        }}));
            }
            if (InventoryAPI.getItem(i -> ArrayUtils.contains(MovementConstants.RING_OF_DUELING, i.getId())) != null) {
                teleports.add(new Teleport(new WorldPoint(3315, 3235, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Emir's Arena", MovementConstants.RING_OF_DUELING));
                        }}));
                teleports.add(new Teleport(new WorldPoint(2441, 3091, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Castle Wars", MovementConstants.RING_OF_DUELING));
                        }}));
                teleports.add(new Teleport(new WorldPoint(3151, 3636, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Ferox Enclave", MovementConstants.RING_OF_DUELING));
                        }}));
            }
            if (InventoryAPI.getItem(i -> ArrayUtils.contains(MovementConstants.COMBAT_BRACELET, i.getId())) != null) {
                teleports.add(new Teleport(new WorldPoint(2883, 3549, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Warriors", MovementConstants.COMBAT_BRACELET));
                        }}));
                teleports.add(new Teleport(new WorldPoint(3189, 3368, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Champions", MovementConstants.COMBAT_BRACELET));
                        }}));
                teleports.add(new Teleport(new WorldPoint(3053, 3487, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Monastery", MovementConstants.COMBAT_BRACELET));
                        }}));
                teleports.add(new Teleport(new WorldPoint(2654, 3441, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Ranging", MovementConstants.COMBAT_BRACELET));
                        }}));
            }
            if (InventoryAPI.getItem(i -> ArrayUtils.contains(MovementConstants.SKILLS_NECKLACE, i.getId())) != null) {
                teleports.add(new Teleport(new WorldPoint(2612, 3391, 0), 4,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport2(0, 12255235, "Rub", MovementConstants.SKILLS_NECKLACE));
                        }}));
                teleports.add(new Teleport(new WorldPoint(3049, 9764, 0), 4,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport2(1, 12255235, "Rub", MovementConstants.SKILLS_NECKLACE));
                        }}));
                teleports.add(new Teleport(new WorldPoint(2933, 3297, 0), 4,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport2(2, 12255235, "Rub", MovementConstants.SKILLS_NECKLACE));
                        }}));
                teleports.add(new Teleport(new WorldPoint(3145, 3439, 0), 2,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport2(3, 12255235, "Rub", MovementConstants.SKILLS_NECKLACE));
                        }}));
                teleports.add(new Teleport(new WorldPoint(1662, 3505, 0), 3,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport2(4, 12255235, "Rub", MovementConstants.SKILLS_NECKLACE));
                        }}));
                teleports.add(new Teleport(new WorldPoint(1249, 3718, 0), 3,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport2(5, 12255235, "Rub", MovementConstants.SKILLS_NECKLACE));
                        }}));
            }
            if (InventoryAPI.getItem(i -> ArrayUtils.contains(MovementConstants.DIGSITE_PENDANT, i.getId())) != null) {
                //TODO
            }
            if (InventoryAPI.getItem(i -> ArrayUtils.contains(MovementConstants.NECKLACE_OF_PASSAGE, i.getId())) != null) {
                teleports.add(new Teleport(new WorldPoint(3114, 3181, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Wizards' Tower", MovementConstants.NECKLACE_OF_PASSAGE));
                        }}));
                teleports.add(new Teleport(new WorldPoint(2431, 3348, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "The Outpost", MovementConstants.NECKLACE_OF_PASSAGE));
                        }}));
                teleports.add(new Teleport(new WorldPoint(3406, 3157, 0), 5,
                        new ArrayList<>() {{
                            addAll(jewelryTeleport("Rub", "Eagle's Eyrie", MovementConstants.NECKLACE_OF_PASSAGE));
                        }}));
            }
            if (InventoryAPI.getItem(i -> ArrayUtils.contains(MovementConstants.BURNING_AMULET, i.getId())) != null) {
                //TODO
            }

            return teleports;
        });
    }

    public static List<Teleport> getTeleportSpells() {
        var teleports = new ArrayList<Teleport>();

//        if(GameAPI.getWildyLevel(client) > 20)
//        {
//            return teleports;
//        }
//
//        var canCastAnything = Inventory.contains(client, ItemID.LAW_RUNE)
//                || RunePouch.getRunePouch(client) != null;
//
//        if(!canCastAnything){
//            // only home teleport can be used
//            var homeTeleport = TeleportSpell.getHomeTeleport(client);
//            if(homeTeleport.canCast(client) && homeTeleport.distanceFromPoint(client) > 50)
//            {
//                teleports.add(Teleport.fromSpell(homeTeleport));
//            }
//            return teleports;
//        }
//
//        for (TeleportSpell teleportSpell : TeleportSpell.values()) {
//            if (teleportSpell.canCast(client) && teleportSpell.distanceFromPoint(client) > 50)
//            {
//                teleports.add(Teleport.fromSpell(teleportSpell));
//            }
//        }

        return teleports;
    }

    public static Teleport itemTeleport(TeleportItem teleportItem) {
        return new Teleport(teleportItem.getDestination(), 5, new ArrayList<>() {{
            add(() ->
            {
                ItemEx item = InventoryAPI.getItem(i -> ArrayUtils.contains(teleportItem.getItemId(), i.getId()));
                if (item != null) {
                    InventoryAPI.interact(item, teleportItem.getAction());
                }
            });
        }});
    }

    public static List<Runnable> jewelryTeleport(String itemAction, String target, int... ids) {
        return new ArrayList<>() {{
            add(() -> {
                ItemEx item = InventoryAPI.getItem(i -> ArrayUtils.contains(ids, i.getId()));
                if (item == null)
                    return;
                InventoryAPI.interact(item, itemAction);
            });
            add(() -> {
                if (!DialogueAPI.dialoguePresent())
                    return;
                DialogueAPI.selectOption(target);
            });
            add(() -> DialogueAPI.selectOption(target));
        }};
    }

    public static List<Runnable> jewelryTeleport2(int option, int WidgetId, String itemAction, int... ids) {
        return new ArrayList<>() {{
            add(() -> {
                ItemEx item = InventoryAPI.getItem(i -> ArrayUtils.contains(ids, i.getId()));
                if (item == null)
                    return;
                InventoryAPI.interact(item, itemAction);
            });
            add(() -> {
                TClient tClient = Static.getClient();
                Static.invoke(() -> {
                    ClickManager.click(PacketInteractionType.UNBOUND_INTERACT);
                    tClient.invokeMenuAction("", "", 0, 30, option, WidgetId, -1, -1, -1);
                });
            });
            add(() -> {
                TClient tClient = Static.getClient();
                Static.invoke(() -> {
                    tClient.getPacketWriter().clickPacket(0, -1, -1);
                    tClient.invokeMenuAction("", "", 0, 30, option, WidgetId, -1, -1, -1);
                });
            });
        }};
    }
}