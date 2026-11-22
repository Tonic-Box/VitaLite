package com.tonic.rlmixins;

import com.tonic.injector.annotations.*;
import com.tonic.util.asm.BytecodeBuilder;
import com.tonic.injector.util.LdcRewriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

@Mixin("net/runelite/client/ui/SplashScreen")
public class SplashScreenMixin
{
    @Insert(
            method = "<init>",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void constructorHook(MethodNode method, AbstractInsnNode insertionPoint)
    {
        InsnList code = BytecodeBuilder.create()
                .pushThis()
                .invokeStatic(
                        "com/tonic/runelite/ClientUIUpdater",
                        "patchSplashScreen",
                        "(Ljavax/swing/JFrame;)V"
                )
                .build();

        method.instructions.insertBefore(
                insertionPoint,
                code
        );

        LdcRewriter.rewriteString(method, "runelite_splash.png", "icon_splash.png");
        LdcRewriter.rewriteClassRef(method, "net/runelite/client/ui/SplashScreen", "com/tonic/vitalite/Main");
    }
}
