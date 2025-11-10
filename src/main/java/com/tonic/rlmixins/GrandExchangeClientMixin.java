package com.tonic.rlmixins;

import com.tonic.injector.annotations.MethodOverride;
import com.tonic.injector.annotations.Mixin;

@Mixin("net/runelite/client/plugins/grandexchange/GrandExchangeClient")
public class GrandExchangeClientMixin {
    @MethodOverride("submit")
    public void submit()
    {

    }
}
