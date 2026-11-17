package com.tonic.mixins;

import com.tonic.api.TClient;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;

@Mixin("Client")
public abstract class TLoginMixin implements TClient
{
    @Shadow("loginType")
    private static Object loginType;

    @Shadow("LoginType_jagexAccount")
    private static Object jagexAccountLoginType;

    @Shadow("setLoginIndex")
    @Override
    public abstract void setLoginIndex(int index);

    @Inject
    @Override
    public void useJagexAccountLoginType()
    {
        loginType = jagexAccountLoginType;
    }
}
