package com.tonic.data.settings;

import lombok.Getter;

/**
 * The kinds of settings menu row that hold a setting, with the client script that reads each one's value.
 */
@Getter
public enum SettingKind
{
    TOGGLE(0, 6716),
    SLIDER(1, 3961),
    DROPDOWN(2, 3962),
    KEY_BINDING(3, 3963),
    NUMBER(4, 4398),
    ACTION(6, -1),
    COLOUR(9, 4181),
    LIST_EDITOR(10, 7107);

    private final int code;
    private final int getterScript;

    SettingKind(int code, int getterScript)
    {
        this.code = code;
        this.getterScript = getterScript;
    }

    /**
     * Finds the kind for a cache code.
     * @param code the code
     * @return the kind, or null for layout rows and unknown codes
     */
    public static SettingKind of(int code)
    {
        for (SettingKind kind : values())
        {
            if (kind.code == code)
            {
                return kind;
            }
        }
        return null;
    }
}
