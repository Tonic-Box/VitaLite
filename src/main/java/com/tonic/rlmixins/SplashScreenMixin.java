package com.tonic.rlmixins;

import com.tonic.Static;
import com.tonic.injector.annotations.*;
import com.tonic.injector.util.BytecodeBuilder;
import com.tonic.injector.util.LdcRewriter;
import com.tonic.model.ConditionType;
import com.tonic.vitalite.Main;
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
        BytecodeBuilder bb = BytecodeBuilder.create();
        var label = bb.createLabel("skipPatch");
        InsnList code = bb
                .invokeStatic(
                        "com/tonic/Static",
                        "getCliArgs",
                        "()Lcom/tonic/VitaLiteOptions;"
                )
                .invokeVirtual(
                        "com/tonic/VitaLiteOptions",
                        "isIncognito",
                        "()Z"
                )
                .jumpIf(ConditionType.TRUE, label)
                .pushThis()
                .invokeStatic(
                        "com/tonic/runelite/ClientUIUpdater",
                        "patchSplashScreen",
                        "(Ljavax/swing/JFrame;)V"
                )
                .placeLabel(label)
                .build();

        method.instructions.insertBefore(
                insertionPoint,
                code
        );

        if(Static.getCliArgs().isIncognito())
            return;

        LdcRewriter.rewriteString(method, "runelite_splash.png", "icon_splash.png");
        LdcRewriter.rewriteClassRef(method, "net/runelite/client/ui/SplashScreen", "com/tonic/vitalite/Main");
    }
}
