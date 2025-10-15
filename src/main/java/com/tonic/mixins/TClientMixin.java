package com.tonic.mixins;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.*;
import com.tonic.injector.annotations.*;
import com.tonic.injector.util.ExceptionUtil;
import com.tonic.model.ui.VitaLiteOptionsPanel;

@Mixin("Client")
public abstract class TClientMixin implements TClient
{
    @Shadow("packetWriter")
    private static TPacketWriter packetWriter;

    @Shadow("MouseHandler_instance")
    private static TMouseHandler mouseHandler;

    @Inject
    @Override
    public TPacketWriter getPacketWriter()
    {
        return packetWriter;
    }

    @Inject
    @Override
    public TMouseHandler getMouseHandler()
    {
        return mouseHandler;
    }

    @Override
    @Construct("ClientPacket")
    public abstract TClientPacket newClientPacket(int id, int length);

    @Shadow("getPacketBufferNode")
    public abstract TPacketBufferNode getPacketBufferNode(TClientPacket clientPacket, TIsaacCipher isaacCipher);

    @Shadow("mouseLastPressedTimeMillis")
    private static long clientMouseLastPressedMillis;

    @Inject
    public long getClientMouseLastPressedMillis() {
        return clientMouseLastPressedMillis;
    }

    @Inject
    public void setClientMouseLastPressedMillis(long millis) {
        clientMouseLastPressedMillis = millis;
    }

    @FieldHook("MouseHandler_idleCycles")
    public static boolean onIdleCycleSet(int value) {
        return false;
    }

    @FieldHook("client")
    public static boolean onClientSet(TClient client)
    {
        Static.set(client, "RL_CLIENT");
        return true;
    }
}
