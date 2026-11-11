package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.util.asm.BytecodeBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Lets GroundHooks draw the frame's ground shapes into the GPU plugin's scene, after the scene and before it is shown.
 */
@Mixin("net/runelite/client/plugins/gpu/GpuPlugin")
public class GpuPluginMixin
{
    private static final String GPU_PLUGIN = "net/runelite/client/plugins/gpu/GpuPlugin";

    @Insert(
            method = "draw",
            desc = "(I)V",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void drawGround(MethodNode method, AbstractInsnNode insertionPoint)
    {
        for (AbstractInsnNode instruction : method.instructions)
        {
            if (!(instruction instanceof MethodInsnNode))
            {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!call.owner.equals(GPU_PLUGIN) || !call.name.equals("blitSceneFbo"))
            {
                continue;
            }

            InsnList ground = BytecodeBuilder.create()
                    .loadLocal(0, Opcodes.ALOAD)
                    .getField(GPU_PLUGIN, "fboScene", "I")
                    .getStaticField(GPU_PLUGIN, "glProgram", "I")
                    .loadLocal(0, Opcodes.ALOAD)
                    .getField(GPU_PLUGIN, "uniWorldProj", "I")
                    .invokeStatic("com/tonic/services/ground/GroundHooks", "drawGl", "(III)V")
                    .build();
            method.instructions.insertBefore(call, ground);
            return;
        }
    }
}
