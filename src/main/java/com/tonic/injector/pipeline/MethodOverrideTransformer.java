package com.tonic.injector.pipeline;

import com.tonic.injector.annotations.MethodOverride;
import com.tonic.injector.util.AnnotationUtil;
import com.tonic.injector.util.TransformerUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import java.util.HashMap;
import java.util.Map;

/**
 * Completely replaces method implementations with bytecode from mixin methods.
 */
public class MethodOverrideTransformer {
    /**
     * Replaces target method with mixin method implementation.
     * @param mixin mixin class containing replacement method
     * @param method method annotated with MethodOverride
     */
    public static void patch(ClassNode mixin, MethodNode method) {
        String name = AnnotationUtil.getAnnotation(method, MethodOverride.class, "value");
        MethodNode toReplace = TransformerUtil.getTargetMethod(mixin, name);

        if (toReplace == null) {
            String mixinTarget = AnnotationUtil.getAnnotation(mixin, com.tonic.injector.annotations.Mixin.class, "value");
            throw new RuntimeException("MethodOverride target not found: " + name + " in mixin " + mixinTarget + " (method: " + method.name + method.desc + ")");
        }

        toReplace.instructions.clear();
        toReplace.tryCatchBlocks.clear();
        toReplace.localVariables.clear();

        Map<LabelNode, LabelNode> labelMap = new HashMap<>();
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof LabelNode) {
                labelMap.put((LabelNode) insn, new LabelNode());
            }
        }

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            AbstractInsnNode cloned = insn.clone(labelMap);
            if (cloned != null) {
                toReplace.instructions.add(cloned);
            } else {
                System.err.println("[MethodOverrideTransformer] Warning: Failed to clone instruction: " + insn);
            }
        }

        toReplace.maxStack  = method.maxStack;
        toReplace.maxLocals = method.maxLocals;
    }
}