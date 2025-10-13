package com.tonic.mixins;

import com.tonic.Static;
import com.tonic.api.TGameEngine;
import com.tonic.injector.annotations.Disable;
import com.tonic.injector.annotations.Mixin;

@Mixin("GameEngine")
public class TGameEngineMixin implements TGameEngine
{
    @Disable("graphicsTick")
    public static boolean onGraphicsTick()
    {
        return !Static.isHeadless();
    }

//    @Disable("post")
//    public static boolean post()
//    {
//        return !Static.isHeadless();
//    }
}
