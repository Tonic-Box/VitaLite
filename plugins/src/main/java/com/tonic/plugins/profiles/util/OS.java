package com.tonic.plugins.profiles.util;

public enum OS {
    WINDOWS(1),
    MAC(2),
    LINUX(3),
    UNKNOWN(4);

    private final int value;

    OS(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static OS detect() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return WINDOWS;
        } else if (os.contains("mac") || os.contains("darwin")) {
            return MAC;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return LINUX;
        } else {
            return UNKNOWN;
        }
    }
}