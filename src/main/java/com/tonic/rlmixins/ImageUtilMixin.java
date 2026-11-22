package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.util.asm.BytecodeBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Has each image recoloured with a colour VitaLite restyled keep a stock twin, which the streamer mirror draws instead.
 */
@Mixin("net/runelite/client/util/ImageUtil")
public class ImageUtilMixin
{
    @Insert(
            method = "recolorImage",
            desc = "(Ljava/awt/Image;Ljava/awt/Color;)Ljava/awt/image/BufferedImage;",
            at = @At(value = AtTarget.RETURN),
            all = true,
            raw = true
    )
    public static void keepStockTwin(MethodNode method, AbstractInsnNode insertionPoint)
    {
        method.instructions.insertBefore(insertionPoint, BytecodeBuilder.create()
                .dup()
                .loadLocal(0, Opcodes.ALOAD)
                .loadLocal(1, Opcodes.ALOAD)
                .invokeStatic("com/tonic/services/streamer/MirrorImages", "recoloured", "(Ljava/awt/Image;Ljava/awt/Image;Ljava/awt/Color;)V")
                .build());
    }
}
