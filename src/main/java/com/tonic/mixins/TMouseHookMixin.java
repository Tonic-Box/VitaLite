package com.tonic.mixins;

import com.tonic.Static;
import com.tonic.injector.annotations.Disable;
import com.tonic.injector.annotations.Mixin;

@Mixin("Client")
public class TMouseHookMixin
{
    @Disable("mouseHookLoader")
    public static boolean mouseHookLoader()
    {
        return !Static.getCliArgs().isDisableMouseHook();
    }
}
