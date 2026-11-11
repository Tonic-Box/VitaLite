package com.tonic.data.settings;

/**
 * A setting entered as a number.
 */
public final class NumberSetting extends AbstractSetting
{
    /**
     * @param structId the struct id of the setting
     */
    public NumberSetting(int structId)
    {
        super(structId);
    }

    @Override
    public SettingKind getKind()
    {
        return SettingKind.NUMBER;
    }
}
