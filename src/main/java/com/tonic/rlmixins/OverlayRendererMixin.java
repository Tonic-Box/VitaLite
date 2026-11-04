package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.model.ConditionType;
import com.tonic.util.asm.BytecodeBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Leaves RuneLite's overlays laid over the scene undrawn while WorldOverlays says so, as while a VR
 * headset shows the world itself.
 */
@Mixin("net/runelite/client/ui/overlay/OverlayRenderer")
public class OverlayRendererMixin
{
    @Insert(
            method = "renderOverlayLayer",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void skipWorldLayers(MethodNode method, AbstractInsnNode insertionPoint)
    {
        // At the very start: if (WorldOverlays.skips(layer)) return;
        LabelNode draw = new LabelNode();
        InsnList skip = BytecodeBuilder.create()
                .loadLocal(2, Opcodes.ALOAD)
                .invokeStatic("com/tonic/services/WorldOverlays", "skips", "(Ljava/lang/Object;)Z")
                .jumpIf(ConditionType.FALSE, draw)
                .returnVoid()
                .placeLabel(draw)
                .build();
        method.instructions.insert(skip);
    }
}
