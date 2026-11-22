package com.tonic.mixins;

import com.tonic.Static;
import com.tonic.api.*;
import com.tonic.injector.annotations.*;
import com.tonic.services.SceneScale;
import com.tonic.services.streamer.StreamerMenu;
import net.runelite.api.FontTypeFace;

@Mixin("Client")
public abstract class TClientMixin implements TClient
{
    @Shadow("packetWriter")
    private static TPacketWriter packetWriter;

    @Shadow("MouseHandler_instance")
    private static TMouseHandler mouseHandler;

    @Shadow("heading")
    private static int shipHeading;

    @Shadow("serverRevision")
    private static int serverRevision;

    @Shadow("loginDisplayName")
    private static String loginDisplayName;

    @Shadow("rasterDepth")
    private static float[] rasterDepth;

    @Inject
    @Override
    public float[] getRasterDepth()
    {
        SceneScale.stretchDepth(rasterDepth);
        return rasterDepth;
    }

    @Override
    @Shadow("depthOf")
    public abstract float depthOf(float viewZ);

    @Override
    @Shadow("getComponentGroup")
    public abstract Object[] getComponentGroup(int group);

    @Inject
    @Override
    public String getLoginDisplayName()
    {
        return loginDisplayName;
    }

    @Inject
    //@Override
    public int getServerRevision()
    {
        return serverRevision;
    }

    @Inject
    @Override
    public int getShipHeading()
    {
        return shipHeading;
    }

    @Inject
    @Override
    public void setShipHeading(int heading)
    {
        shipHeading = heading;
    }

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
        return !Static.getVitaConfig().shouldNeverLog();
    }

    @Shadow("drawText")
    public static void drawFontText(FontTypeFace font, String text, int x, int y, int fifth, int sixth, int seventh)
    {
    }

    /**
     * Draws the mouseover text, and a second time for the streamer mirror when its stock-only menu gives other text. The draws of the mouseover text in the methods that draw the menu come here while streamer mode is on.
     *
     * @param font the font
     * @param text the text
     * @param x where it is drawn, across the canvas
     * @param y its baseline, down the canvas
     * @param fifth passed on
     * @param sixth passed on
     * @param seventh passed on
     */
    @Inject
    public static void streamerDrawMouseOverText(FontTypeFace font, String text, int x, int y, int fifth, int sixth, int seventh)
    {
        if (!StreamerMenu.textBegin(text, x, y))
        {
            drawFontText(font, text, x, y, fifth, sixth, seventh);
            return;
        }
        try
        {
            drawFontText(font, text, x, y, fifth, sixth, seventh);
            try
            {
                if (StreamerMenu.textSwap())
                {
                    String mirrorText = StreamerMenu.textMirror(text);
                    if (mirrorText != null)
                    {
                        drawFontText(font, mirrorText, x, y, fifth, sixth, seventh);
                    }
                }
            }
            catch (RuntimeException e)
            {
                StreamerMenu.failed(e);
            }
        }
        finally
        {
            StreamerMenu.textDone();
        }
    }
}
