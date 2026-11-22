package com.tonic.injector.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

/**
 * Static boolean gates named as "owner/Class.field", which injected code checks before it runs.
 */
public final class Gates
{
    private Gates()
    {
    }

    /**
     * Runs code only while a gate is true.
     *
     * @param gate the gate, or empty for none
     * @param code the code to run
     * @return the code, behind the gate when there is one
     * @throws IllegalArgumentException if the gate is not "owner/Class.field"
     */
    public static InsnList around(String gate, InsnList code)
    {
        if (gate.isEmpty())
        {
            return code;
        }
        LabelNode closed = new LabelNode();
        InsnList gated = new InsnList();
        gated.add(read(gate));
        gated.add(new JumpInsnNode(Opcodes.IFEQ, closed));
        gated.add(code);
        gated.add(closed);
        return gated;
    }

    /**
     * Reads a gate.
     *
     * @param gate the gate
     * @return the read, leaving the gate's value on the stack
     * @throws IllegalArgumentException if the gate is not "owner/Class.field"
     */
    public static FieldInsnNode read(String gate)
    {
        int dot = gate.lastIndexOf('.');
        if (dot <= 0 || dot == gate.length() - 1)
        {
            throw new IllegalArgumentException("gate must be \"owner/Class.field\": " + gate);
        }
        return new FieldInsnNode(Opcodes.GETSTATIC, gate.substring(0, dot), gate.substring(dot + 1), "Z");
    }
}
