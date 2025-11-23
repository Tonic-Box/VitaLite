package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.game.GameAPI;
import com.tonic.api.game.SkillAPI;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API for interacting with the Tanner interface (widget group 324).
 * Provides methods to tan various leather types using menu actions.
 */
public class TanningAPI {
    private static final int TANNER_GROUP = 324;
    private static final int CLOSE_BUTTON = 10;

    /**
     * Enum representing the different leather types available at the tanner.
     * Each type contains metadata about input/output items, costs, and requirements.
     */
    public enum LeatherType {
        SOFT_LEATHER(92, "Soft leather", ItemID.COW_HIDE, ItemID.LEATHER, 1, 1),
        HARD_LEATHER(93, "Hard leather", ItemID.COW_HIDE, ItemID.HARD_LEATHER, 3, 28),
        SNAKESKIN(94, "Snakeskin", ItemID.VILLAGE_SNAKE_HIDE, ItemID.VILLAGE_SNAKE_SKIN, 15, 45),
        SNAKESKIN_SWAMP(95, "Snakeskin", ItemID.TEMPLETREK_SWAMP_SNAKE_HIDE, ItemID.VILLAGE_SNAKE_SKIN, 20, 45),
        GREEN_DHIDE(96, "Green d'hide", ItemID.DRAGONHIDE_GREEN, ItemID.DRAGON_LEATHER, 20, 57),
        BLUE_DHIDE(97, "Blue d'hide", ItemID.DRAGONHIDE_BLUE, ItemID.DRAGON_LEATHER_BLUE, 20, 66),
        RED_DHIDE(98, "Red d'hide", ItemID.DRAGONHIDE_RED, ItemID.DRAGON_LEATHER_RED, 20, 73),
        BLACK_DHIDE(99, "Black d'hide", ItemID.DRAGONHIDE_BLACK, ItemID.DRAGON_LEATHER_BLACK, 20, 79);

        private final int slotRoot;
        private final String displayName;
        private final int inputItemId;
        private final int outputItemId;
        private final int standardCostPerHide;
        private final int requiredCraftingLevel;

        LeatherType(int slotRoot, String displayName, int inputItemId, int outputItemId,
                    int standardCostPerHide, int requiredCraftingLevel) {
            this.slotRoot = slotRoot;
            this.displayName = displayName;
            this.inputItemId = inputItemId;
            this.outputItemId = outputItemId;
            this.standardCostPerHide = standardCostPerHide;
            this.requiredCraftingLevel = requiredCraftingLevel;
        }

        public int getSlotRoot() { return slotRoot; }
        public String getDisplayName() { return displayName; }
        public int getInputItemId() { return inputItemId; }
        public int getOutputItemId() { return outputItemId; }
        public int getCostPerHide() { return standardCostPerHide; }
        public int getRequiredCraftingLevel() { return requiredCraftingLevel; }
    }

    /**
     * Checks if the tanning interface is currently open.
     * @return true if the tanning interface is visible, false otherwise
     */
    public static boolean isOpen() {
        return WidgetAPI.isVisible(TANNER_GROUP, 0);
    }

    /**
     * Closes the tanning interface.
     */
    public static void close() {
        GameAPI.invokeMenuAction(0, 26, 0, (TANNER_GROUP << 16) | CLOSE_BUTTON, -1);
    }

    /**
     * Tans leather of the specified type.
     * @param type The leather type to tan
     * @param amount The quantity to tan: 1, 5, -1 for All, or any other value for X
     */
    public static void tan(LeatherType type, int amount) {
        if (!isValidAmount(amount)) return;

        tanAction(type.getSlotRoot(), amount);
    }

    /**
     * Tans leather by input item ID.
     * @param inputItemId The input item ID (e.g., ItemID.COW_HIDE)
     * @param amount The quantity to tan: 1, 5, -1 for All, or any other value for X
     */
    public static void tan(int inputItemId, int amount) {
        if (!isValidAmount(amount)) return;

        LeatherType type = fromInputItem(inputItemId);
        if (type == null) {
            return;
        }
        tanAction(type.getSlotRoot(), amount);
    }

    /**
     * Tans leather by item name.
     * @param itemName The name of the leather to tan (e.g., "Soft leather", "Green d'hide")
     * @param amount The quantity to tan: 1, 5, -1 for All, or any other value for X
     */
    public static void tan(String itemName, int amount) {
        if (!isValidAmount(amount)) return;

        for (LeatherType type : LeatherType.values()) {
            if (WidgetAPI.isVisible(TANNER_GROUP, type.getSlotRoot() + 16)) {
                String displayText = WidgetAPI.getText(TANNER_GROUP, type.getSlotRoot() + 16);
                if (displayText != null && displayText.contains(itemName)) {
                    tanAction(type.getSlotRoot(), amount);
                    return;
                }
            }
        }
    }

    /**
     * Check if the player has enough coins to tan the specified amount.
     * @param type The leather type to tan
     * @param amount The quantity to tan
     * @return true if a player has enough coins
     */
    public static boolean hasEnoughCoinsToPay(LeatherType type, int amount) {
        int totalCost = calculateCost(type, amount);
        return InventoryAPI.count(ItemID.COINS) >= totalCost;
    }

    /**
     * Calculate total cost for a tanning operation.
     * @param type The leather type to tan
     * @param amount The quantity to tan
     * @return Total cost in coins
     */
    public static int calculateCost(LeatherType type, int amount) {
        return type.getCostPerHide() * amount;
    }

    /**
     * Get the maximum amount that can be tanned with current coins.
     * @param type The leather type to tan
     * @return Maximum affordable amount
     */
    public static int getMaxAffordableAmount(LeatherType type) {
        int coins = InventoryAPI.count(ItemID.COINS);
        return coins / type.getCostPerHide();
    }

    /**
     * Find LeatherType by input item ID.
     * @param itemId The input item ID (e.g., cowhide, dragonhide)
     * @return Matching LeatherType or null if not found
     */
    public static LeatherType fromInputItem(int itemId) {
        for (LeatherType type : LeatherType.values()) {
            if (type.getInputItemId() == itemId) {
                return type;
            }
        }
        return null;
    }

    /**
     * Check if the player has the required input items.
     * @param type The leather type to tan
     * @param amount The quantity to tan
     * @return true if the player has enough input items
     */
    public static boolean hasInputItems(LeatherType type, int amount) {
        return InventoryAPI.count(type.getInputItemId()) >= amount;
    }

    /**
     * Check if the player meets the Crafting level requirement for this leather type.
     * @param type The leather type to check
     * @return true if the player meets the requirement
     */
    public static boolean meetsRequirement(LeatherType type) {
        return SkillAPI.getLevel(Skill.CRAFTING) >= type.getRequiredCraftingLevel();
    }

    /**
     * Get all leather types the player can currently tan based on Crafting level.
     * @return List of available leather types
     */
    public static List<LeatherType> getAvailableLeatherTypes() {
        int craftingLevel = SkillAPI.getLevel(Skill.CRAFTING);
        return Arrays.stream(LeatherType.values())
                .filter(type -> craftingLevel >= type.getRequiredCraftingLevel())
                .collect(Collectors.toList());
    }

    /**
     * Check if a player can afford and has the skill to tan.
     * @param type The leather type to tan
     * @param amount The quantity to tan
     * @return true if all requirements are met
     */
    public static boolean canTan(LeatherType type, int amount) {
        return meetsRequirement(type)
                && hasEnoughCoinsToPay(type, amount)
                && hasInputItems(type, amount);
    }

    /**
     * Gets the child widget ID for the specified amount button.
     * @param slotRoot The root child ID of the leather slot
     * @param amount The amount to tan
     * @return The child widget ID for the corresponding button
     */
    private static int getChildForAmount(int slotRoot, int amount) {
        switch (amount) {
            case 1: return slotRoot + 56;   // _1 button
            case 5: return slotRoot + 48;   // _5 button
            case -1: return slotRoot + 32;  // _ALL button
            default: return slotRoot + 40;  // _X button
        }
    }

    /**
     * Internal method that performs the actual tanning action.
     * @param slotRoot The root child ID of the leather slot
     * @param amount The quantity to tan
     */
    private static void tanAction(int slotRoot, int amount) {
        if (!isValidAmount(amount)) return;

        int childId = getChildForAmount(slotRoot, amount);
        if (childId == -1) return;

        // Send menu action with opcode 24
        GameAPI.invokeMenuAction(0, 24, 0, (TANNER_GROUP << 16) | childId, -1);

        // For the "X" option, follow up with count dialogue
        if (amount != 1 && amount != 5 && amount != -1) {
            TClient client = Static.getClient();
            Static.invoke(() -> client.getPacketWriter().resumeCountDialoguePacket(amount));
        }
    }

    /**
     * Validates the tanning amount parameter.
     * @param amount The amount to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidAmount(int amount) {
        return amount >= -1 && amount != 0;
    }
}