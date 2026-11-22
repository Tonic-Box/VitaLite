package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Delegate;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.Gates;
import com.tonic.injector.util.StreamerBridges;
import com.tonic.util.asm.BytecodeBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/**
 * Lets GroundHooks draw the frame's ground shapes into the GPU plugin's scene, after the scene and before it is shown, and has GpuMirror read the scene back for the streamer mirror just before that.
 */
@Mixin("net/runelite/client/plugins/gpu/GpuPlugin")
public class GpuPluginMixin
{
    private static final String GPU_PLUGIN = "net/runelite/client/plugins/gpu/GpuPlugin";

    /**
     * Reads the scene back for the streamer mirror, then draws the frame's ground shapes into it, just before the plugin shows it.
     *
     * @param method GpuPlugin.draw(int)
     * @param insertionPoint the blitSceneFbo call
     */
    @Insert(method = "draw", desc = "(I)V", at = @At(value = AtTarget.INVOKE, owner = GPU_PLUGIN, target = "blitSceneFbo"), expectedMatches = 1, raw = true)
    public static void drawScene(MethodNode method, AbstractInsnNode insertionPoint)
    {
        InsnList readBack = BytecodeBuilder.create()
                .loadLocal(0, Opcodes.ALOAD)
                .getField(GPU_PLUGIN, "fboScene", "I")
                .loadLocal(0, Opcodes.ALOAD)
                .getField(GPU_PLUGIN, "lastStretchedCanvasWidth", "I")
                .loadLocal(0, Opcodes.ALOAD)
                .getField(GPU_PLUGIN, "lastStretchedCanvasHeight", "I")
                .invokeStatic("com/tonic/services/streamer/GpuMirror", "scene", "(III)V")
                .build();
        InsnList ground = BytecodeBuilder.create()
                .loadLocal(0, Opcodes.ALOAD)
                .getField(GPU_PLUGIN, "fboScene", "I")
                .getStaticField(GPU_PLUGIN, "glProgram", "I")
                .loadLocal(0, Opcodes.ALOAD)
                .getField(GPU_PLUGIN, "uniWorldProj", "I")
                .invokeStatic("com/tonic/services/ground/GroundHooks", "drawGl", "(III)V")
                .build();
        method.instructions.insertBefore(insertionPoint, Gates.around(StreamerBridges.STREAMER_ACTIVE, readBack));
        method.instructions.insertBefore(insertionPoint, ground);
    }

    /**
     * Ends streamer mode's GPU readback at each return of the plugin's frame, which also lets go of its buffers once streamer mode is off.
     */
    @Insert(method = "draw", desc = "(I)V", at = @At(value = AtTarget.RETURN), all = true)
    @Delegate("com/tonic/services/streamer/GpuMirror.frameDone")
    public static native void streamerFrameDone();
}
