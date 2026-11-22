package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.LocalPlayerGuard;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Stops the ground marker overlay throwing while the local player is not loaded, which it reads unchecked for each marked tile it draws.
 */
@Mixin("net/runelite/client/plugins/groundmarkers/GroundMarkerOverlay")
public class GroundMarkerOverlayMixin
{
    /**
     * Has each marked tile's draw skip while the local player is not loaded.
     *
     * @param method GroundMarkerOverlay.drawTile(Graphics2D, WorldView, WorldPoint, Color, String, Stroke)
     * @param insertionPoint unused
     */
    @Insert(
            method = "drawTile",
            desc = "(Ljava/awt/Graphics2D;Lnet/runelite/api/WorldView;Lnet/runelite/api/coords/WorldPoint;Ljava/awt/Color;Ljava/lang/String;Ljava/awt/Stroke;)V",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void skipWithoutLocalPlayer(MethodNode method, AbstractInsnNode insertionPoint)
    {
        LocalPlayerGuard.returnWhenMissing(method);
    }
}
