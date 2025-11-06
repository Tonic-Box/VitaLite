package com.tonic.data.runecrafting;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Enum representing different essence pouch types with their properties.
 * This enum acts as a pure configuration model and is client-agnostic.
 */
public enum PouchType {
    SMALL(ItemID.RCU_POUCH_SMALL, 3, VarbitID.SMALL_ESSENCE_POUCH, -1, new int[]{}, -1),
    MEDIUM(ItemID.RCU_POUCH_MEDIUM, 6, VarbitID.MEDIUM_ESSENCE_POUCH, VarPlayerID.RCU_POUCH_DEGRADATION_MED, new int[]{800, 0, 400, 3}, ItemID.RCU_POUCH_MEDIUM_DEGRADE),
    LARGE(ItemID.RCU_POUCH_LARGE, 9, VarbitID.LARGE_ESSENCE_POUCH, VarPlayerID.RCU_POUCH_DEGRADATION_LARGE, new int[]{1000, 0, 800, 3, 600, 5, 400, 7}, ItemID.RCU_POUCH_LARGE_DEGRADE),
    GIANT(ItemID.RCU_POUCH_GIANT, 12, VarbitID.GIANT_ESSENCE_POUCH, VarPlayerID.RCU_POUCH_DEGRADATION_GIANT, new int[]{1200, 0, 1000, 3, 800, 5, 600, 6, 400, 7, 300, 8, 200, 9}, ItemID.RCU_POUCH_GIANT_DEGRADE),
    COLOSSAL(ItemID.RCU_POUCH_COLOSSAL, 40, VarbitID.COLOSSAL_ESSENCE_POUCH, VarbitID.RCU_POUCH_DEGRADATION_COLOSSAL, new int[]{1020, 0, 1015, 5, 995, 10, 950, 15, 870, 20, 745, 25, 565, 30, 320, 35}, ItemID.RCU_POUCH_COLOSSAL_DEGRADE);

    private final int itemId;
    private final int maxCapacity;
    private final int amountVarbit;
    private final int degradationVar;
    private final int[] degradationLevels;
    private final int degradedId;

    PouchType(int itemId, int maxCapacity, int amountVarbit, int degradationVar,
              int[] degradationLevels, int degradedId) {
        this.itemId = itemId;
        this.maxCapacity = maxCapacity;
        this.amountVarbit = amountVarbit;
        this.degradationVar = degradationVar;
        this.degradationLevels = degradationLevels;
        this.degradedId = degradedId;
    }

    // --- Public Getters ---
    public int getItemId() { return itemId; }
    public int getMaxCapacity() { return maxCapacity; }
    public int getAmountVarbit() { return amountVarbit; }
    public int getDegradationVar() { return degradationVar; }
    public int getDegradedId() { return degradedId; }
    public boolean canDegrade() { return degradedId != -1; }
    public int[] getDegradationLevels() { return degradationLevels; }

    /**
     * Convert durability value to essence count using formula.
     * Formula: essence = ceil(0.4 * durability^1.07)
     *
     * @param durability the durability value
     * @return estimated essence count
     */
    public static int durabilityToEssenceCount(int durability) {
        return (int) Math.ceil(0.4 * Math.pow(durability, 1.07));
    }

    /**
     * Find a PouchType by its item ID (works for both normal and degraded versions).
     * @param itemId the item ID to look up
     * @return matching PouchType, or null if not found
     */
    public static PouchType fromItemId(int itemId) {
        for (PouchType type : values()) {
            if (type.itemId == itemId || type.degradedId == itemId) {
                return type;
            }
        }
        return null;
    }

    /**
     * Get pouch types ordered by capacity (largest to smallest).
     * This ensures consistent ordering for fillAll/emptyAll operations.
     * @return array of PouchTypes ordered by descending capacity
     */
    public static PouchType[] getOrderedTypes() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(PouchType::getMaxCapacity).reversed())
                .toArray(PouchType[]::new);
    }
}
