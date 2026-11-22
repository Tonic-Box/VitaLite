package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Delegate;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;

/**
 * Reports each world map point added to MirrorWorldMap, which marks those that content which is not stock adds. It runs while streamer mode is off too, as points last.
 */
@Mixin("net/runelite/client/ui/overlay/worldmap/WorldMapPointManager")
public class WorldMapPointManagerMixin
{
    /**
     * Reports a point added.
     *
     * @param point the point
     */
    @Insert(method = "add", desc = "(Lnet/runelite/client/ui/overlay/worldmap/WorldMapPoint;)V", at = @At(value = AtTarget.HEAD))
    @Delegate("com/tonic/services/streamer/MirrorWorldMap.added")
    public static native void streamerPointAdded(Object point);
}
