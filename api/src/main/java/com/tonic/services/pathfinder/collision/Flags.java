package com.tonic.services.pathfinder.collision;

public class Flags {
    public static final byte NORTHWEST = 0x1;
    public static final byte NORTH = 0x2;
    public static final byte NORTHEAST = 0x4;
    public static final byte WEST = 0x8;
    public static final byte EAST = 0x10;
    public static final byte SOUTHWEST = 0x20;
    public static final byte SOUTH = 0x40;
    public static final byte SOUTHEAST = (byte) 0x80;
    public static final byte ALL = (byte) 0xFF;
    public static final byte NONE = 0x0;
}