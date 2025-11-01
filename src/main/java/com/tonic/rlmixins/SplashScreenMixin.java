package com.tonic.rlmixins;

import com.tonic.injector.annotations.*;
import com.tonic.injector.util.BytecodeBuilder;
import com.tonic.injector.util.LdcRewriter;
import com.tonic.model.ConditionType;
import com.tonic.vitalite.Main;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
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
                .getStaticField(
                        "com/tonic/vitalite/Main",
                        "optionsParser",
                        "Lcom/tonic/VitaLiteOptions;"
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
    }

    @Insert(
            method = "<init>",
            at = @At(
                    value = AtTarget.GETSTATIC,
                    target = "BRAND_ORANGE",
                    owner = "net/runelite/client/ui/ColorScheme"
            ),
            raw = true
    )
    public static void constructorHook2(MethodNode method, AbstractInsnNode insertionPoint)
    {
        if(Main.optionsParser.isIncognito())
            return;
        ((FieldInsnNode)insertionPoint).name = "GRAND_EXCHANGE_LIMIT";
        LdcRewriter.rewriteString(method, "runelite_splash.png", "icon_splash.png");
        LdcRewriter.rewriteClassRef(method, "net/runelite/client/ui/SplashScreen", "com/tonic/vitalite/Main");
    }
}
