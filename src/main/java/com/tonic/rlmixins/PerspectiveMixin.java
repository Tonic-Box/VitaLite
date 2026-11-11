package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.util.asm.BytecodeBuilder;
import com.tonic.util.asm.InsnUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/**
 * Reports every local point Perspective projects, and the corners of every tile polygon it builds, so ground shapes can be found in what overlays draw.
 */
@Mixin("net/runelite/api/Perspective")
public class PerspectiveMixin
{
    private static final String GROUND_HOOKS = "com/tonic/services/ground/GroundHooks";

    @Insert(
            method = "localToCanvas",
            desc = "(Lnet/runelite/api/Client;IIII)Lnet/runelite/api/Point;",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void reportProjection(MethodNode method, AbstractInsnNode insertionPoint)
    {
        for (AbstractInsnNode exit : InsnUtil.withOpcode(method, Opcodes.ARETURN))
        {
            InsnList report = BytecodeBuilder.create()
                    .dup()
                    .loadLocal(0, Opcodes.ALOAD)
                    .loadLocal(1, Opcodes.ILOAD)
                    .loadLocal(2, Opcodes.ILOAD)
                    .loadLocal(3, Opcodes.ILOAD)
                    .loadLocal(4, Opcodes.ILOAD)
                    .invokeStatic(GROUND_HOOKS, "projected", "(Lnet/runelite/api/Point;Lnet/runelite/api/Client;IIII)V")
                    .build();
            method.instructions.insertBefore(exit, report);
        }
    }

    @Insert(
            method = "getCanvasTileAreaPoly",
            desc = "(Lnet/runelite/api/Client;Lnet/runelite/api/coords/LocalPoint;IIII)Ljava/awt/Polygon;",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void reportTileArea(MethodNode method, AbstractInsnNode insertionPoint)
    {
        for (AbstractInsnNode exit : InsnUtil.withOpcode(method, Opcodes.ARETURN))
        {
            InsnList leave = BytecodeBuilder.create()
                    .invokeStatic(GROUND_HOOKS, "exitArea", "()V")
                    .build();
            method.instructions.insertBefore(exit, leave);
        }

        InsnList enter = BytecodeBuilder.create()
                .loadLocal(4, Opcodes.ILOAD)
                .invokeStatic(GROUND_HOOKS, "enterArea", "(I)V")
                .build();
        method.instructions.insert(enter);
    }
}
