package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.game.GameAPI;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;

/**
 * API for interacting with the Tanner interface.
 * Provides methods to tan various leather types using menu actions.
 */
public class TanningAPI {

    /**
     * Enum representing the tanning slots in the interface.
     * Each slot contains the widget IDs for the different amount options.
     */
    public enum TanningSlot {
        SLOT_A(InterfaceID.Tanner.TANNING_A_1, InterfaceID.Tanner.TANNING_A_5,
                InterfaceID.Tanner.TANNING_A_ALL, InterfaceID.Tanner.TANNING_A_X,
                InterfaceID.Tanner.TANNING_A_TEXT),
        SLOT_B(InterfaceID.Tanner.TANNING_B_1, InterfaceID.Tanner.TANNING_B_5,
                InterfaceID.Tanner.TANNING_B_ALL, InterfaceID.Tanner.TANNING_B_X,
                InterfaceID.Tanner.TANNING_B_TEXT),
        SLOT_C(InterfaceID.Tanner.TANNING_C_1, InterfaceID.Tanner.TANNING_C_5,
                InterfaceID.Tanner.TANNING_C_ALL, InterfaceID.Tanner.TANNING_C_X,
                InterfaceID.Tanner.TANNING_C_TEXT),
        SLOT_D(InterfaceID.Tanner.TANNING_D_1, InterfaceID.Tanner.TANNING_D_5,
                InterfaceID.Tanner.TANNING_D_ALL, InterfaceID.Tanner.TANNING_D_X,
                InterfaceID.Tanner.TANNING_D_TEXT),
        SLOT_E(InterfaceID.Tanner.TANNING_E_1, InterfaceID.Tanner.TANNING_E_5,
                InterfaceID.Tanner.TANNING_E_ALL, InterfaceID.Tanner.TANNING_E_X,
                InterfaceID.Tanner.TANNING_E_TEXT),
        SLOT_F(InterfaceID.Tanner.TANNING_F_1, InterfaceID.Tanner.TANNING_F_5,
                InterfaceID.Tanner.TANNING_F_ALL, InterfaceID.Tanner.TANNING_F_X,
                InterfaceID.Tanner.TANNING_F_TEXT),
        SLOT_G(InterfaceID.Tanner.TANNING_G_1, InterfaceID.Tanner.TANNING_G_5,
                InterfaceID.Tanner.TANNING_G_ALL, InterfaceID.Tanner.TANNING_G_X,
                InterfaceID.Tanner.TANNING_G_TEXT),
        SLOT_H(InterfaceID.Tanner.TANNING_H_1, InterfaceID.Tanner.TANNING_H_5,
                InterfaceID.Tanner.TANNING_H_ALL, InterfaceID.Tanner.TANNING_H_X,
                InterfaceID.Tanner.TANNING_H_TEXT);

        public final int widgetId1;
        public final int widgetId5;
        public final int widgetIdAll;
        public final int widgetIdX;
        public final int widgetIdText;

        TanningSlot(int widgetId1, int widgetId5, int widgetIdAll, int widgetIdX, int widgetIdText) {
            this.widgetId1 = widgetId1;
            this.widgetId5 = widgetId5;
            this.widgetIdAll = widgetIdAll;
            this.widgetIdX = widgetIdX;
            this.widgetIdText = widgetIdText;
        }
    }

    /**
     * Enum representing the different leather types available at the tanner.
     * Each type contains metadata about input/output items, costs, and requirements.
     */
    public enum LeatherType {
        SOFT_LEATHER("Soft leather", ItemID.COW_HIDE, ItemID.LEATHER, 1, TanningSlot.SLOT_A),
        HARD_LEATHER("Hard leather", ItemID.COW_HIDE, ItemID.HARD_LEATHER, 3, TanningSlot.SLOT_B),
        SNAKESKIN("Snakeskin", ItemID.VILLAGE_SNAKE_HIDE, ItemID.VILLAGE_SNAKE_SKIN, 15, TanningSlot.SLOT_C),
        SNAKESKIN_SWAMP("Snakeskin", ItemID.TEMPLETREK_SWAMP_SNAKE_HIDE, ItemID.VILLAGE_SNAKE_SKIN, 20, TanningSlot.SLOT_D),
        GREEN_DHIDE("Green d'hide", ItemID.DRAGONHIDE_GREEN, ItemID.DRAGON_LEATHER, 20, TanningSlot.SLOT_E),
        BLUE_DHIDE("Blue d'hide", ItemID.DRAGONHIDE_BLUE, ItemID.DRAGON_LEATHER_BLUE, 20, TanningSlot.SLOT_F),
        RED_DHIDE("Red d'hide", ItemID.DRAGONHIDE_RED, ItemID.DRAGON_LEATHER_RED, 20, TanningSlot.SLOT_G),
        BLACK_DHIDE("Black d'hide", ItemID.DRAGONHIDE_BLACK, ItemID.DRAGON_LEATHER_BLACK, 20, TanningSlot.SLOT_H);

        public final String displayName;
        public final int inputItemId;
        public final int outputItemId;
        public final int standardCostPerHide;
        public final TanningSlot slot;

        LeatherType(String displayName, int inputItemId, int outputItemId,
                    int standardCostPerHide, TanningSlot slot) {
            this.displayName = displayName;
            this.inputItemId = inputItemId;
            this.outputItemId = outputItemId;
            this.standardCostPerHide = standardCostPerHide;
            this.slot = slot;
        }
    }

    private static final LeatherType[] LEATHER_TYPES = LeatherType.values();

    /**
     * Checks if the tanning interface is currently open.
     * @return true if the tanning interface is visible, false otherwise
     */
    public static boolean isOpen() {
        return WidgetAPI.isVisible(InterfaceID.Tanner.ROOT_GRAPHIC0);
    }

    /**
     * Closes the tanning interface.
     */
    public static void close() {
        GameAPI.invokeMenuAction(0, 26, 0, InterfaceID.Tanner.TANNING_CLOSE_BUTTON_LAYER, -1);
    }

    /**
     * Tans leather of the specified type.
     * @param type The leather type to tan
     * @param amount The quantity to tan: 1, 5, -1 for All, or any other value for X
     */
    public static void tan(LeatherType type, int amount) {
        if (amount < -1 || amount == 0) return;

        tanAction(type, amount);
    }

    /**
     * Tans leather by item name.
     * @param itemName The name of the leather to tan (e.g., "Soft leather", "Green d'hide")
     * @param amount The quantity to tan: 1, 5, -1 for All, or any other value for X
     */
    public static void tan(String itemName, int amount) {
        if (amount < -1 || amount == 0) return;

        for (LeatherType type : LEATHER_TYPES) {
            TanningSlot slot = type.slot;
            if (WidgetAPI.isVisible(slot.widgetIdText)) {
                String displayText = WidgetAPI.getText(slot.widgetIdText);
                if (displayText != null && displayText.contains(itemName)) {
                    tanAction(type, amount);
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
        return type.standardCostPerHide * amount;
    }

    /**
     * Get the maximum amount that can be tanned with current coins.
     * @param type The leather type to tan
     * @return Maximum affordable amount
     */
    public static int getMaxAffordableAmount(LeatherType type) {
        int coins = InventoryAPI.count(ItemID.COINS);
        return coins / type.standardCostPerHide;
    }

    /**
     * Check if the player has the required input items.
     * @param type The leather type to tan
     * @param amount The quantity to tan
     * @return true if the player has enough input items
     */
    public static boolean hasInputItems(LeatherType type, int amount) {
        return InventoryAPI.count(type.inputItemId) >= amount;
    }

    /**
     * Check if a player can afford and has the skill to tan.
     * @param type The leather type to tan
     * @param amount The quantity to tan
     * @return true if all requirements are met
     */
    public static boolean canTan(LeatherType type, int amount) {
        return hasEnoughCoinsToPay(type, amount) && hasInputItems(type, amount);
    }

    /**
     * Gets the widget ID for the specified amount button.
     * @param slot The tanning slot
     * @param amount The amount to tan
     * @return The widget ID for the corresponding button
     */
    private static int getWidgetForAmount(TanningSlot slot, int amount) {
        if (amount == 1) return slot.widgetId1;
        if (amount == 5) return slot.widgetId5;
        if (amount == -1) return slot.widgetIdAll;
        return slot.widgetIdX;
    }

    /**
     * Internal method that performs the actual tanning action.
     * @param type The leather type to tan
     * @param amount The quantity to tan
     */
    private static void tanAction(LeatherType type, int amount) {
        int widgetId = getWidgetForAmount(type.slot, amount);

        GameAPI.invokeMenuAction(0, 24, 0, widgetId, -1);

        // For the "X" option, follow up with count dialogue
        if (amount != 1 && amount != 5 && amount != -1) {
            TClient client = Static.getClient();
            Static.invoke(() -> client.getPacketWriter().resumeCountDialoguePacket(amount));
        }
    }
}