package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.model.ConditionType;
import com.tonic.util.asm.BytecodeBuilder;
import com.tonic.util.asm.InsnUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Leaves RuneLite's overlays laid over the scene undrawn while WorldOverlays says so, as while a VR
 * headset shows the world itself, and draws the rest, those over the interfaces too, through
 * GroundHooks so their ground shapes can be drawn on the ground.
 */
@Mixin("net/runelite/client/ui/overlay/OverlayRenderer")
public class OverlayRendererMixin
{
    private static final String GROUND_HOOKS = "com/tonic/services/ground/GroundHooks";
    private static final String LAYER_HOOK = "(Ljava/awt/Graphics2D;Lnet/runelite/client/ui/overlay/OverlayLayer;)";

    @Insert(
            method = "renderOverlayLayer",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void skipWorldLayers(MethodNode method, AbstractInsnNode insertionPoint)
    {
        for (AbstractInsnNode exit : InsnUtil.withOpcode(method, Opcodes.RETURN))
        {
            InsnList finish = BytecodeBuilder.create()
                    .loadLocal(1, Opcodes.ALOAD)
                    .loadLocal(2, Opcodes.ALOAD)
                    .invokeStatic(GROUND_HOOKS, "finish", LAYER_HOOK + "V")
                    .build();
            method.instructions.insertBefore(exit, finish);
        }

        LabelNode draw = new LabelNode();
        InsnList skip = BytecodeBuilder.create()
                .loadLocal(1, Opcodes.ALOAD)
                .loadLocal(2, Opcodes.ALOAD)
                .invokeStatic(GROUND_HOOKS, "wrap", LAYER_HOOK + "Ljava/awt/Graphics2D;")
                .storeLocal(1, Opcodes.ASTORE)
                .loadLocal(2, Opcodes.ALOAD)
                .invokeStatic("com/tonic/services/WorldOverlays", "skips", "(Ljava/lang/Object;)Z")
                .jumpIf(ConditionType.FALSE, draw)
                .returnVoid()
                .placeLabel(draw)
                .build();
        method.instructions.insert(skip);
    }

    @Insert(
            method = "renderAfterInterface",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void captureAfterInterface(MethodNode method, AbstractInsnNode insertionPoint)
    {
        method.instructions.insert(wrapWidgets());
    }

    @Insert(
            method = "renderAfterLayer",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void captureAfterLayer(MethodNode method, AbstractInsnNode insertionPoint)
    {
        method.instructions.insert(wrapWidgets());
    }

    private static InsnList wrapWidgets()
    {
        return BytecodeBuilder.create()
                .loadLocal(1, Opcodes.ALOAD)
                .invokeStatic(GROUND_HOOKS, "wrapWidgets", "(Ljava/awt/Graphics2D;)Ljava/awt/Graphics2D;")
                .storeLocal(1, Opcodes.ASTORE)
                .build();
    }
}
