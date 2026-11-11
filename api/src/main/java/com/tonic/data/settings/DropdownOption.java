package com.tonic.data.settings;

import lombok.Getter;

/**
 * An option of a dropdown setting, by its index in the dropdown.
 */
@Getter
public final class DropdownOption
{
    private final DropdownSetting setting;
    private final int value;

    /**
     * @param setting the dropdown the option belongs to
     * @param value the option's index in the dropdown
     */
    public DropdownOption(DropdownSetting setting, int value)
    {
        if (setting == null)
        {
            throw new IllegalArgumentException("setting cannot be null");
        }
        if (value < 0)
        {
            throw new IllegalArgumentException("value cannot be negative");
        }
        this.setting = setting;
        this.value = value;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof DropdownOption))
        {
            return false;
        }
        DropdownOption option = (DropdownOption) other;
        return option.setting.equals(setting) && option.value == value;
    }

    @Override
    public int hashCode()
    {
        return setting.hashCode() * 31 + value;
    }

    @Override
    public String toString()
    {
        return "DropdownOption(" + setting.getStructId() + ", " + value + ")";
    }
}
