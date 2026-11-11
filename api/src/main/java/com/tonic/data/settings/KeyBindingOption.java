package com.tonic.data.settings;

import lombok.Getter;

/**
 * A key a key binding can be set to, by its index in the key list.
 */
@Getter
public final class KeyBindingOption
{
    private final int value;

    /**
     * @param value the key's index in the key list
     */
    public KeyBindingOption(int value)
    {
        if (value < 0)
        {
            throw new IllegalArgumentException("value cannot be negative");
        }
        this.value = value;
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof KeyBindingOption && ((KeyBindingOption) other).value == value;
    }

    @Override
    public int hashCode()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return "KeyBindingOption(" + value + ")";
    }
}
