package com.tonic.data.settings;

/**
 * A key binding.
 */
public final class KeyBindingSetting extends AbstractSetting
{
    /**
     * @param structId the struct id of the setting
     */
    public KeyBindingSetting(int structId)
    {
        super(structId);
    }

    @Override
    public SettingKind getKind()
    {
        return SettingKind.KEY_BINDING;
    }
}
