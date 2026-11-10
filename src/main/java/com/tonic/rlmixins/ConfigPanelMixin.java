package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.util.asm.BytecodeBuilder;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

@Mixin("net/runelite/client/plugins/config/ConfigPanel")
public class ConfigPanelMixin
{
    @Insert(
            method = "createCheckbox",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void switchCheckbox(MethodNode method, AbstractInsnNode insertionPoint)
    {
        method.instructions.insertBefore(insertionPoint, BytecodeBuilder.create()
                .dup()
                .invokeStatic(
                        "com/tonic/ui/sdn/HubTheme",
                        "styleSwitch",
                        "(Ljavax/swing/JCheckBox;)V"
                )
                .build());
    }
}
