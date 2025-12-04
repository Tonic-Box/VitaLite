package com.tonic.injector.pipeline;

import com.tonic.injector.annotations.MethodHook;
import com.tonic.injector.util.AnnotationUtil;
import com.tonic.injector.util.MethodUtil;
import com.tonic.injector.util.TransformerUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * Injects method calls at the beginning of target methods.
 */
public class MethodHookTransformer
{
    /**
     * Patches the target method specified in the {@link MethodHook} annotation
     * @param mixin the mixin class node containing the hook method
     * @param method the method node annotated with {@link MethodHook} to be used as a hook
     * @throws RuntimeException if the target method cannot be found
     */
    public static void patch(ClassNode mixin, MethodNode method) {
        String name = AnnotationUtil.getAnnotation(method, MethodHook.class, "value");
        ClassNode gamepack = TransformerUtil.getMethodClass(mixin, name);
        InjectTransformer.patch(gamepack, mixin, method);

        MethodNode toHook = TransformerUtil.getTargetMethod(mixin, name);

        if (toHook == null) {
            System.err.println("Could not find method to hook: " + name);
            return;
        }

        InsnList call = MethodUtil.generateContextAwareInvoke(gamepack, toHook, method, true);

        AbstractInsnNode injectionPoint = null;

        if (toHook.name.equals("<init>")) {
            for (AbstractInsnNode insn : toHook.instructions) {
                if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    if (methodInsn.name.equals("<init>")) {
                        injectionPoint = insn;
                        break;
                    }
                }
            }
        }

        if (injectionPoint != null) {
            toHook.instructions.insert(injectionPoint, call);
        } else {
            toHook.instructions.insert(call);
        }
    }
}
