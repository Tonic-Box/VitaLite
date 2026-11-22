package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Delegate;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.StreamerBridges;

/**
 * Has each tooltip marked as it is added while streamer mode is on, so the mirror can tell which ones to show.
 */
@Mixin("net/runelite/client/ui/overlay/tooltip/TooltipManager")
public class TooltipManagerMixin
{
    private static final String ADD = "(Lnet/runelite/client/ui/overlay/tooltip/Tooltip;)V";
    private static final String ADDED = "com/tonic/services/streamer/MirrorTooltips.added";

    /**
     * Marks each tooltip added while streamer mode is on.
     *
     * @param tooltip the tooltip added
     */
    @Insert(method = "add", desc = ADD, at = @At(value = AtTarget.HEAD), gate = StreamerBridges.STREAMER_MIRRORING)
    @Delegate(ADDED)
    public static native void streamerTooltipAdded(Object tooltip);

    /**
     * Marks each tooltip added to the front while streamer mode is on.
     *
     * @param tooltip the tooltip added
     */
    @Insert(method = "addFront", desc = ADD, at = @At(value = AtTarget.HEAD), gate = StreamerBridges.STREAMER_MIRRORING)
    @Delegate(ADDED)
    public static native void streamerTooltipAddedFront(Object tooltip);
}
