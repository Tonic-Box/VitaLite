package com.tonic.mixins;

import com.tonic.api.TClient;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.MethodHook;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import com.tonic.model.ui.VitaLiteOptionsPanel;

@Mixin("Client")
public abstract class TDoActionMixin implements TClient {
    @MethodHook("doAction")
    public static void invokeMenuActionHook(int param0, int param1, int opcode, int id, int itemId, int worldViewId, String option, String target, int canvasX, int canvasY) {
        VitaLiteOptionsPanel.getInstance().onMenuAction(option, target, id, opcode, param0, param1, itemId);
    }

    @Shadow("doAction")
    public abstract void RSDoAction(int param0, int param1, int opcode, int id, int itemId, int worldViewId, String option, String target, int canvasX, int canvasY);

    @Override
    @Inject
    public void invokeMenuAction(String option, String target, int identifier, int opcode, int param0, int param1, int itemId, int x, int y) {
        if (!isClientThread())
            return;

        RSDoAction(param0, param1, opcode, identifier, itemId, -1, option, target, x, y);
    }

    @Override
    @Inject
    public void invokeMenuAction(String option, String target, int identifier, int opcode, int param0, int param1, int itemId, int worldView, int x, int y) {
        if (!isClientThread())
            return;

        RSDoAction(param0, param1, opcode, identifier, itemId, worldView, option, target, x, y);
    }
}
