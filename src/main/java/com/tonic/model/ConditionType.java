package com.tonic.model;

import lombok.Getter;

import static org.objectweb.asm.Opcodes.*;

public enum ConditionType
{
    // Existing two-operand comparisons
    EQUALS(IF_ICMPEQ, IF_ICMPNE, true),
    NOT_EQUALS(IF_ICMPNE, IF_ICMPEQ, true),
    GREATER(IF_ICMPGT, IF_ICMPLE, true),
    LESS(IF_ICMPLT, IF_ICMPGE, true),
    GREATER_OR_EQUALS(IF_ICMPGE, IF_ICMPLT, true),
    LESS_OR_EQUALS(IF_ICMPLE, IF_ICMPGT, true),

    // New single-operand (boolean) comparisons
    TRUE(IFNE, IFEQ, false),      // if value != 0 (true)
    FALSE(IFEQ, IFNE, false);      // if value == 0 (false)

    @Getter
    private final int opcode;
    private final int inverse;
    @Getter
    private final boolean requiresTwoOperands;

    ConditionType(int opcode, int inverse, boolean requiresTwoOperands) {
        this.opcode = opcode;
        this.inverse = inverse;
        this.requiresTwoOperands = requiresTwoOperands;
    }

    public ConditionType invert()
    {
        for (ConditionType type : values())
        {
            if (type.opcode == inverse)
            {
                return type;
            }
        }
        return EQUALS; //default
    }
}