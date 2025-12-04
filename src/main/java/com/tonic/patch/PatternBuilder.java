package com.tonic.patch;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Constant;
import com.tonic.injector.annotations.Shift;

/**
 * Utility class for building common instruction patterns for @Insert annotations.
 * Provides fluent API for creating complex patterns without verbose annotation syntax.
 * 
 * Usage examples:
 * 
 * // Method call patterns
 * PatternBuilder.invoke("updatePosition").build()
 * PatternBuilder.invoke("Entity.setHealth(I)V").build()
 * PatternBuilder.invokeStatic("Math.max(II)I").build()
 * 
 * // Field access patterns  
 * PatternBuilder.getField("player.health").build()
 * PatternBuilder.putField("Entity.x").build()
 * 
 * // Constant patterns
 * PatternBuilder.constant(100).build()
 * PatternBuilder.constant("PLAYER_DIED").build()
 * 
 * // Complex patterns with chaining
 * PatternBuilder.invoke("process").before().build()
 * PatternBuilder.opcode("IMUL").after().ordinal(2).build()
 */
public class PatternBuilder {
    
    private AtTarget value = AtTarget.INVOKE;
    private String target = "";
    private String owner = "";
    private String opcode = "";
    private int line = -1;
    private Constant constant = null;
    private Shift shift = Shift.TAIL;
    
    private PatternBuilder() {}
    
    // Factory methods for common patterns
    public static PatternBuilder invoke(String methodTarget) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.INVOKE;
        builder.target = methodTarget;
        return builder;
    }
    
    public static PatternBuilder invokeStatic(String methodTarget) {
        return invoke(methodTarget);
    }
    
    public static PatternBuilder getField(String fieldTarget) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.GETFIELD;
        builder.target = fieldTarget;
        return builder;
    }
    
    public static PatternBuilder putField(String fieldTarget) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.PUTFIELD;
        builder.target = fieldTarget;
        return builder;
    }
    
    public static PatternBuilder getStatic(String fieldTarget) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.GETSTATIC;
        builder.target = fieldTarget;
        return builder;
    }
    
    public static PatternBuilder putStatic(String fieldTarget) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.PUTSTATIC;
        builder.target = fieldTarget;
        return builder;
    }
    
    public static PatternBuilder newInstance(String className) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.NEW;
        builder.target = className;
        return builder;
    }
    
    public static PatternBuilder constant(int value) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.LDC;
        builder.constant = new ConstantImpl().withInt(value);
        return builder;
    }
    
    public static PatternBuilder constant(long value) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.LDC;
        builder.constant = new ConstantImpl().withLong(value);
        return builder;
    }
    
    public static PatternBuilder constant(float value) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.LDC;
        builder.constant = new ConstantImpl().withFloat(value);
        return builder;
    }
    
    public static PatternBuilder constant(double value) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.LDC;
        builder.constant = new ConstantImpl().withDouble(value);
        return builder;
    }
    
    public static PatternBuilder constant(String value) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.LDC;
        builder.constant = new ConstantImpl().withString(value);
        return builder;
    }
    
    public static PatternBuilder constant(Class<?> value) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.LDC;
        builder.constant = new ConstantImpl().withClass(value);
        return builder;
    }
    
    public static PatternBuilder opcode(String opcName) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.OPCODE;
        builder.opcode = opcName;
        return builder;
    }
    
    public static PatternBuilder line(int lineNumber) {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.LINE;
        builder.line = lineNumber;
        return builder;
    }
    
    public static PatternBuilder returnInsn() {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.RETURN;
        return builder;
    }
    
    public static PatternBuilder jump() {
        PatternBuilder builder = new PatternBuilder();
        builder.value = AtTarget.JUMP;
        return builder;
    }
    
    // Modifier methods
    public PatternBuilder before() {
        this.shift = Shift.HEAD;
        return this;
    }
    
    public PatternBuilder after() {
        this.shift = Shift.TAIL;
        return this;
    }
    
    public PatternBuilder owner(String ownerClass) {
        this.owner = ownerClass;
        return this;
    }
    
    // Build the At annotation
    public At build() {
        return new AtImpl(value, target, owner, opcode, line, constant, shift);
    }
    
    // Implementation classes
    private static class AtImpl implements At {
        private final AtTarget value;
        private final String target;
        private final String owner;
        private final String opcode;
        private final int line;
        private final Constant constant;
        private final Shift shift;
        
        public AtImpl(AtTarget value, String target, String owner, String opcode, int line, Constant constant, Shift shift) {
            this.value = value;
            this.target = target;
            this.owner = owner;
            this.opcode = opcode;
            this.line = line;
            this.constant = constant != null ? constant : new ConstantImpl();
            this.shift = shift;
        }
        
        @Override
        public AtTarget value() { return value; }
        
        @Override
        public String target() { return target; }
        
        @Override
        public String owner() { return owner; }
        
        @Override
        public String opcode() { return opcode; }
        
        @Override
        public int line() { return line; }

        @Override
        public int local() {
            return 0;
        }

        @Override
        public Constant constant() { return constant; }
        
        @Override
        public Shift shift() { return shift; }
        
        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return At.class;
        }
    }
    
    private static class ConstantImpl implements Constant {
        private int intValue = Integer.MIN_VALUE;
        private long longValue = Long.MIN_VALUE;
        private float floatValue = Float.NEGATIVE_INFINITY;
        private double doubleValue = Double.NEGATIVE_INFINITY;
        private String stringValue = "\u0000UNSET\u0000";
        private Class<?> classValue = Void.class;
        
        public ConstantImpl withInt(int value) {
            this.intValue = value;
            return this;
        }
        
        public ConstantImpl withLong(long value) {
            this.longValue = value;
            return this;
        }
        
        public ConstantImpl withFloat(float value) {
            this.floatValue = value;
            return this;
        }
        
        public ConstantImpl withDouble(double value) {
            this.doubleValue = value;
            return this;
        }
        
        public ConstantImpl withString(String value) {
            this.stringValue = value;
            return this;
        }
        
        public ConstantImpl withClass(Class<?> value) {
            this.classValue = value;
            return this;
        }
        
        @Override
        public int intValue() { return intValue; }
        
        @Override
        public long longValue() { return longValue; }
        
        @Override
        public float floatValue() { return floatValue; }
        
        @Override
        public double doubleValue() { return doubleValue; }
        
        @Override
        public String stringValue() { return stringValue; }
        
        @Override
        public Class<?> classValue() { return classValue; }
        
        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return Constant.class;
        }
    }
}