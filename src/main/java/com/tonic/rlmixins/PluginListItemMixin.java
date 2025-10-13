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

@Mixin("net/runelite/client/plugins/config/PluginListItem")
public class PluginListItemMixin
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
                ).build();

        method.instructions.insertBefore(
                insertionPoint,
                code
        );
    }
}
