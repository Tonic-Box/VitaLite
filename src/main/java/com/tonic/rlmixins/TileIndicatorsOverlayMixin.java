package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.LocalPlayerGuard;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Stops the tile indicators overlay throwing on every frame drawn while the local player is not loaded, which it reads unchecked to find the hovered tile.
 */
@Mixin("net/runelite/client/plugins/tileindicators/TileIndicatorsOverlay")
public class TileIndicatorsOverlayMixin
{
    /**
     * Has the overlay draw nothing while the local player is not loaded.
     *
     * @param method TileIndicatorsOverlay.render(Graphics2D)
     * @param insertionPoint unused
     */
    @Insert(
            method = "render",
            desc = "(Ljava/awt/Graphics2D;)Ljava/awt/Dimension;",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void skipWithoutLocalPlayer(MethodNode method, AbstractInsnNode insertionPoint)
    {
        LocalPlayerGuard.returnWhenMissing(method);
    }
}
