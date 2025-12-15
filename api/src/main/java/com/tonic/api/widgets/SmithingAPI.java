package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.game.GameAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.data.smithing.*;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.services.pathfinder.requirements.Requirements;

import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API for interacting with the OSRS Smithing interface.
 * Provides methods to smith various items, check smithing conditions,
 * and query available smithable items by bar type.
 */
public class SmithingAPI {
    // ==================== Constants & Reverse Lookup ====================

    private static final Map<BarType, List<SmithableItem>> ITEMS_BY_BAR_TYPE = Map.of(
            BarType.BRONZE, Arrays.asList(BronzeItem.values()),
            BarType.IRON, Arrays.asList(IronItem.values()),
            BarType.STEEL, Arrays.asList(SteelItem.values()),
            BarType.MITHRIL, Arrays.asList(MithrilItem.values()),
            BarType.ADAMANT, Arrays.asList(AdamantItem.values()),
            BarType.RUNE, Arrays.asList(RuneItem.values())
    );

    private static final Map<SmithableItem, BarType> ITEM_TO_BAR_TYPE;
    static {
        Map<SmithableItem, BarType> map = new HashMap<>();
        ITEMS_BY_BAR_TYPE.forEach((barType, items) ->
                items.forEach(item -> map.put(item, barType)));
        ITEM_TO_BAR_TYPE = Collections.unmodifiableMap(map);
    }

    // ==================== Interface State Methods ====================

    /**
     * Checks if the smithing interface is currently open.
     *
     * @return true if the smithing interface is visible, false otherwise
     */
    public static boolean isOpen() {
        return WidgetAPI.isVisible(InterfaceID.Smithing.FRAME);
    }

    /**
     * Closes the smithing interface if it is open.
     */
    public static void close() {
        GameAPI.invokeMenuAction(0, 57, 11, 20447233, -1);
    }

    /**
     * Checks if the player is currently smithing.
     *
     * @return true if the player is actively smithing, false otherwise
     */
    public static boolean isSmithing() {
        int currentAnim = PlayerEx.getLocal().getPlayer().getAnimation();
        boolean isInSmithingAnim = currentAnim == AnimationID.HUMAN_SMITHING
                || currentAnim == AnimationID.HUMAN_SMITHING_IMCANDO_HAMMER;

        return !isOpen() && VarAPI.getVar(VarPlayerID.MAKEXCRAFTING) > 0 && isInSmithingAnim;
    }

    // ==================== Core Smithing Actions ====================

    /**
     * Attempts to smith a specified item with the given quantity.
     *
     * @param item   the item to smith (must not be null)
     * @param amount the quantity: 1, 5, 10, -1 for All, or any other value for X
     * @param barId  the ID of the bar type to use
     * @return true if smithing was initiated successfully, false otherwise
     */
    public static boolean smith(SmithableItem item, int amount, int barId) {
        if (item == null) return false;
        if (amount < -1 || amount == 0) return false;
        if (!isOpen()) return false;
        if (!isAccessible(item)) return false;
        if (!hasHammer()) return false;
        if (!hasEnoughBars(item, amount, barId)) return false;
        if (getBarTypeForItem(item) != BarType.fromBarId(barId)) return false;

        smithAction(item, amount);
        return true;
    }

    // ==================== Requirement & Validation Methods ====================

    /**
     * Checks if the player has a hammer required for smithing.
     * Accepts either a regular hammer or Imcando hammer in inventory or equipped.
     *
     * @return true if a player has a hammer available, false otherwise
     */
    public static boolean hasHammer() {
        return InventoryAPI.contains(ItemID.HAMMER)
                || InventoryAPI.contains(ItemID.IMCANDO_HAMMER)
                || EquipmentAPI.isEquipped(ItemID.IMCANDO_HAMMER);
    }

    /**
     * Checks if a smithable item is accessible to the player.
     * An item is accessible if it has no requirements or all requirements are met.
     *
     * @param item the item to check
     * @return true if the item is accessible, false if null or requirements not met
     */
    public static boolean isAccessible(SmithableItem item) {
        if (item == null) {
            return false;
        }
        Requirements reqs = item.getRequirements();
        return reqs == null || reqs.fulfilled();
    }

    /**
     * Checks if the player has enough bars to smith the specified quantity of an item.
     *
     * @param item   the item to check
     * @param amount the quantity to smith (-1 checks for at least 1 bar per item)
     * @param barId  the ID of the bar type to check
     * @return true if sufficient bars are available, false otherwise
     */
    public static boolean hasEnoughBars(SmithableItem item, int amount, int barId) {
        // For ALL we only validate if we have enough for the first item
        if (amount == -1) {
            return InventoryAPI.count(barId) >= item.getBarCount();
        }

        return InventoryAPI.count(barId) >= item.getBarCount() * amount;
    }

    /**
     * Checks if the player can smith a specific item with the given quantity.
     * Combines checks for bars, hammer, and accessibility.
     *
     * @param item   the item to check
     * @param amount the quantity to smith
     * @param barId  the ID of the bar type to use
     * @return true if all conditions are met to smith the item, false otherwise
     */
    public static boolean canSmith(SmithableItem item, int amount, int barId) {
        return hasEnoughBars(item, amount, barId)
                && hasHammer()
                && isAccessible(item);
    }

    /**
     * Calculates the maximum number of items that can be smithed with available bars.
     *
     * @param item  the item to check
     * @param barId the ID of the bar type to use
     * @return the maximum smithable quantity (0 if no bars available)
     */
    public static int getMaxSmithableAmount(SmithableItem item, int barId) {
        int bars = InventoryAPI.count(barId);
        return bars / item.getBarCount();
    }

    // ==================== Item Lookup Methods ====================

    /**
     * Finds a smithable item by its base name within a specific bar type.
     * The base name excludes the metal prefix (e.g., "platebody" instead of "Bronze platebody").
     * Search is case-insensitive and trims whitespace.
     *
     * @param barType  the bar type to search within
     * @param baseName the base name of the item (e.g., "sword", "platebody")
     * @return the matching SmithableItem, or null if not found
     */
    public static SmithableItem getItemByName(BarType barType, String baseName) {
        if (baseName == null) return null;

        String target = baseName.trim().toLowerCase();
        return getItemsForBarType(barType).stream()
                .filter(item -> {
                    String dn = item.getDisplayName();
                    if (dn == null) return false;
                    int idx = dn.indexOf(' ');
                    String base = idx > 0 ? dn.substring(idx + 1).toLowerCase() : dn.toLowerCase();
                    return base.equals(target);
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all smithable items available for a specific bar type.
     *
     * @param barType the bar type to query
     * @return a list of smithable items for the bar type (empty list if none)
     */
    public static List<SmithableItem> getItemsForBarType(BarType barType) {
        return ITEMS_BY_BAR_TYPE.getOrDefault(barType, Collections.emptyList());
    }

    // ==================== Private Helper Methods ====================

    /**
     * Gets the widget ID for the specified quantity button.
     *
     * @param amount the quantity (1, 5, 10, -1 for All, or other for X)
     * @return the corresponding widget ID
     */
    private static int getWidgetForAmount(int amount) {
        if (amount == 1) return InterfaceID.Smithing.MAKE_1;
        if (amount == 5) return InterfaceID.Smithing.MAKE_5;
        if (amount == 10) return InterfaceID.Smithing.MAKE_10;
        if (amount == -1) return InterfaceID.Smithing.MAKE_ALL;
        return InterfaceID.Smithing.MAKE_X;
    }

    /**
     * Checks if the specified quantity is already selected via VarPlayer MAKEXCRAFTING.
     *
     * @param amount the quantity to check
     * @return true if the quantity is already set, false otherwise
     */
    private static boolean isQuantityAlreadySet(int amount) {
        int currentAmount = VarAPI.getVar(VarPlayerID.MAKEXCRAFTING);

        if (amount == 1) return currentAmount == 1;
        if (amount == 5) return currentAmount == 5;
        if (amount == 10) return currentAmount == 10;
        if (amount == -1) return currentAmount == 0; // "All" shows as 0
        // For X amounts, check if current amount matches desired X amount
        return currentAmount == amount;
    }

    /**
     * Sets the quantity selection in the smithing interface.
     * For custom amounts (X), this method also sends the count dialogue packet.
     *
     * @param amount the quantity to set
     */
    private static void setQuantity(int amount) {
        int quantityWidget = getWidgetForAmount(amount);
        if (!WidgetAPI.isVisible(quantityWidget)) return;

        WidgetAPI.interact(1, quantityWidget, -1, -1);

        // For the "X" option, follow up with count dialogue
        if (amount != 1 && amount != 5 && amount != 10 && amount != -1) {
            TClient client = Static.getClient();
            Static.invoke(() -> client.getPacketWriter().resumeCountDialoguePacket(amount));
        }
    }

    /**
     * Performs the actual smithing action by setting quantity and clicking the item.
     *
     * @param item   the item to smith
     * @param amount the quantity to smith
     */
    private static void smithAction(SmithableItem item, int amount) {
        if (!isQuantityAlreadySet(amount)) {
            setQuantity(amount);
        }

        WidgetAPI.interact(1, item.getInterfaceID(), -1, -1);
    }

    /**
     * Determines the BarType associated with a given SmithableItem.
     *
     * @param item the item to check
     * @return the BarType for the item, or null if not found
     */
    private static BarType getBarTypeForItem(SmithableItem item) {
        return ITEM_TO_BAR_TYPE.get(item);
    }
}
