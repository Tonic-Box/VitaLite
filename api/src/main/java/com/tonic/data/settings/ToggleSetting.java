package com.tonic.data.settings;

/**
 * A setting switched on or off.
 */
public final class ToggleSetting extends AbstractSetting
{
    /**
     * @param structId the struct id of the setting
     */
    public ToggleSetting(int structId)
    {
        super(structId);
    }

    @Override
    public SettingKind getKind()
    {
        return SettingKind.TOGGLE;
    }
}
