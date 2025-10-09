package com.tonic.mixins;

import com.tonic.injector.annotations.MethodOverride;
import com.tonic.injector.annotations.Mixin;

@Mixin("Client")
public class TMouseHookMixin
{
    @MethodOverride("mouseHookLoader")
    public static void mouseHookLoader()
    {

    }
}
