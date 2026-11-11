package com.tonic.data.settings;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * A setting as the live settings menu lists it: where its row is and what the cache says about it.
 */
@Getter
public final class SettingEntry
{
    /**
     * -- GETTER --
     *
     * @return the struct id of the setting
     */
    private final int structId;
    /**
     * -- GETTER --
     *
     * @return the kind of setting
     */
    private final SettingKind kind;
    /**
     * -- GETTER --
     *
     * @return the id the game's setting scripts know the setting by
     */
    private final int settingId;
    /**
     * -- GETTER --
     *
     * @return the index of the menu tab the setting is listed under
     */
    private final int category;
    /**
     * -- GETTER --
     *
     * @return the index of the setting's row in its tab
     */
    private final int row;
    /**
     * -- GETTER --
     *
     * @return the setting's name
     */
    private final String name;
    /**
     * -- GETTER --
     *
     * @return the setting's description
     */
    private final String description;
    /**
     * -- GETTER --
     *
     * @return the message the game shows when the setting cannot be changed
     */
    private final String blockedMessage;
    /**
     * -- GETTER --
     *
     * @return whether a stored 0 means on, for toggles
     */
    private final boolean inverted;
    /**
     * -- GETTER --
     *
     * @return the option labels for dropdowns and key bindings, each option's value being its index
     */
    private final List<String> options;
    /**
     * -- GETTER --
     *
     * @return the number of steps, for sliders
     */
    private final int steps;
    /**
     * -- GETTER --
     *
     * @return the index of the slider's first notch in the tab, or -1 when the slider has no notches
     */
    private final int notchStart;
    /**
     * -- GETTER --
     *
     * @return whether the slider moves smoothly rather than in steps
     */
    private final boolean smooth;
    /**
     * -- GETTER --
     *
     * @return the colour used while none is set, for colour settings
     */
    private final int defaultColour;

    SettingEntry(int structId, SettingKind kind, int settingId, int category, int row, String name, String description, String blockedMessage, boolean inverted, List<String> options, int steps, int notchStart, boolean smooth, int defaultColour)
    {
        this.structId = structId;
        this.kind = kind;
        this.settingId = settingId;
        this.category = category;
        this.row = row;
        this.name = name;
        this.description = description;
        this.blockedMessage = blockedMessage;
        this.inverted = inverted;
        this.options = Collections.unmodifiableList(options);
        this.steps = steps;
        this.notchStart = notchStart;
        this.smooth = smooth;
        this.defaultColour = defaultColour;
    }

}
