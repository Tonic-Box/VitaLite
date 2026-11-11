package com.tonic.data.settings;

/**
 * A setting picked on a slider.
 */
public final class SliderSetting extends AbstractSetting
{
    /**
     * @param structId the struct id of the setting
     */
    public SliderSetting(int structId)
    {
        super(structId);
    }

    @Override
    public SettingKind getKind()
    {
        return SettingKind.SLIDER;
    }
}
