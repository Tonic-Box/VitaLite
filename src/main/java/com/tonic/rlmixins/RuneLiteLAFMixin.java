package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Passes RuneLite's look-and-feel defaults through MirrorLook as they are built, so the colours VitaLite's accent changed can show stock in the streamer mirror.
 */
@Mixin("net/runelite/client/ui/laf/RuneLiteLAF")
public class RuneLiteLAFMixin
{
    @Insert(
            method = "getDefaults",
            desc = "()Ljavax/swing/UIDefaults;",
            at = @At(value = AtTarget.RETURN),
            all = true,
            raw = true
    )
    public static void mirrorDefaults(MethodNode method, AbstractInsnNode insertionPoint)
    {
        method.instructions.insertBefore(insertionPoint, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/tonic/services/streamer/MirrorLook", "defaults", "(Ljavax/swing/UIDefaults;)Ljavax/swing/UIDefaults;", false));
    }
}
