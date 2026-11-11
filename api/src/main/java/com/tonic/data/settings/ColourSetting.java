package com.tonic.data.settings;

/**
 * A setting picked as a colour.
 */
public final class ColourSetting extends AbstractSetting
{
    /**
     * @param structId the struct id of the setting
     */
    public ColourSetting(int structId)
    {
        super(structId);
    }

    @Override
    public SettingKind getKind()
    {
        return SettingKind.COLOUR;
    }
}
