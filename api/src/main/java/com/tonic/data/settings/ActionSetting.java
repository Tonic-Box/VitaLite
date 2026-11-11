package com.tonic.data.settings;

/**
 * A button in the settings menu that does something when pressed.
 */
public final class ActionSetting extends AbstractSetting
{
    /**
     * @param structId the struct id of the setting
     */
    public ActionSetting(int structId)
    {
        super(structId);
    }

    @Override
    public SettingKind getKind()
    {
        return SettingKind.ACTION;
    }
}
