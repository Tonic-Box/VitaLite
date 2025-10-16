package com.tonic.injector.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.*;

public class ControlFlow
{
    /**
     * Analyze control flow and return instructions in approximate execution order
     */
    public static List<AbstractInsnNode> normalize(MethodNode method) {
        List<AbstractInsnNode> executionOrder = new ArrayList<>();
        Set<AbstractInsnNode> visited = new HashSet<>();

        Map<LabelNode, AbstractInsnNode> labelMap = new HashMap<>();
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LabelNode) {
                labelMap.put((LabelNode) insn, insn);
            }
        }

        AbstractInsnNode start = method.instructions.getFirst();
        dfsTraversal(start, executionOrder, visited, labelMap);

        return executionOrder;
    }

    private static void dfsTraversal(AbstractInsnNode current, List<AbstractInsnNode> order, Set<AbstractInsnNode> visited, Map<LabelNode, AbstractInsnNode> labelMap) {
        if(current == null || visited.contains(current)) {
            return;
        }

        visited.add(current);
        order.add(current);

        int opcode = current.getOpcode();
        if(current instanceof JumpInsnNode) {
            JumpInsnNode jump = (JumpInsnNode) current;
            if (opcode != Opcodes.GOTO) {
                dfsTraversal(current.getNext(), order, visited, labelMap);
            }

            AbstractInsnNode target = labelMap.get(jump.label);
            dfsTraversal(target, order, visited, labelMap);

        }
        else if (current instanceof TableSwitchInsnNode) {
            TableSwitchInsnNode switchInsn = (TableSwitchInsnNode) current;
            dfsTraversal(labelMap.get(switchInsn.dflt), order, visited, labelMap);

            for (LabelNode label : switchInsn.labels) {
                dfsTraversal(labelMap.get(label), order, visited, labelMap);
            }
        }
        else if (current instanceof LookupSwitchInsnNode) {
            LookupSwitchInsnNode switchInsn = (LookupSwitchInsnNode) current;

            dfsTraversal(labelMap.get(switchInsn.dflt), order, visited, labelMap);

            for (LabelNode label : switchInsn.labels) {
                dfsTraversal(labelMap.get(label), order, visited, labelMap);
            }

        }
        else if (opcode == Opcodes.ATHROW || opcode == Opcodes.RETURN ||
                opcode == Opcodes.ARETURN || opcode == Opcodes.IRETURN ||
                opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN ||
                opcode == Opcodes.DRETURN) {
            return;

        }
        else {
            dfsTraversal(current.getNext(), order, visited, labelMap);
        }
    }
}
