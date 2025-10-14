package com.tonic.rlmixins;

import com.tonic.injector.annotations.MethodOverride;
import com.tonic.injector.annotations.Mixin;

@Mixin("net/runelite/client/discord/DiscordService")
public class DiscordServiceMixin
{
    @MethodOverride("<init>")
    public DiscordServiceMixin()
    {
        super();
        // Disable Discord integration
    }

    @MethodOverride("init")
    public void init()
    {
        // Disable Discord integration
    }

    @MethodOverride("updatePresence")
    public void updatePresence()
    {
        // Disable Discord integration
    }
}
