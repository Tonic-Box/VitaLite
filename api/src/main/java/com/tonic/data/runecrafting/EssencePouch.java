package com.tonic.data.runecrafting;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.queries.InventoryQuery;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class for managing runecrafting essence pouches.
 * Provides methods to fill, empty, and track pouch degradation.
 * <p>
 * This class represents an instance of a pouch in the player's inventory
 * and depends on the client state. For static pouch data, see {@link PouchType}.
 */
public class EssencePouch {
    public static final int[] ESSENCE_IDS = {
            ItemID.BLANKRUNE,
            ItemID.BLANKRUNE_HIGH,
            ItemID.BLANKRUNE_DAEYALT,
            ItemID.GOTR_GUARDIAN_ESSENCE
    };

    public static final int[] ALL_POUCH_IDS = {
            ItemID.RCU_POUCH_SMALL, ItemID.RCU_POUCH_MEDIUM, ItemID.RCU_POUCH_LARGE, ItemID.RCU_POUCH_GIANT, ItemID.RCU_POUCH_COLOSSAL,
            ItemID.RCU_POUCH_MEDIUM_DEGRADE, ItemID.RCU_POUCH_LARGE_DEGRADE, ItemID.RCU_POUCH_GIANT_DEGRADE, ItemID.RCU_POUCH_COLOSSAL_DEGRADE
    };

    private final ItemEx item;
    private final PouchType pouchType;

    private EssencePouch(ItemEx item, PouchType pouchType) {
        this.item = item;
        this.pouchType = pouchType;
    }

    // --- Private Client-Dependent State Readers ---

    /**
     * Get the current amount of essence in this pouch from varbit.
     * @param type the PouchType model
     * @return amount of essence currently stored in the pouch
     */
    private static int getCurrentAmountFromVar(PouchType type) {
        return VarAPI.getVar(type.getAmountVarbit());
    }

    /**
     * Get the current degradation value for this pouch type.
     * @param type the PouchType model
     * @return degradation value, or 0 if pouch cannot degrade
     */
    private static int getDegradation(PouchType type) {
        if (!type.canDegrade()) {
            return 0;
        }

        int degradationVar = type.getDegradationVar();

        if (type == PouchType.COLOSSAL) {
            return VarAPI.getVar(degradationVar);
        }
        return VarAPI.getVarp(degradationVar);
    }

    // --- Private Pure Logic Helpers ---

    /**
     * Helper to determine Colossal Pouch capacity scaling based on Runecraft level.
     * @param rc Runecraft level
     * @return Max essence capacity (scaled: 8, 16, 27, or 40)
     */
    private static int getColossalPouchLevelScale(int rc) {
        if (rc >= 85) {
            return 40;
        } else if (rc >= 75) {
            return 27;
        } else if (rc >= 50) {
            return 16;
        } else {
            return 8;
        }
    }

    /**
     * Calculate actual capacity based on degradation and runecraft level.
     * This is a pure function that requires client state (rcLevel) to be passed in.
     *
     * @param type    the PouchType model
     * @param rcLevel the player's current Runecraft level
     * @return actual capacity the pouch can currently hold
     */
    private static int getActualCapacity(PouchType type, int rcLevel) {
        int deg = getDegradation(type);
        int limit = type.getMaxCapacity();
        int[] degradationLevels = type.getDegradationLevels();

        // Find capacity based on degradation level
        for (int i = 0; i < degradationLevels.length; i += 2) {
            if (deg >= degradationLevels[i]) {
                limit = degradationLevels[i + 1];
                break;
            }
        }

        // Apply runecraft level scaling for colossal pouch
        if (type == PouchType.COLOSSAL && limit > 0) {
            int scaledMax = getColossalPouchLevelScale(rcLevel);
            limit = Math.max(1, (limit * scaledMax) / 40);
        }

        return limit;
    }

    /**
     * Get the next degradation breakpoint value.
     * @param type the PouchType model
     * @return next degradation threshold, or current degradation if at max
     */
    private static int getNextDegradationBreakpoint(PouchType type) {
        int deg = getDegradation(type);
        int[] degradationLevels = type.getDegradationLevels();
        for (int i = degradationLevels.length - 2; i >= 0; i -= 2) {
            if (deg < degradationLevels[i]) {
                return degradationLevels[i];
            }
        }
        return deg;
    }

    /**
     * Calculate remaining fills before the next degradation breakpoint.
     * This is a pure function that requires client state (rcLevel) to be passed in.
     *
     * @param type    the PouchType model
     * @param rcLevel the player's current Runecraft level
     * @return number of fills remaining, or Integer.MAX_VALUE if the pouch doesn't degrade
     */
    private static int getRemainingFills(PouchType type, int rcLevel) {
        if (!type.canDegrade()) {
            return Integer.MAX_VALUE;
        }

        int breakpoint = getNextDegradationBreakpoint(type);
        int currentDeg = getDegradation(type);
        int remainingDurability = breakpoint - currentDeg;

        int remEss = PouchType.durabilityToEssenceCount(remainingDurability);
        int capacity = getActualCapacity(type, rcLevel);

        return capacity > 0 ? (remEss + capacity - 1) / capacity : 0;
    }

    // --- Public Instance Methods ---

    /**
     * Get the current amount of essence in the pouch.
     * @return amount of essence currently stored
     */
    public int getCurrentAmount() {
        return getCurrentAmountFromVar(pouchType);
    }

    /**
     * Get actual capacity (accounting for degradation and level).
     * This method fetches the live RC level from the client.
     * @return current maximum capacity of the pouch
     */
    public int getActualCapacity() {
        Client client = Static.getClient();
        int rc = Static.invoke(() -> client.getRealSkillLevel(Skill.RUNECRAFT));
        return getActualCapacity(pouchType, rc);
    }

    /**
     * Get remaining fills before the next degradation.
     * This method fetches the live RC level from the client.
     * @return The number of fills remaining before the pouch degrades further
     */
    public int getRemainingFills() {
        Client client = Static.getClient();
        int rc = Static.invoke(() -> client.getRealSkillLevel(Skill.RUNECRAFT));
        return getRemainingFills(pouchType, rc);
    }

    /**
     * Get the maximum capacity of this pouch (ignoring degradation/level).
     * @return maximum capacity value
     */
    public int getMaxCapacity() { return pouchType.getMaxCapacity(); }

    /**
     * Check if the pouch is full.
     * @return true if the pouch is at maximum capacity
     */
    public boolean isFull() {
        return getCurrentAmount() >= getActualCapacity();
    }

    /**
     * Check if the pouch is empty.
     * @return true if the pouch contains no essence
     */
    public boolean isEmpty() {
        return getCurrentAmount() == 0;
    }

    /**
     * Check if this pouch is degraded (broken state).
     * @return true if pouch is in degraded state, false otherwise
     */
    public boolean isDegraded() {
        if (item == null || !pouchType.canDegrade()) {
            return false;
        }
        return item.getId() == pouchType.getDegradedId();
    }

    // --- Factory and Inventory Lookup Methods ---

    /**
     * Create an EssencePouch from an ItemEx.
     * @param item the item to create a pouch from
     * @return EssencePouch instance, or null if the item is not a pouch
     */
    public static EssencePouch fromItem(ItemEx item) {
        if (item == null) {
            return null;
        }

        PouchType type = PouchType.fromItemId(item.getId());
        if (type == null) {
            Logger.warn("Item is not an essence pouch: " + item.getId());
            return null;
        }

        return new EssencePouch(item, type);
    }

    /**
     * Helper method to find a pouch item in inventory (checks both normal and degraded versions).
     * @param type the pouch type to find
     * @return ItemEx if found, null otherwise
     */
    private static ItemEx findPouchItemInInventory(PouchType type) {
        ItemEx item = InventoryAPI.getItem(type.getItemId());
        if (item == null && type.canDegrade()) {
            item = InventoryAPI.getItem(type.getDegradedId());
        }
        return item;
    }

    /**
     * Get the first essence pouch found in inventory.
     * @return first EssencePouch found, or null if none found
     */
    public static EssencePouch getPouch() {
        ItemEx item = InventoryAPI.getItem(i -> ArrayUtils.contains(ALL_POUCH_IDS, i.getId()));
        return fromItem(item);
    }

    /**
     * Get all essence pouches found in inventory.
     * @return list of all essence pouches found in inventory, or empty list if none found
     */
    public static List<EssencePouch> getAllPouches() {
        return InventoryQuery.fromInventoryId(InventoryID.INV)
                .withId(ALL_POUCH_IDS)
                .collect()
                .stream()
                .map(EssencePouch::fromItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // --- Static Utility Methods ---

    /**
     * Checks if any pouch currently held in the inventory is not at its maximum capacity.
     * @return true if at least one pouch can still hold more essence, false otherwise
     */
    public static boolean isAnyNotFull() {
        return getAllPouches().stream().anyMatch(pouch -> !pouch.isFull());
    }

    /**
     * Check if any essence pouch in inventory is degraded.
     * @return true if at least one pouch is in the degraded state
     */
    public static boolean isAnyDegraded() {
        return getAllPouches().stream().anyMatch(EssencePouch::isDegraded);
    }

    // --- Public Instance Action Methods ---

    /**
     * Fill the pouch with any essences in inventory.
     * Checks if there's enough essence and if the pouch has space.
     *
     * @return true if fill action was initiated, false if preconditions not met
     */
    public boolean fill() {
        return fill(ESSENCE_IDS);
    }

    /**
     * Fill the pouch with a specific essence type.
     *
     * @param essenceId the specific essence item ID to fill with (must be one of ESSENCE_IDS)
     * @return true if fill action was initiated, false if preconditions not met
     * @throws IllegalArgumentException if essenceId is not a valid essence type
     */
    public boolean fill(int essenceId) {
        if (!ArrayUtils.contains(ESSENCE_IDS, essenceId)) {
            throw new IllegalArgumentException("Invalid essence ID: " + essenceId);
        }
        return fill(new int[]{essenceId});
    }

    /**
     * Core fill logic. Fills the pouch with any of the specified essence types.
     *
     * @param essenceIds Array of essence IDs to check count for.
     * @return true if fill action was initiated, false otherwise.
     */
    public boolean fill(int[] essenceIds) {
        if (item == null) {
            return false;
        }

        int currentAmount = getCurrentAmount();
        int capacity = getActualCapacity();

        if (currentAmount >= capacity) {
            return false;
        }

        int essenceCount = InventoryAPI.count(essenceIds);
        if (essenceCount == 0) {
            return false;
        }

        int remainingFills = getRemainingFills();
        if (remainingFills <= 1 && pouchType.canDegrade()) {
            Logger.warn(pouchType.name() + " pouch will degrade after this fill!");
        }

        Widget widget = item.getWidget();
        if (widget != null) {
            WidgetAPI.interact(widget, "Fill");
            return true;
        } else {
            Logger.warn("Could not get widget for pouch ID: " + item.getId() + " to fill with essence");
            return false;
        }
    }

    /**
     * Safely fill the pouch with any essences in inventory.
     * This method will *not* fill the pouch if it is about to degrade.
     *
     * @return true if fill action was initiated, false if preconditions not met or if fill was skipped for safety.
     */
    public boolean fillSafe() {
        return fillSafe(ESSENCE_IDS);
    }

    /**
     * Safely fill the pouch with a specific essence type.
     * This method will *not* fill the pouch if it is about to degrade.
     *
     * @param essenceId the specific essence item ID to fill with
     * @return true if fill action was initiated, false if preconditions not met or if fill was skipped for safety.
     */
    public boolean fillSafe(int essenceId) {
        if (!ArrayUtils.contains(ESSENCE_IDS, essenceId)) {
            throw new IllegalArgumentException("Invalid essence ID: " + essenceId);
        }
        return fillSafe(new int[]{essenceId});
    }

    /**
     * Core "safe" fill logic. Skips filling if the pouch is 1 fill away from degrading.
     *
     * @param essenceIds Array of essence IDs to check count for.
     * @return true if fill action was initiated, false otherwise.
     */
    public boolean fillSafe(int[] essenceIds) {
        int remainingFills = getRemainingFills();
        if (remainingFills <= 1 && pouchType.canDegrade()) {
            Logger.warn("Skipping fill for " + pouchType.name() + ": pouch will degrade.");
            return false;
        }

        return fill(essenceIds);
    }

    /**
     * Empty the pouch.
     * Checks if there's enough inventory space.
     * @return true if empty action was initiated, false if preconditions not met
     */
    public boolean empty() {
        if (item == null) {
            return false;
        }

        int currentAmount = getCurrentAmount();
        if (currentAmount == 0) {
            return false;
        }

        int emptySlots = InventoryAPI.getEmptySlots();
        if (emptySlots < currentAmount) {
            Logger.warn("Not enough inventory space to empty " + pouchType.name() +
                    " pouch (need " + currentAmount + " slots, have " + emptySlots + ")");
            return false;
        }

        Widget widget = item.getWidget();
        if (widget != null) {
            WidgetAPI.interact(widget, "Empty");
            return true;
        } else {
            Logger.warn("Could not get widget for pouch id: " + item.getId());
            return false;
        }
    }

    /**
     * Check the pouch contents (displays an info message in-game).
     * @return true if check action was initiated, false if failed
     */
    public boolean check() {
        if (item == null) {
            Logger.warn("Cannot check null pouch");
            return false;
        }

        Widget widget = item.getWidget();
        if (widget != null) {
            WidgetAPI.interact(widget, "Check");
            return true;
        } else {
            Logger.warn("Could not get widget for pouch");
            return false;
        }
    }

    // --- Static Action Methods ---

    /**
     * Fill all essence pouches in the inventory (largest to smallest) with any essence type.
     * This method *will* fill pouches that are about to degrade (but will warn).
     */
    public static void fillAll() {
        if (InventoryAPI.count(ESSENCE_IDS) == 0) {
            Logger.warn("No essence in inventory to fill pouches");
            return;
        }
        fillAllInternal(ESSENCE_IDS, false);
    }

    /**
     * Fill all essence pouches in the inventory (largest to smallest) with a specific essence type.
     * This method *will* fill pouches that are about to degrade (but will warn).
     *
     * @param essenceId the specific essence item ID to fill with
     */
    public static void fillAll(int essenceId) {
        if (!ArrayUtils.contains(ESSENCE_IDS, essenceId)) {
            throw new IllegalArgumentException("Invalid essence ID: " + essenceId);
        }
        if (InventoryAPI.count(essenceId) == 0) {
            Logger.warn("No essence of type " + essenceId + " in inventory to fill pouches");
            return;
        }
        fillAllInternal(new int[]{essenceId}, false);
    }

    /**
     * Safely fill all essence pouches in inventory (largest to smallest) with any essence type.
     * This method will *skip* filling any pouch that is about to degrade.
     */
    public static void fillAllSafe() {
        if (InventoryAPI.count(ESSENCE_IDS) == 0) {
            Logger.warn("No essence in inventory to fill pouches");
            return;
        }
        fillAllInternal(ESSENCE_IDS, true);
    }

    /**
     * Safely fill all essence pouches in inventory (largest to smallest) with a specific essence type.
     * This method will *skip* filling any pouch that is about to degrade.
     *
     * @param essenceId the specific essence item ID to fill with
     */
    public static void fillAllSafe(int essenceId) {
        if (!ArrayUtils.contains(ESSENCE_IDS, essenceId)) {
            throw new IllegalArgumentException("Invalid essence ID: " + essenceId);
        }
        if (InventoryAPI.count(essenceId) == 0) {
            Logger.warn("No essence of type " + essenceId + " in inventory to fill pouches");
            return;
        }
        fillAllInternal(new int[]{essenceId}, true);
    }

    /**
     * Internal helper for all `fillAll` variants.
     *
     * @param essenceIds Array of essence IDs to use for filling.
     * @param safeFill   If true, skip filling pouches that are <= 1 fill from degrading.
     */
    private static void fillAllInternal(int[] essenceIds, boolean safeFill) {
        for (PouchType type : PouchType.getOrderedTypes()) {
            ItemEx item = findPouchItemInInventory(type);

            if (item != null) {
                EssencePouch pouch = new EssencePouch(item, type);

                if (!pouch.isFull()) {
                    if (InventoryAPI.count(essenceIds) == 0) {
                        break;
                    }

                    if (safeFill) {
                        pouch.fillSafe(essenceIds);
                    } else {
                        pouch.fill(essenceIds);
                    }
                }
            }
        }
    }

    /**
     * Empty all essence pouches in inventory (largest to smallest).
     * Only empties pouches if there's enough inventory space.
     * Stops if inventory becomes full.
     */
    public static void emptyAll() {
        for (PouchType type : PouchType.getOrderedTypes()) {
            ItemEx item = findPouchItemInInventory(type);

            if (item != null) {
                EssencePouch pouch = new EssencePouch(item, type);

                if (!pouch.isEmpty()) {
                    int currentAmount = pouch.getCurrentAmount();
                    int emptySlots = InventoryAPI.getEmptySlots();

                    if (emptySlots < currentAmount) {
                        Logger.warn("Not enough inventory space to empty " + type.name() +
                                " pouch (need " + currentAmount + " slots, have " + emptySlots + ")");
                        break;
                    }

                    pouch.empty();
                }
            }
        }
    }

    // Getters
    public ItemEx getItem() { return item; }
    public PouchType getPouchType() { return pouchType; }
}
