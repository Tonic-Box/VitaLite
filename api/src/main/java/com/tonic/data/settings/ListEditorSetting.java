package com.tonic.data.settings;

/**
 * A list edited in its own interface.
 */
public final class ListEditorSetting extends AbstractSetting
{
    /**
     * @param structId the struct id of the setting
     */
    public ListEditorSetting(int structId)
    {
        super(structId);
    }

    @Override
    public SettingKind getKind()
    {
        return SettingKind.LIST_EDITOR;
    }
}
