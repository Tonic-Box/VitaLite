package com.tonic.rlmixins;

import com.tonic.injector.annotations.*;
import com.tonic.injector.util.BytecodeBuilder;
import org.objectweb.asm.tree.*;

@Mixin("net/runelite/client/eventbus/EventBus")
public class EventBusMixin {
    @Insert(
            method = "register",
            at = @At(
                    value = AtTarget.INVOKE,
                    owner = "com/google/common/base/Preconditions",
                    target = "checkArgument(ZLjava/lang/Object;)V"
            ),
            ordinal = -1,
            raw = true
    )
    public static void register(ClassNode classNode, MethodNode method, AbstractInsnNode insertionPoint)
    {
        InsnList toInject = BytecodeBuilder.create()
                .pop2()
                .build();

        method.instructions.insertBefore(insertionPoint, toInject);
        method.instructions.remove(insertionPoint);
    }
}
