package com.tonic.mixins;

import com.tonic.api.TClient;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Delegate;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;

/**
 * Lets VitaLite's world select stand in for the client's own login world select: the client's is dropped the frame it is opened and VitaLite's is opened in its place.
 */
@Mixin("Client")
public abstract class TWorldSelectMixin implements TClient
{
    private static final String GATE = "com/tonic/services/worldselect/WorldSelectBridge.enabled";

    @Shadow("worldSelect")
    private static Object worldSelect;

    @Shadow("leaveWorldSelect")
    public static void leaveWorldSelectPage(boolean reset)
    {
    }

    @Inject
    @Override
    public void leaveWorldSelect()
    {
        leaveWorldSelectPage(false);
    }

    @Inject
    @Override
    public boolean isWorldSelectOpen()
    {
        return worldSelect != null;
    }

    @Inject
    @Override
    public void closeWorldSelect()
    {
        worldSelect = null;
    }

    /**
     * Swaps the client's world select for VitaLite's, before the login screen is drawn, while VitaLite's is in use.
     */
    @Insert(method = "drawLoginScreen", at = @At(value = AtTarget.HEAD), gate = GATE)
    @Delegate("com/tonic/services/worldselect/WorldSelect.intercept")
    public static native void worldSelectIntercept();
}
