package com.tonic.injector.annotations;

/**
 * Enum defining the shift positions for instruction injection.
 * This provides better IDE support, autocomplete, and compile-time validation compared to string literals.
 */
public enum Shift {
    /**
     * Insert before the matched instruction (HEAD)
     * The injected code will be executed before the target instruction
     */
    HEAD,
    
    /**
     * Insert after the matched instruction (TAIL) - default behavior
     * The injected code will be executed after the target instruction
     */
    TAIL;
}