package com.tonic.data.slayerrewards;

public abstract class AbstractTabImpl
{
    public abstract RewardsTab getTab();

    public final boolean isOpen()
    {
        return getTab().isOpen();
    }

    public final void open()
    {
        getTab().open();
    }
}
