package com.tonic.data.smithing;

import com.tonic.services.pathfinder.requirements.Requirements;

/**
 * Interface for smithable items across different metals.
 */
public interface SmithableItem {
    String getDisplayName();

    int getBarCount();

    int getOutputId();

    int getOutputQuantity();

    int getInterfaceID();

    Requirements getRequirements();

    boolean canAccess();
}
