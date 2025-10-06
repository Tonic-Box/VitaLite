package com.tonic.injector.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class LogCallRemover {
    public static void removeLogInfoCall(MethodNode method) {
        AbstractInsnNode[] insns = method.instructions.toArray();

        for (int i = 0; i < insns.length - 3; i++) {
            if (insns[i] instanceof FieldInsnNode
                    && insns[i].getOpcode() == Opcodes.GETSTATIC
                    && ((FieldInsnNode) insns[i]).name.equals("log")
                    && insns[i + 1] instanceof LdcInsnNode
                    && "Side-loading plugin {}".equals(((LdcInsnNode) insns[i + 1]).cst)
                    && insns[i + 2].getOpcode() == Opcodes.ALOAD
                    && insns[i + 3] instanceof MethodInsnNode
                    && ((MethodInsnNode) insns[i + 3]).name.equals("info")) {

                for (int j = 0; j < 4; j++) {
                    method.instructions.remove(insns[i + j]);
                }
                break;
            }
        }
    }
}
