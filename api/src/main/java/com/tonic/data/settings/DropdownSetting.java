package com.tonic.data.settings;

/**
 * A setting picked from a dropdown.
 */
public final class DropdownSetting extends AbstractSetting
{
    /**
     * @param structId the struct id of the setting
     */
    public DropdownSetting(int structId)
    {
        super(structId);
    }

    @Override
    public SettingKind getKind()
    {
        return SettingKind.DROPDOWN;
    }
}
