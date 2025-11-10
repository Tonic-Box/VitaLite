package com.tonic.injector.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class LoopSleepInjector {

    /**
     * Container for loop structure information
     */
    private static class LoopInfo {
        AbstractInsnNode aloadNode;
        FieldInsnNode getfieldNode;
        JumpInsnNode ifeqNode;
        LabelNode loopStartLabel;

        LoopInfo(AbstractInsnNode aload, FieldInsnNode getfield, JumpInsnNode ifeq, LabelNode label) {
            this.aloadNode = aload;
            this.getfieldNode = getfield;
            this.ifeqNode = ifeq;
            this.loopStartLabel = label;
        }
    }

    /**
     * Finds the loop structure including the condition check and loop labels
     */
    private static LoopInfo findLoopStructure(MethodNode methodNode) {
        AbstractInsnNode insn = methodNode.instructions.getFirst();

        while (insn != null) {
            if (insn.getType() == AbstractInsnNode.LABEL) {
                LabelNode potentialLoopLabel = (LabelNode) insn;
                AbstractInsnNode next = insn.getNext();
                while (next != null && (next.getType() == AbstractInsnNode.FRAME ||
                        next.getType() == AbstractInsnNode.LINE)) {
                    next = next.getNext();
                }
                if (next != null && next.getOpcode() == Opcodes.ALOAD) {
                    VarInsnNode aload = (VarInsnNode) next;
                    if (aload.var == 0) {
                        AbstractInsnNode getfield = aload.getNext();
                        if (getfield != null && getfield.getOpcode() == Opcodes.GETFIELD) {
                            FieldInsnNode field = (FieldInsnNode) getfield;
                            if ("Z".equals(field.desc)) {
                                AbstractInsnNode ifeq = field.getNext();
                                if (ifeq != null && ifeq.getOpcode() == Opcodes.IFEQ) {
                                    return new LoopInfo(aload, field, (JumpInsnNode) ifeq, potentialLoopLabel);
                                }
                            }
                        }
                    }
                }
            }
            insn = insn.getNext();
        }

        return null;
    }

    /**
     * Version with try-catch for InterruptedException handling
     */
    public static void injectConditionalSleepSafe(MethodNode methodNode,
                                                  String targetClass,
                                                  String targetMethod,
                                                  String targetDesc) {
        LoopInfo loopInfo = findLoopStructure(methodNode);

        if (loopInfo == null) {
            throw new IllegalStateException("Could not find loop pattern in method");
        }

        InsnList injection = new InsnList();

        LabelNode skipSleep = new LabelNode();
        LabelNode tryStart = new LabelNode();
        LabelNode tryEnd = new LabelNode();
        LabelNode catchBlock = new LabelNode();
        injection.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                targetClass,
                targetMethod,
                targetDesc,
                false
        ));
        injection.add(new JumpInsnNode(Opcodes.IFEQ, skipSleep));
        injection.add(tryStart);
        injection.add(new LdcInsnNode(50L));
        injection.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "java/lang/Thread",
                "sleep",
                "(J)V",
                false
        ));

        injection.add(tryEnd);
        injection.add(new JumpInsnNode(Opcodes.GOTO, loopInfo.loopStartLabel));
        injection.add(catchBlock);
        injection.add(new InsnNode(Opcodes.POP)); // Pop the exception
        injection.add(new JumpInsnNode(Opcodes.GOTO, loopInfo.loopStartLabel));
        injection.add(skipSleep);
        methodNode.tryCatchBlocks.add(0, new TryCatchBlockNode(
                tryStart,
                tryEnd,
                catchBlock,
                "java/lang/InterruptedException"
        ));
        methodNode.instructions.insert(loopInfo.ifeqNode, injection);
    }
}
