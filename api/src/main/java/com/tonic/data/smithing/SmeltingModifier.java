package com.tonic.data.smithing;

/**
 * Base interface for smelting modifiers.
 * Modifiers alter the smelting process behavior (experience, success rate, etc.)
 */
public interface SmeltingModifier {}

/**
 * Modifies experience gained when wearing goldsmith gauntlets or smithing cape.
 */
final class GoldsmithGauntletsModifier implements SmeltingModifier {
    private final double modifiedExperience;

    /**
     * Creates an experience modifier for goldsmith gauntlets.
     *
     * @param modifiedExperience the experience gained with goldsmith gauntlets or smithing cape
     * @throws IllegalArgumentException if experience is not positive
     */
    public GoldsmithGauntletsModifier(double modifiedExperience) {
        if (modifiedExperience <= 0) {
            throw new IllegalArgumentException("Modified experience must be positive");
        }
        this.modifiedExperience = modifiedExperience;
    }

    /**
     * Returns the modified experience value.
     *
     * @return the modified experience
     */
    public double getModifiedExperience() {
        return modifiedExperience;
    }
}

/**
 * Modifies the success rate of smelting.
 * Currently only used for iron bars which have a 50% base success rate.
 */
final class SuccessRateModifier implements SmeltingModifier {
    private final double successRate;

    /**
     * Creates a success rate modifier.
     *
     * @param successRate the success rate (must be between 0.0 and 1.0 inclusive)
     * @throws IllegalArgumentException if success rate is not between 0.0 and 1.0
     */
    public SuccessRateModifier(double successRate) {
        if (successRate < 0.0 || successRate > 1.0) {
            throw new IllegalArgumentException("Success rate must be between 0.0 and 1.0");
        }
        this.successRate = successRate;
    }

    /**
     * Returns the success rate.
     *
     * @return the success rate (0.0 to 1.0)
     */
    public double getSuccessRate() {
        return successRate;
    }
}
