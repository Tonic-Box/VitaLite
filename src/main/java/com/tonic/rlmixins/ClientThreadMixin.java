package com.tonic.rlmixins;

import com.tonic.injector.annotations.*;
import com.tonic.util.asm.BytecodeBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

@Mixin("net/runelite/client/callback/ClientThread")
public class ClientThreadMixin {

    @Insert(
        method = "invoke",
        desc = "(Ljava/lang/Runnable;)V",
        at = @At(value = AtTarget.LOAD, local = 0, shift = Shift.HEAD),
        ordinal = 0,
        raw = true
    )
    public static void wrapInvoke(MethodNode method, AbstractInsnNode insertionPoint) {
        InsnList wrapper = BytecodeBuilder.create()
            .newInstance("com/tonic/services/watchdog/TrackedRunnable")
            .dup()
            .loadLocal(1, Opcodes.ALOAD)
            .invokeSpecial("com/tonic/services/watchdog/TrackedRunnable", "<init>", "(Ljava/lang/Runnable;)V")
            .storeLocal(1, Opcodes.ASTORE)
            .build();
        method.instructions.insertBefore(insertionPoint, wrapper);
    }

    @Insert(
        method = "invokeLater",
        desc = "(Ljava/lang/Runnable;)V",
        at = @At(value = AtTarget.LOAD, local = 0, shift = Shift.HEAD),
        ordinal = 0,
        raw = true
    )
    public static void wrapInvokeLater(MethodNode method, AbstractInsnNode insertionPoint) {
        InsnList wrapper = BytecodeBuilder.create()
            .newInstance("com/tonic/services/watchdog/TrackedRunnable")
            .dup()
            .loadLocal(1, Opcodes.ALOAD)
            .invokeSpecial("com/tonic/services/watchdog/TrackedRunnable", "<init>", "(Ljava/lang/Runnable;)V")
            .storeLocal(1, Opcodes.ASTORE)
            .build();
        method.instructions.insertBefore(insertionPoint, wrapper);
    }
}
