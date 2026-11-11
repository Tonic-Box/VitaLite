package com.tonic.data.settings;

/**
 * How a threaded settings change ended.
 */
public enum SettingResult
{
    /**
     * The setting holds the value asked for.
     */
    DONE,
    /**
     * The game does not allow the setting to be changed right now, usually because another setting must be changed first.
     */
    BLOCKED,
    /**
     * The setting is not listed in the menu right now, such as a setting only the official clients have.
     */
    NOT_AVAILABLE,
    /**
     * The settings menu could not be opened.
     */
    NOT_OPEN,
    /**
     * The change was sent but the setting never showed the value.
     */
    NOT_CONFIRMED,
    /**
     * The player is not logged in.
     */
    NOT_IN_GAME
}
