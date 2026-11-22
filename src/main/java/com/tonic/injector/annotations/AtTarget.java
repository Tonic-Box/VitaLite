package com.tonic.injector.annotations;

/**
 * Enum defining the types of instruction patterns that can be matched with @At annotations.
 * This provides better IDE support, autocomplete, and compile-time validation compared to string literals.
 */
public enum AtTarget {
    /**
     * The start of the method's own code, after the superclass constructor call in a constructor. Calls are inserted before it.
     * Usage: @At(value = AtTarget.HEAD)
     */
    HEAD,
    
    /**
     * Method invocation (INVOKEVIRTUAL, INVOKESTATIC, INVOKEINTERFACE, INVOKESPECIAL)
     * Usage: @At(value = AtTarget.INVOKE, target = "methodName", owner = "ClassName")
     */
    INVOKE,
    
    /**
     * Instance field read access (GETFIELD)
     * Usage: @At(value = AtTarget.GETFIELD, target = "fieldName", owner = "ClassName")
     */
    GETFIELD,
    
    /**
     * Instance field write access (PUTFIELD) 
     * Usage: @At(value = AtTarget.PUTFIELD, target = "fieldName", owner = "ClassName")
     */
    PUTFIELD,
    
    /**
     * Static field read access (GETSTATIC)
     * Usage: @At(value = AtTarget.GETSTATIC, target = "fieldName", owner = "ClassName")
     */
    GETSTATIC,
    
    /**
     * Static field write access (PUTSTATIC)
     * Usage: @At(value = AtTarget.PUTSTATIC, target = "fieldName", owner = "ClassName")
     */
    PUTSTATIC,
    
    /**
     * Object instantiation (NEW)
     * Usage: @At(value = AtTarget.NEW, owner = "ClassName")
     */
    NEW,
    
    /**
     * Load constant instruction (LDC)
     * Usage: @At(value = AtTarget.LDC, constant = @Constant(...))
     */
    LDC,
    
    /**
     * Specific bytecode opcode
     * Usage: @At(value = AtTarget.OPCODE, opcode = "IMUL")
     */
    OPCODE,
    
    /**
     * Line number (requires debug information)
     * Usage: @At(value = AtTarget.LINE, line = 42)
     */
    LINE,
    
    /**
     * Return instructions (RETURN, IRETURN, ARETURN, etc.)
     * Usage: @At(value = AtTarget.RETURN)
     */
    RETURN,
    
    /**
     * Jump instructions (GOTO, IF_*, etc.)
     * Usage: @At(value = AtTarget.JUMP)
     */
    JUMP,
    
    /**
     * Load from local variable (ILOAD, LLOAD, FLOAD, DLOAD, ALOAD)
     * Usage: @At(value = AtTarget.LOAD, local = 7)
     */
    LOAD,
    
    /**
     * Store to local variable (ISTORE, LSTORE, FSTORE, DSTORE, ASTORE)
     * Usage: @At(value = AtTarget.STORE, local = 2)
     */
    STORE,
    
    /**
     * Increment local variable (IINC)
     * Usage: @At(value = AtTarget.IINC, local = 5)
     */
    IINC,
    
    /**
     * Array load operations (IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD)
     * Usage: @At(value = AtTarget.ARRAY_LOAD)
     */
    ARRAY_LOAD,
    
    /**
     * Array store operations (IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE)
     * Usage: @At(value = AtTarget.ARRAY_STORE)
     */
    ARRAY_STORE,
    
    /**
     * Type cast check instruction (CHECKCAST)
     * Usage: @At(value = AtTarget.CHECKCAST, owner = "net/runelite/client/RuneLite")
     * The owner parameter specifies the class type being cast to
     */
    CHECKCAST,
    
    /**
     * Empty/no targeting - used for slice defaults
     * Internal use only
     */
    NONE;
    
    /**
     * Convert enum to string for backwards compatibility with existing string-based logic.
     */
    @Override
    public String toString() {
        return name();
    }
}