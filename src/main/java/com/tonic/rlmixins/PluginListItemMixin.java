package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.BytecodeBuilder;
import com.tonic.vitalite.Main;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.ArrayList;
import java.util.List;

@Mixin("net/runelite/client/plugins/config/PluginListItem")
public class PluginListItemMixin
{
    @Insert(
            method = "<init>",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void constructorHook(MethodNode method, AbstractInsnNode insertionPoint) {
        BytecodeBuilder builder = BytecodeBuilder.create();

        builder.tryCatch(
                "java/lang/Exception",
                tryBlock -> {
                    tryBlock
                            .pushThis()
                            .pushLocal(2)
                            .invokeVirtual(
                                    "net/runelite/client/plugins/config/PluginConfigurationDescriptor",
                                    "getPlugin",
                                    "()Lnet/runelite/client/plugins/Plugin;"
                            )
                            .invokeStatic(
                                    "com/tonic/services/hotswapper/PluginReloader",
                                    "addRedButtonAfterPin",
                                    "(Ljavax/swing/JPanel;Lnet/runelite/client/plugins/Plugin;)Ljavax/swing/JButton;"
                            )
                            .pop();
                },
                catchBlock -> {
                    catchBlock
                            .dup()
                            .invokeVirtual("java/lang/Exception", "printStackTrace", "()V")
                            .pop();
                }
        );

        InsnList code = builder.build();
        method.instructions.insertBefore(insertionPoint, code);

        List<TryCatchBlockNode> tryCatchBlocks = builder.getTryCatchBlocks();
        if (method.tryCatchBlocks == null) {
            method.tryCatchBlocks = new ArrayList<>();
        }
        method.tryCatchBlocks.addAll(tryCatchBlocks);
    }
}
