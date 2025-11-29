package com.tonic.services.pathfinder.tiletype;

import lombok.Getter;

@Getter
public enum TileType {
    WATER((byte) 1),
    CRANDOR_SMEGMA_WATER((byte) 2),
    TEMPOR_STORM_WATER((byte) 3),
    DISEASE_WATER((byte) 4),
    KELP_WATER((byte) 5),
    SUNBAKED_WATER((byte) 6),
    JAGGED_REEFS_WATER((byte) 7),
    SHARP_CRYSTAL_WATER((byte) 8),
    ICE_WATER((byte) 9),
    NE_PURPLE_GRAY_WATER((byte) 10),
    NW_GRAY_WATER((byte) 11),
    SE_PURPLE_WATER((byte) 12),
    UNKNOWN((byte) -1);

    private final byte value;

    TileType(byte value) {
        this.value = value;
    }

    public static TileType fromValue(byte value) {
        for (TileType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return UNKNOWN;
    }

    public static final byte F_WATER = 1;
    public static final byte F_CRANDOR_SMEGMA_WATER = 2;
    public static final byte F_TEMPOR_STORM_WATER = 3;
    public static final byte F_DISEASE_WATER = 4;
    public static final byte F_KELP_WATER = 5;
    public static final byte F_SUNBAKED_WATER = 6;
    public static final byte F_JAGGED_REEFS_WATER = 7;
    public static final byte F_SHARP_CRYSTAL_WATER = 8;
    public static final byte F_ICE_WATER = 9;
    public static final byte F_NE_PURPLE_GRAY_WATER = 10;
    public static final byte F_NW_GRAY_WATER = 11;
    public static final byte F_SE_PURPLE_WATER = 12;
}