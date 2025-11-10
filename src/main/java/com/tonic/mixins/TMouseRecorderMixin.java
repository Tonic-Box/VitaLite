package com.tonic.mixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.LoopSleepInjector;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

@Mixin("MouseRecorder")
public class TMouseRecorderMixin {
    @Insert(method = "run", at = @At(value = AtTarget.RETURN), raw = true)
    public static void constructorHook(MethodNode method, AbstractInsnNode insertionPoint) {
        LoopSleepInjector.injectConditionalSleepSafe(method, "com/tonic/services/ClickManager", "shouldBlockManualMovement", "()Z");
    }
}
