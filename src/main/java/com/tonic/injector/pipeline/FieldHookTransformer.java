package com.tonic.injector.pipeline;

import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JField;
import com.tonic.injector.Injector;
import com.tonic.injector.util.MappingProvider;
import com.tonic.injector.annotations.FieldHook;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.types.FieldHookDef;
import com.tonic.injector.util.AnnotationUtil;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Intercepts field write operations with conditional hook method calls.
 */
public class FieldHookTransformer {
    private static final List<FieldHookDef> fieldHooks = new ArrayList<>();

    /**
     * Registers field hook for later instrumentation.
     * @param mixin mixin class containing hook method
     * @param method method annotated with FieldHook
     */
    public static void patch(ClassNode mixin, MethodNode method)
    {
        String gamepackName = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
        String name = AnnotationUtil.getAnnotation(method, FieldHook.class, "value");
        
        JClass jClass = MappingProvider.getClass(gamepackName);
        JField jField = MappingProvider.getField(jClass, name);

        ClassNode gamepack = Injector.gamepack.get(jField.getOwnerObfuscatedName());
        InjectTransformer.patch(gamepack, mixin, method);

        FieldHookDef hook = new FieldHookDef(jField, method.name, jField.getOwnerObfuscatedName(), method.desc);
        fieldHooks.add(hook);
    }

    /**
     * Instruments all methods in class with registered field hooks.
     * @param classNode class to instrument
     */
    public static void instrument(ClassNode classNode)
    {
        for(FieldHookDef hook : fieldHooks)
        {
            for(MethodNode methodNode : classNode.methods)
            {
                instrument(methodNode, hook);
            }
        }
    }

    /**
     * Instruments single method with field access hooks.
     * @param method method to instrument
     * @param hook field hook definition
     */
    private static void instrument(MethodNode method, FieldHookDef hook) {
        String desc = hook.getTarget().getDescriptor();
        boolean isStatic = hook.isStatic();

        if((method.name + method.desc).equals(hook.getHookMethod() + hook.getHookDesc()))
            return;

        List<AbstractInsnNode> fieldSets = getInjectionPoints(method, hook, isStatic);
        for (AbstractInsnNode fieldInsn : fieldSets) {
            InsnList wrapper = new InsnList();
            LabelNode skipLabel = new LabelNode();

            if (isStatic) {
                wrapper.add(new InsnNode(getDupOpcode(desc)));
                
                if (hook.getTarget().getGetter() != null) {
                    Number multiplier = hook.getTarget().getGetter();
                    if (desc.equals("I")) {
                        wrapper.add(new LdcInsnNode(multiplier.intValue()));
                        wrapper.add(new InsnNode(Opcodes.IMUL));
                    } else if (desc.equals("J")) {
                        wrapper.add(new LdcInsnNode(multiplier.longValue()));
                        wrapper.add(new InsnNode(Opcodes.LMUL));
                    }
                }
                
                wrapper.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        hook.getHookClass(),
                        hook.getHookMethod(),
                        hook.getHookDesc(),
                        false
                ));
                wrapper.add(new JumpInsnNode(Opcodes.IFNE, skipLabel));
                if (isWideType(desc)) {
                    wrapper.add(new InsnNode(Opcodes.POP2));
                } else {
                    wrapper.add(new InsnNode(Opcodes.POP));
                }
                LabelNode continueLabel = new LabelNode();
                wrapper.add(new JumpInsnNode(Opcodes.GOTO, continueLabel));
                wrapper.add(skipLabel);
                method.instructions.insertBefore(fieldInsn, wrapper);
                method.instructions.insert(fieldInsn, continueLabel);
            } else {
                //dupe obj ref {obj, value} -> {obj, value, obj}
                wrapper.add(new InsnNode(Opcodes.DUP2));

                //wrapper.add(new InsnNode(getDupOpcode(desc)));
//                if (isWideType(desc)) {
//                    wrapper.add(new InsnNode(Opcodes.DUP2_X1));
//                    wrapper.add(new InsnNode(Opcodes.POP2));
//                    wrapper.add(new InsnNode(Opcodes.DUP2));
//                } else {
//                    wrapper.add(new InsnNode(Opcodes.DUP_X1));
//                    wrapper.add(new InsnNode(Opcodes.POP));
//                    wrapper.add(new InsnNode(Opcodes.DUP));
//                }
                
                if (hook.getTarget().getGetter() != null) {
                    Number multiplier = hook.getTarget().getGetter();
                    if (desc.equals("I")) {
                        wrapper.add(new LdcInsnNode(multiplier.intValue()));
                        wrapper.add(new InsnNode(Opcodes.IMUL));
                    } else if (desc.equals("J")) {
                        wrapper.add(new LdcInsnNode(multiplier.longValue()));
                        wrapper.add(new InsnNode(Opcodes.LMUL));
                    }
                }
                
                wrapper.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        hook.getHookClass(),
                        hook.getHookMethod(),
                        hook.getHookDesc(),
                        false
                ));
                wrapper.add(new JumpInsnNode(Opcodes.IFNE, skipLabel));
                if (isWideType(desc)) {
                    wrapper.add(new InsnNode(Opcodes.POP2));
                } else {
                    wrapper.add(new InsnNode(Opcodes.POP));
                }
                wrapper.add(new InsnNode(Opcodes.POP));
                LabelNode continueLabel = new LabelNode();
                wrapper.add(new JumpInsnNode(Opcodes.GOTO, continueLabel));
                wrapper.add(skipLabel);

                method.instructions.insertBefore(fieldInsn, wrapper);
                method.instructions.insert(fieldInsn, continueLabel);
            }
        }
    }

    /**
     * Finds field access instructions matching hook definition.
     * @param method method to scan
     * @param hook hook definition
     * @param isStatic whether to look for static field access
     * @return matching field access instructions
     */
    @NotNull
    private static List<AbstractInsnNode> getInjectionPoints(MethodNode method, FieldHookDef hook, boolean isStatic) {
        List<AbstractInsnNode> fieldSets = new ArrayList<>();
        for (AbstractInsnNode insn = method.instructions.getFirst();
             insn != null; insn = insn.getNext()) {

            if ((isStatic && insn.getOpcode() == Opcodes.PUTSTATIC) || (!isStatic && insn.getOpcode() == Opcodes.PUTFIELD)) {
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                if (fieldInsn.owner.equals(hook.getTarget().getOwnerObfuscatedName()) &&
                        fieldInsn.name.equals(hook.getTarget().getObfuscatedName()) &&
                        fieldInsn.desc.equals(hook.getTarget().getDescriptor())) {
                    fieldSets.add(insn);
                }
            }
        }
        return fieldSets;
    }

    /**
     * Gets DUP opcode for field descriptor.
     * @param desc field descriptor
     * @return DUP2 for wide types, DUP for others
     */
    private static int getDupOpcode(String desc) {
        return (desc.equals("J") || desc.equals("D")) ? Opcodes.DUP2 : Opcodes.DUP;
    }

    /**
     * Checks if field descriptor represents wide type.
     * @param desc field descriptor
     * @return true for long/double types
     */
    private static boolean isWideType(String desc) {
        return desc.equals("J") || desc.equals("D");
    }
}
