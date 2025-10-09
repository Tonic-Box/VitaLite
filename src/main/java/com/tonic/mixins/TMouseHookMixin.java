package com.tonic.mixins;

import com.tonic.injector.annotations.Disable;
import com.tonic.injector.annotations.Mixin;
import com.tonic.vitalite.Main;

@Mixin("Client")
public class TMouseHookMixin
{
    @Disable("mouseHookLoader")
    public static boolean mouseHookLoader()
    {
        return !Main.optionsParser.isDisableMouseHook();
    }
}
