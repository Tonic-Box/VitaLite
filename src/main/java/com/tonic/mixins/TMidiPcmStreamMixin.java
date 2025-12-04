package com.tonic.mixins;

import com.tonic.Static;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.util.asm.BytecodeBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

@Mixin("MidiPcmStream")
public class TMidiPcmStreamMixin {
    @Insert(method = "<init>", at = @At(value = AtTarget.RETURN), raw = true)
    public static void constructorHook(MethodNode method, AbstractInsnNode insertionPoint) {
        if (!Static.getCliArgs().isNoMusic() && !Static.getCliArgs().isMin()) return;

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() != Opcodes.NEW) continue;

            TypeInsnNode typeInsn = (TypeInsnNode) insn;
            if (!typeInsn.desc.equals("java/util/PriorityQueue")) continue;

            AbstractInsnNode current = findPriorityQueueConstructor(insn);
            if (current == null) continue;

            AbstractInsnNode dupInsn = insn.getNext();
            if (dupInsn == null || dupInsn.getOpcode() != Opcodes.DUP) continue;

            removeInstructionsBetween(method, dupInsn.getNext(), current);
            replacePriorityQueueConstructor(method, current);
            break;
        }
    }

    private static AbstractInsnNode findPriorityQueueConstructor(AbstractInsnNode start) {
        AbstractInsnNode current = start;
        while (current != null) {
            if (current.getOpcode() == Opcodes.INVOKESPECIAL &&
                    current instanceof MethodInsnNode &&
                    ((MethodInsnNode) current).owner.equals("java/util/PriorityQueue")) {
                return current;
            }
            current = current.getNext();
        }
        return null;
    }

    private static void removeInstructionsBetween(MethodNode method, AbstractInsnNode start, AbstractInsnNode end)
    {
        AbstractInsnNode toRemove = start;
        while (toRemove != null && toRemove != end) {
            AbstractInsnNode next = toRemove.getNext();
            method.instructions.remove(toRemove);
            toRemove = next;
        }
    }

    private static void replacePriorityQueueConstructor(MethodNode method, AbstractInsnNode target) {
        InsnList replacement = BytecodeBuilder.create()
                .invokeSpecial("java/util/PriorityQueue", "<init>", "()V")
                .build();
        method.instructions.set(target, replacement.getFirst());
    }
}