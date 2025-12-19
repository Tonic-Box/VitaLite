package com.tonic.data.smithing;

import com.tonic.api.widgets.InventoryAPI;
import com.tonic.services.pathfinder.requirements.Requirement;

/**
 * Represents an ore requirement for smelting a bar.  
 */
public final class OreRequirement implements Requirement {
    private final int oreId;
    private final int count;

    public OreRequirement(int oreId, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Ore count must be positive");
        }
        this.oreId = oreId;
        this.count = count;
    }

    public int getOreId() {
        return oreId;
    }

    public int getCount() {
        return count;
    }

    @Override
    public Boolean get() {
        return InventoryAPI.getCount(oreId) >= count;
    }

    @Override
    public String toString() {
        return count + "x ore[" + oreId + "]";
    }
}
