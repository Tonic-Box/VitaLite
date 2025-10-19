package com.tonic.plugins.breakhandler.settings;

public enum Property
{
    MIN_BETWEEN(Settings.MIN_BETWEEN),
    MAX_BETWEEN(Settings.MAX_BETWEEN),
    MIN_DURATION(Settings.MIN_DURATION),
    MAX_DURATION(Settings.MAX_DURATION),

    ACCOUNT_MODE(Settings.ACCOUNT_MODE),
    ACCOUNT_PROFILE(Settings.ACCOUNT_PROFILE),
    ACCOUNT_USERNAME(Settings.ACCOUNT_USERNAME),
    ACCOUNT_PASSWORD(Settings.ACCOUNT_PASSWORD),

    HOP_ENABLED(Settings.HOP_ENABLED),
    F2P_ONLY(Settings.F2P_ONLY),

    DISABLE_HIGH_RISK(Settings.DISABLE_HIGH_RISK),
    DISABLE_LMS(Settings.DISABLE_LMS),
    DISABLE_SKILL_TOTAL(Settings.DISABLE_SKILL_TOTAL),
    DISABLE_BOUNTY(Settings.DISABLE_BOUNTY),

    DISABLE_REGION_US(Settings.DISABLE_REGION_US),
    DISABLE_REGION_UK(Settings.DISABLE_REGION_UK),
    DISABLE_REGION_DE(Settings.DISABLE_REGION_DE),
    DISABLE_REGION_AU(Settings.DISABLE_REGION_AU);

    private final String key;

    Property(String key) {
        this.key = key;
    }

    public String key() {
        return Settings.GROUP + key;
    }
}
