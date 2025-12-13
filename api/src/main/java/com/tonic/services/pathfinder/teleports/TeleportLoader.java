package com.tonic.services.pathfinder.teleports;

import com.tonic.Static;
import com.tonic.api.game.GameAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.game.WorldsAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.PlayerEx;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import org.apache.commons.lang3.ArrayUtils;
import java.util.*;
import java.util.function.Predicate;

public class TeleportLoader {
    public static List<Teleport> buildTeleports() {
        return Static.invoke(() -> {
            List<Teleport> teleports = new ArrayList<>();

            // TODO: if teleblocked return here
            // Client client = Static.getClient();
            // if (client.getVarbitValue(VarbitID.TELEBLOCK_CYCLES) > 0)
            // return teleports;

            // var spellTeles = getTeleportSpells();
            // teleports.addAll(spellTeles);

            if (InventoryAPI.isEmpty() && EquipmentAPI.getAll().isEmpty()) {
                return teleports;
            }

            Client client = Static.getClient();

            for (TeleportItem tele : TeleportItem.values()) {
                if (tele.canUse() && tele.getDestination().distanceTo(PlayerEx.getLocal().getWorldPoint()) > 20) {
                    if (!membersCheck(tele.getItemId()))
                        continue;

                    if (tele == TeleportItem.ROYAL_SEED_POD) {
                        if (GameAPI.getWildyLevel() <= 30) {
                            teleports.add(itemTeleport(tele));
                        }
                    }
                    if (GameAPI.getWildyLevel() <= 20) {
                        teleports.add(itemTeleport(tele));
                    }
                }
            }

            // TODO: fix this to support tele items to 30 wild
            if (GameAPI.getWildyLevel() > 20) {
                return teleports;
            }

            boolean inMembers = WorldsAPI.inMembersWorld();

            if (getTeleportItem(MovementConstants.SLAYER_RING) != null && inMembers) {
                teleports.add(new Teleport(new WorldPoint(2432, 3423, 0), 2,
                        jewelryTeleport("Teleport", "Stronghold", MovementConstants.SLAYER_RING)));
                // todo if we have priest in peril
                teleports.add(new Teleport(new WorldPoint(3422, 3537, 0), 2,
                        jewelryTeleport("Teleport", "Slayer Tower", MovementConstants.SLAYER_RING)));
                teleports.add(new Teleport(new WorldPoint(2802, 10000, 0), 2,
                        jewelryTeleport("Teleport", "Fremennik", MovementConstants.SLAYER_RING)));
                // todo if we have haunted mine
                teleports.add(new Teleport(new WorldPoint(3185, 4601, 0), 2,
                        jewelryTeleport("Teleport", "Tarn's Lair", MovementConstants.SLAYER_RING)));
            }
            if (getTeleportItem(MovementConstants.AMULET_OF_GLORY) != null && inMembers) {
                teleports.add(new Teleport(new WorldPoint(3087, 3496, 0), 5,
                        jewelryTeleport("Rub", "Edgeville", MovementConstants.AMULET_OF_GLORY)));
                teleports.add(new Teleport(new WorldPoint(2918, 3176, 0), 5,
                        jewelryTeleport("Rub", "Karamja", MovementConstants.AMULET_OF_GLORY)));
                teleports.add(new Teleport(new WorldPoint(3105, 3251, 0), 5,
                        jewelryTeleport("Rub", "Draynor Village", MovementConstants.AMULET_OF_GLORY)));
                teleports.add(new Teleport(new WorldPoint(3293, 3163, 0), 5,
                        jewelryTeleport("Rub", "Al Kharid", MovementConstants.AMULET_OF_GLORY)));
            }
            if (getTeleportItem(MovementConstants.GAMES_NECKLACE) != null && inMembers) {
                teleports.add(new Teleport(new WorldPoint(2898, 3552, 0), 5,
                        jewelryTeleport("Rub", "Burthorpe", MovementConstants.GAMES_NECKLACE)));
                teleports.add(new Teleport(new WorldPoint(2521, 3571, 0), 5,
                        jewelryTeleport("Rub", "Barbarian Outpost", MovementConstants.GAMES_NECKLACE)));
                teleports.add(new Teleport(new WorldPoint(2965, 4382, 2), 5,
                        jewelryTeleport("Rub", "Corporeal Beast", MovementConstants.GAMES_NECKLACE)));
                teleports.add(new Teleport(new WorldPoint(3245, 9500, 0), 5,
                        jewelryTeleport("Rub", "Tears of Guthix", MovementConstants.GAMES_NECKLACE)));
                teleports.add(new Teleport(new WorldPoint(1625, 3937, 0), 5,
                        jewelryTeleport("Rub", "Wintertodt Camp", MovementConstants.GAMES_NECKLACE)));
            }
            if (getTeleportItem(MovementConstants.RING_OF_WEALTH) != null && inMembers) {
                teleports.add(new Teleport(new WorldPoint(2535, 3862, 0), 5,
                        jewelryTeleport("Rub", "Miscellania", MovementConstants.RING_OF_WEALTH)));
                teleports.add(new Teleport(new WorldPoint(3162, 3480, 0), 5,
                        jewelryTeleport("Rub", "Grand Exchange", MovementConstants.RING_OF_WEALTH)));
                teleports.add(new Teleport(new WorldPoint(2995, 3375, 0), 5,
                        jewelryTeleport("Rub", "Falador", MovementConstants.RING_OF_WEALTH)));
                // todo if we have the "between a rock" quest
                teleports.add(new Teleport(new WorldPoint(2831, 10165, 0), 5,
                        jewelryTeleport("Rub", "Dondakan", MovementConstants.RING_OF_WEALTH)));
            }
            if (getTeleportItem(MovementConstants.RING_OF_DUELING) != null && inMembers) {
                teleports.add(new Teleport(new WorldPoint(3315, 3235, 0), 5,
                        jewelryTeleport("Rub", "Emir's Arena", MovementConstants.RING_OF_DUELING)));
                teleports.add(new Teleport(new WorldPoint(2441, 3091, 0), 5,
                        jewelryTeleport("Rub", "Castle Wars", MovementConstants.RING_OF_DUELING)));
                teleports.add(new Teleport(new WorldPoint(3151, 3636, 0), 5,
                        jewelryTeleport("Rub", "Ferox Enclave", MovementConstants.RING_OF_DUELING)));
                if (VarAPI.getVarp(VarPlayerID.COLOSSEUM_GLORY) >= 12000) {
                    teleports.add(new Teleport(new WorldPoint(1791, 3107, 0), 3,
                            jewelryTeleport("Rub", "Fortis Colosseum", MovementConstants.RING_OF_DUELING)));
                }
            }
            if (getTeleportItem(MovementConstants.COMBAT_BRACELET) != null && inMembers) {
                teleports.add(new Teleport(new WorldPoint(2883, 3549, 0), 5,
                        jewelryTeleport("Rub", "Warriors' Guild", MovementConstants.COMBAT_BRACELET)));
                teleports.add(new Teleport(new WorldPoint(3189, 3368, 0), 5,
                        jewelryTeleport("Rub", "Champions' Guild", MovementConstants.COMBAT_BRACELET)));
                teleports.add(new Teleport(new WorldPoint(3053, 3487, 0), 5,
                        jewelryTeleport("Rub", "Monastery", MovementConstants.COMBAT_BRACELET)));
                teleports.add(new Teleport(new WorldPoint(2654, 3441, 0), 5,
                        jewelryTeleport("Rub", "Ranging Guild", MovementConstants.COMBAT_BRACELET)));
            }
            if (getTeleportItem(MovementConstants.SKILLS_NECKLACE) != null && inMembers) {
                teleports.add(new Teleport(new WorldPoint(2612, 3391, 0), 4,
                        jewelryTeleport("Rub", "Fishing Guild", MovementConstants.SKILLS_NECKLACE)));
                teleports.add(new Teleport(new WorldPoint(3049, 9764, 0), 4,
                        jewelryTeleport("Rub", "Mining Guild", MovementConstants.SKILLS_NECKLACE)));
                teleports.add(new Teleport(new WorldPoint(2933, 3297, 0), 4,
                        jewelryTeleport("Rub", "Crafting Guild", MovementConstants.SKILLS_NECKLACE)));
                teleports.add(new Teleport(new WorldPoint(3145, 3439, 0), 2,
                        jewelryTeleport("Rub", "Cooking Guild", MovementConstants.SKILLS_NECKLACE)));
                teleports.add(new Teleport(new WorldPoint(1662, 3505, 0), 3,
                        jewelryTeleport("Rub", "Woodcutting Guild", MovementConstants.SKILLS_NECKLACE)));
                teleports.add(new Teleport(new WorldPoint(1249, 3718, 0), 3,
                        jewelryTeleport("Rub", "Farming Guild", MovementConstants.SKILLS_NECKLACE)));
            }
            if (InventoryAPI.getItem(i -> ArrayUtils.contains(MovementConstants.DIGSITE_PENDANT, i.getId())) != null
                    && inMembers) {
                teleports.add(new Teleport(new WorldPoint(3341, 3444, 0), 3,
                        jewelryTeleport("Rub", "Digsite", MovementConstants.DIGSITE_PENDANT)));
                // TODO implement requirements for Fossil Island and Lithkren
            }
            if (getTeleportItem(MovementConstants.NECKLACE_OF_PASSAGE) != null && inMembers) {
                teleports.add(new Teleport(new WorldPoint(3114, 3181, 0), 5,
                        jewelryTeleport("Rub", "Wizards' Tower", MovementConstants.NECKLACE_OF_PASSAGE)));
                teleports.add(new Teleport(new WorldPoint(2431, 3348, 0), 5,
                        jewelryTeleport("Rub", "The Outpost", MovementConstants.NECKLACE_OF_PASSAGE)));
                teleports.add(new Teleport(new WorldPoint(3406, 3157, 0), 5,
                        jewelryTeleport("Rub", "Eagles' Eyrie", MovementConstants.NECKLACE_OF_PASSAGE)));
            }
            if (InventoryAPI.getItem(i -> ArrayUtils.contains(MovementConstants.BURNING_AMULET, i.getId())) != null) {
                teleports.add(new Teleport(new WorldPoint(3236, 3635, 0), 5,
                        jewelryTeleport("Rub", "Chaos Temple", MovementConstants.BURNING_AMULET)));
                teleports.add(new Teleport(new WorldPoint(3038, 3697, 0), 5,
                        jewelryTeleport("Rub", "Bandit Camp", MovementConstants.BURNING_AMULET)));
                teleports.add(new Teleport(new WorldPoint(3029, 3843, 0), 5,
                        jewelryTeleport("Rub", "Lava Maze", MovementConstants.BURNING_AMULET)));
            }

            return teleports;
        });
    }

    // public static List<Teleport> getTeleportSpells() {
    // var teleports = new ArrayList<Teleport>();
    //
    // if(GameAPI.getWildyLevel(client) > 20)
    // {
    // return teleports;
    // }
    //
    // var canCastAnything = Inventory.contains(client, ItemID.LAW_RUNE)
    // || RunePouch.getRunePouch(client) != null;
    //
    // if(!canCastAnything){
    // // only home teleport can be used
    // var homeTeleport = TeleportSpell.getHomeTeleport(client);
    // if(homeTeleport.canCast(client) && homeTeleport.distanceFromPoint(client) >
    // 50)
    // {
    // teleports.add(Teleport.fromSpell(homeTeleport));
    // }
    // return teleports;
    // }
    //
    // for (TeleportSpell teleportSpell : TeleportSpell.values()) {
    // if (teleportSpell.canCast(client) && teleportSpell.distanceFromPoint(client)
    // > 50)
    // {
    // teleports.add(Teleport.fromSpell(teleportSpell));
    // }
    // }
    //
    // return teleports;
    // }

    public static Teleport itemTeleport(TeleportItem teleportItem) {
        return new Teleport(teleportItem.getDestination(), 5, new ArrayList<>() {
            {
                add(() -> {
                    ItemEx item = InventoryAPI.getItem(i -> ArrayUtils.contains(teleportItem.getItemId(), i.getId()));
                    if (item != null) {
                        InventoryAPI.interact(item, teleportItem.getAction());
                    }
                });
            }
        });
    }

    private static ItemEx getTeleportItem(int... ids) {
        ItemEx eq = EquipmentAPI.getItem(i -> ArrayUtils.contains(ids, i.getId()));
        if (eq != null)
            return eq;
        return InventoryAPI.getItem(i -> ArrayUtils.contains(ids, i.getId()));
    }

    public static StepHandler jewelryTeleport(String itemAction, String target, int... ids) {
        return HandlerBuilder.get()
                .add(0, () -> {

                    ItemEx eqItem = EquipmentAPI.getItem(i -> ArrayUtils.contains(ids, i.getId()));
                    if (eqItem != null) {
                        EquipmentAPI.interact(eqItem, target);
                        return 1;
                    }
                    ItemEx invItem = InventoryAPI.getItem(i -> ArrayUtils.contains(ids, i.getId()));
                    if (invItem == null)
                        return 0;
                    Static.invoke(() -> InventoryAPI.interactSubOp(invItem, itemAction, target));
                    return 1;
                })
                .addDelay(1, 1)
                .add(2, () -> {
                    if (DialogueAPI.optionPresent(target)) {
                        DialogueAPI.selectOption(target);
                    }
                    return 3;
                })
                .addDelay(3, 3)
                .build();
    }

    private static boolean membersCheck(int... ids) {
        if (WorldsAPI.inMembersWorld())
            return true;

        ItemEx item = InventoryAPI.search().withId(ids).first();

        return item != null && !item.getName().endsWith("(Members)");
    }
}