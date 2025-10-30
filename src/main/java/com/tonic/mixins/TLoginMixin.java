package com.tonic.mixins;

import com.tonic.api.TAccountType;
import com.tonic.api.TClient;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;

@Mixin("Client")
public abstract class TLoginMixin implements TClient
{
    @Shadow("setLoginIndex")
    @Override
    public abstract void setLoginIndex(int index);

    @Shadow("accountTypeCheck")
    public static TAccountType accountType;

    @Shadow("legacyType")
    public static TAccountType legacyType;

    @Shadow("jagexType")
    public static TAccountType jagexType;

    @Inject
    @Override
    public void setAccountTypeLegacy() {
        accountType = legacyType;
    }

    @Inject
    @Override
    public void setAccountTypeJagex() {
        accountType = jagexType;
    }
}
