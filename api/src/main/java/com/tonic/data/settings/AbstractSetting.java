package com.tonic.data.settings;

/**
 * A setting in the settings menu, keyed by the id of the struct that describes it.
 */
public abstract class AbstractSetting
{
    private final int structId;

    /**
     * @param structId the struct id of the setting
     */
    protected AbstractSetting(int structId)
    {
        if (structId < 0)
        {
            throw new IllegalArgumentException("structId cannot be negative");
        }
        this.structId = structId;
    }

    /**
     * @return the struct id of the setting
     */
    public final int getStructId()
    {
        return structId;
    }

    /**
     * @return the kind of setting
     */
    public abstract SettingKind getKind();

    @Override
    public boolean equals(Object other)
    {
        return other != null && other.getClass() == getClass() && ((AbstractSetting) other).structId == structId;
    }

    @Override
    public int hashCode()
    {
        return structId;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "(" + structId + ")";
    }
}
