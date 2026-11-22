package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Delegate;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.StreamerBridges;
import com.tonic.runelite.ClientUIUpdater;

import java.awt.image.BufferedImage;

@Mixin("net/runelite/client/ui/ClientUI")
public class ClientUIMixin {
    private static final String STREAMER_CURSOR = "com/tonic/services/streamer/StreamerCursor";

    @Insert(
            method = "lambda$init$6",
            at = @At(value = AtTarget.INVOKE, owner = "net/runelite/client/ui/ClientUI", target = "updateFrameConfig"),
            ordinal = -1
    )
    public static void initVita() {
        ClientUIUpdater.inject();
    }

    /**
     * Has streamer mode's mirror draw the image RuneLite sets as its cursor.
     *
     * @param cursor the cursor image
     */
    @Insert(method = "setCursor", desc = "(Ljava/awt/image/BufferedImage;Ljava/lang/String;)V", at = @At(value = AtTarget.HEAD))
    @Delegate(STREAMER_CURSOR + ".set")
    public static native void streamerCursorSet(BufferedImage cursor);

    /**
     * Has streamer mode's mirror go back to its arrow when RuneLite goes back to the system cursor.
     */
    @Insert(method = "resetCursor", desc = "()V", at = @At(value = AtTarget.HEAD))
    @Delegate(STREAMER_CURSOR + ".reset")
    public static native void streamerCursorReset();

    /**
     * Hands the finished frame to streamer mode's mirror when it is one handed over, and decides whether the next one is, once RuneLite has painted its own overlays, the last thing drawn into each frame.
     */
    @Insert(method = "paintOverlays", desc = "(Ljava/awt/Graphics2D;)V", at = @At(value = AtTarget.RETURN), all = true, gate = StreamerBridges.STREAMER_ACTIVE)
    @Delegate("com/tonic/services/streamer/MirrorFrames.frameDrawn")
    public static native void streamerFrameDrawn();
}
