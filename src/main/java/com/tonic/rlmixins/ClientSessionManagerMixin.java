package com.tonic.rlmixins;

import com.tonic.injector.annotations.MethodOverride;
import com.tonic.injector.annotations.Mixin;

@Mixin("net/runelite/client/ClientSessionManager")
public class ClientSessionManagerMixin
{
    @MethodOverride("start")
    public void start()
    {
    }

    @MethodOverride("onClientShutdown")
    public void onClientShutdown()
    {
    }

    @MethodOverride("ping")
    public void ping()
    {
    }

    @MethodOverride("isWorldHostValid")
    public boolean isWorldHostValid()
    {
        return true;
    }
}
