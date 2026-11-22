package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Delegate;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.StreamerBridges;

import java.awt.Graphics2D;

/**
 * Has MirrorFlash note who started each screen flash, which runs while streamer mode is off too as a flash can outlast the switch, and draws the flash through it while streamer mode is on.
 */
@Mixin("net/runelite/client/Notifier")
public class NotifierMixin
{
    private static final String PROCESS_FLASH = "(Ljava/awt/Graphics2D;)V";
    private static final String MIRROR_FLASH = "com/tonic/services/streamer/MirrorFlash";

    /**
     * Notes who started a flash, right after the notification sets it.
     */
    @Insert(method = "notify", desc = "(Lnet/runelite/client/config/Notification;Ljava/lang/String;)V", at = @At(value = AtTarget.PUTFIELD, target = "flashStart"), all = true, expectedMatches = 1)
    @Delegate(MIRROR_FLASH + ".flashSet")
    public static native void streamerFlashSet();

    /**
     * Starts drawing the flash through MirrorFlash, which may give it other graphics.
     *
     * @param graphics the flash's graphics
     * @return the graphics to draw the flash with
     */
    @Insert(method = "processFlash", desc = PROCESS_FLASH, at = @At(value = AtTarget.HEAD), gate = StreamerBridges.STREAMER_MIRRORING, storeIntoParameter = 0)
    @Delegate(MIRROR_FLASH + ".begin")
    public static native Graphics2D streamerFlashBegin(Graphics2D graphics);

    /**
     * Ends drawing the flash at each return.
     */
    @Insert(method = "processFlash", desc = PROCESS_FLASH, at = @At(value = AtTarget.RETURN), all = true, gate = StreamerBridges.STREAMER_MIRRORING)
    @Delegate(MIRROR_FLASH + ".done")
    public static native void streamerFlashDone();
}
