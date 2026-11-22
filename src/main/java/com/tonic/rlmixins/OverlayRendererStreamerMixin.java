package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Delegate;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Redirect;
import com.tonic.injector.util.StreamerBridges;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;

/**
 * Hands each overlay's draw to MirrorOverlays while streamer mode is on, which decides how the mirror shows it, and the outlines drawn while overlays are moved too.
 */
@Mixin("net/runelite/client/ui/overlay/OverlayRenderer")
public class OverlayRendererStreamerMixin
{
    private static final String SAFE_RENDER = "(Lnet/runelite/client/ui/overlay/Overlay;Ljava/awt/Graphics2D;Ljava/awt/Point;)V";
    private static final String MIRROR_OVERLAYS = "com/tonic/services/streamer/MirrorOverlays";

    /**
     * Starts an overlay's draw through MirrorOverlays, which may give it other graphics.
     *
     * @param overlay the overlay
     * @param graphics its graphics
     * @param location where it is drawn
     * @return the graphics to draw it with
     */
    @Insert(method = "safeRender", desc = SAFE_RENDER, at = @At(value = AtTarget.HEAD), gate = StreamerBridges.STREAMER_MIRRORING, storeIntoParameter = 1)
    @Delegate(MIRROR_OVERLAYS + ".begin")
    public native Graphics2D streamerOverlayBegin(Object overlay, Graphics2D graphics, Point location);

    /**
     * Ends an overlay's draw at each return.
     *
     * @param overlay the overlay
     */
    @Insert(method = "safeRender", desc = SAFE_RENDER, at = @At(value = AtTarget.RETURN), all = true, gate = StreamerBridges.STREAMER_MIRRORING)
    @Delegate(MIRROR_OVERLAYS + ".done")
    public static native void streamerOverlayDone(Object overlay);

    /**
     * Draws the outline shown while overlays are moved through MirrorOverlays, so the mirror outlines its own copy of each overlay.
     *
     * @param graphics the layer's graphics
     * @param outline the outline
     */
    @Redirect(method = "renderOverlays", desc = "(Ljava/awt/Graphics2D;Ljava/util/Collection;Lnet/runelite/client/ui/overlay/OverlayLayer;)V", at = @At(value = AtTarget.INVOKE, owner = "java/awt/Graphics2D", target = "draw(Ljava/awt/Shape;)V"), expectedMatches = 1, gate = StreamerBridges.STREAMER_MIRRORING)
    @Delegate(MIRROR_OVERLAYS + ".outline")
    public static native void streamerOutline(Graphics2D graphics, Shape outline);
}
