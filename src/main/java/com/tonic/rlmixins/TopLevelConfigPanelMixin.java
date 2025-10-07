package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.BytecodeBuilder;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

@Mixin("net/runelite/client/plugins/config/TopLevelConfigPanel")
public class TopLevelConfigPanelMixin
{
    @Insert(
            method = "<init>",
            at = @At(
                    value = AtTarget.INVOKE,
                    owner = "net/runelite/client/plugins/config/TopLevelConfigPanel",
                    target = "addTab"
            ),
            ordinal = -1,
            raw = true
    )
    public static void constructorHook(MethodNode method, AbstractInsnNode insertionPoint)
    {
        InsnList code = BytecodeBuilder.create()
                .loadLocal(0, ALOAD)
                .invokeStatic(
                        "com/tonic/ui/sdn/VitaExternalsPanel",
                        "get",
                        "()Lcom/tonic/ui/sdn/VitaExternalsPanel;"
                )
                .pushString("/com/tonic/vitalite/icon-small.png")
                .pushString("Vita Hub")
                .invokeVirtual(
                        "net/runelite/client/plugins/config/TopLevelConfigPanel",
                        "addTab",
                        "(Lnet/runelite/client/ui/PluginPanel;Ljava/lang/String;Ljava/lang/String;)Lnet/runelite/client/ui/components/materialtabs/MaterialTab;"
                ).pop().build();
        method.instructions.insert(insertionPoint, code);
    }
}
