package com.tonic.injector.types;

import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JField;
import com.tonic.util.dto.JMethod;
import com.tonic.injector.util.MappingProvider;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Constant;
import com.tonic.injector.annotations.Shift;
import com.tonic.injector.annotations.Slice;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Matches instruction patterns within method bytecode based on @At annotations.
 */
public class InstructionMatcher {
    
    public static class MatchResult {
        public final AbstractInsnNode instruction;
        public final int index;
        
        public MatchResult(AbstractInsnNode instruction, int index) {
            this.instruction = instruction;
            this.index = index;
        }
    }
    
    /**
     * Find all instructions matching the given pattern.
     */
    public static List<MatchResult> findMatches(MethodNode method, At pattern, Slice slice) {
        return findMatches(method, pattern, slice, null);
    }
    
    /**
     * Find all instructions matching the given pattern with mapping context.
     */
    public static List<MatchResult> findMatches(MethodNode method, At pattern, Slice slice, JClass mappingContext) {
        List<MatchResult> matches = new ArrayList<>();
        
        AbstractInsnNode start = getSliceStart(method, slice, mappingContext);
        AbstractInsnNode end = getSliceEnd(method, slice, mappingContext);
        
        AbstractInsnNode current = start;
        int index = 0;
        
        while (current != null && (end == null || current != end.getNext())) {
            if (matchesPattern(current, pattern, mappingContext)) {
                AbstractInsnNode targetInsn = applyShift(current, pattern);
                matches.add(new MatchResult(targetInsn, index));
            }
            current = current.getNext();
            index++;
        }
        
        return matches;
    }
    
    private static AbstractInsnNode getSliceStart(MethodNode method, Slice slice, JClass mappingContext) {
        AtTarget fromTarget = getTargetType(slice.from());
        if (fromTarget == AtTarget.NONE) {
            return method.instructions.getFirst();
        }
        
        List<MatchResult> fromMatches = findMatches(method, slice.from(), new SliceImpl(), mappingContext);
        return fromMatches.isEmpty() ? method.instructions.getFirst() : fromMatches.get(0).instruction;
    }
    
    private static AbstractInsnNode getSliceEnd(MethodNode method, Slice slice, JClass mappingContext) {
        AtTarget toTarget = getTargetType(slice.to());
        if (toTarget == AtTarget.NONE) {
            return method.instructions.getLast();
        }
        
        List<MatchResult> toMatches = findMatches(method, slice.to(), new SliceImpl(), mappingContext);
        return toMatches.isEmpty() ? method.instructions.getLast() : toMatches.get(0).instruction;
    }
    
    private static boolean matchesPattern(AbstractInsnNode insn, At pattern, JClass mappingContext) {
        AtTarget targetType = getTargetType(pattern);
        
        switch (targetType) {
            case INVOKE:
                return matchesInvoke(insn, pattern.target(), pattern.owner(), mappingContext);
            case GETFIELD:
                return matchesFieldAccess(insn, pattern.target(), pattern.owner(), Opcodes.GETFIELD, mappingContext);
            case PUTFIELD:
                return matchesFieldAccess(insn, pattern.target(), pattern.owner(), Opcodes.PUTFIELD, mappingContext);
            case GETSTATIC:
                return matchesFieldAccess(insn, pattern.target(), pattern.owner(), Opcodes.GETSTATIC, mappingContext);
            case PUTSTATIC:
                return matchesFieldAccess(insn, pattern.target(), pattern.owner(), Opcodes.PUTSTATIC, mappingContext);
            case NEW:
                return matchesNew(insn, pattern.target(), pattern.owner());
            case LDC:
                return matchesLdc(insn, pattern.constant());
            case OPCODE:
                return matchesOpcode(insn, pattern.opcode());
            case LINE:
                return matchesLine(insn, pattern.line());
            case RETURN:
                return matchesReturn(insn);
            case JUMP:
                return matchesJump(insn);
            case LOAD:
                return matchesLocalLoad(insn, getLocalIndex(pattern));
            case STORE:
                return matchesLocalStore(insn, getLocalIndex(pattern));
            case IINC:
                return matchesIinc(insn, getLocalIndex(pattern));
            case ARRAY_LOAD:
                return matchesArrayLoad(insn);
            case ARRAY_STORE:
                return matchesArrayStore(insn);
            case CHECKCAST:
                return matchesCheckcast(insn, pattern.owner(), mappingContext);
            default:
                return false;
        }
    }
    
    /**
     * Get the target type, supporting both enum and legacy string values.
     */
    private static AtTarget getTargetType(At pattern) {
        try {
            java.lang.reflect.InvocationHandler handler = java.lang.reflect.Proxy.getInvocationHandler(pattern);
            java.lang.reflect.Method valueMethod = At.class.getMethod("value");
            Object valueObj = handler.invoke(pattern, valueMethod, null);
            
            if (valueObj instanceof String) {
                return AtTarget.valueOf(((String) valueObj).toUpperCase());
            } else if (valueObj instanceof AtTarget) {
                return (AtTarget) valueObj;
            }
        } catch (Exception e) {
            try {
                Object valueObj = getRawAnnotationValue(pattern, "value");
                if (valueObj instanceof String) {
                    return AtTarget.valueOf(((String) valueObj).toUpperCase());
                }
            } catch (Exception e2) {
                System.err.println("Error getting target type: " + e.getMessage());
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return AtTarget.INVOKE;
    }
    
    /**
     * Attempt to get raw annotation value without triggering proxy casting
     */
    private static Object getRawAnnotationValue(Object proxy, String methodName) {
        try {
            if (java.lang.reflect.Proxy.isProxyClass(proxy.getClass())) {
                java.lang.reflect.InvocationHandler handler = java.lang.reflect.Proxy.getInvocationHandler(proxy);
                java.lang.reflect.Field[] fields = handler.getClass().getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    Object value = field.get(handler);
                    if (value instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> map = (java.util.Map<String, Object>) value;
                        return map.get(methodName);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    
    /**
     * Safely get local index from At pattern, handling potential proxy issues
     */
    private static int getLocalIndex(At pattern) {
        try {
            return pattern.local();
        } catch (Exception e) {
            try {
                Object localObj = getRawAnnotationValue(pattern, "local");
                if (localObj instanceof Integer) {
                    return (Integer) localObj;
                }
            } catch (Exception ex) {
                System.err.println("Error getting local index: " + ex.getMessage());
            }
            return -1;
        }
    }
    
    private static boolean matchesInvoke(AbstractInsnNode insn, String target, String owner, JClass mappingContext) {
        if (!(insn instanceof MethodInsnNode)) return false;
        
        MethodInsnNode methodInsn = (MethodInsnNode) insn;
        
        if (target.isEmpty()) return true;
        
        if (!owner.isEmpty()) {
            String resolvedOwner = resolveClassName(owner);
            if (!methodInsn.owner.equals(resolvedOwner) && !methodInsn.owner.endsWith("/" + resolvedOwner)) {
                return false;
            }
        }
        
        if (target.contains(".") && owner.isEmpty()) {
            String[] parts = target.split("\\.", 2);
            String className = parts[0];
            String methodPart = parts[1];
            
            String resolvedClassName = resolveClassName(className);
            
            if (!methodInsn.owner.equals(resolvedClassName) && !methodInsn.owner.endsWith("/" + resolvedClassName)) {
                return false;
            }
            target = methodPart;
        }
        
        int parenIndex = target.indexOf('(');
        String methodName;
        String descriptor = null;
        
        if (parenIndex == -1) {
            methodName = target;
        } else {
            methodName = target.substring(0, parenIndex);
            descriptor = target.substring(parenIndex);
        }
        
        JClass ownerContext = owner.isEmpty() ? mappingContext : MappingProvider.getClass(owner);
        String resolvedMethodName = resolveMethodName(ownerContext, methodName, descriptor);
        
        if (descriptor != null) {
            return methodInsn.name.equals(resolvedMethodName) && methodInsn.desc.equals(descriptor);
        } else {
            return methodInsn.name.equals(resolvedMethodName);
        }
    }
    
    private static boolean matchesFieldAccess(AbstractInsnNode insn, String target, String owner, int expectedOpcode, JClass mappingContext) {
        if (!(insn instanceof FieldInsnNode) || insn.getOpcode() != expectedOpcode) return false;
        
        FieldInsnNode fieldInsn = (FieldInsnNode) insn;

        if (target.isEmpty()) return true;
        
        String resolvedOwner;
        if (!owner.isEmpty()) {
            resolvedOwner = resolveClassName(owner);
            if (!fieldInsn.owner.equals(resolvedOwner)) {
                return false;
            }
        }
        
        JClass fieldContext = owner.isEmpty() ? mappingContext : MappingProvider.getClass(owner);
        String resolvedFieldName = resolveFieldName(fieldContext, target);

        return fieldInsn.name.equals(resolvedFieldName);
    }
    
    private static boolean matchesNew(AbstractInsnNode insn, String target, String owner) {
        if (!(insn instanceof TypeInsnNode) || insn.getOpcode() != Opcodes.NEW) return false;
        
        TypeInsnNode typeInsn = (TypeInsnNode) insn;
        
        String className = owner.isEmpty() ? target : owner;
        
        if (className.isEmpty()) return true;
        
        String resolvedClassName = resolveClassName(className);
        return typeInsn.desc.equals(resolvedClassName);
    }
    
    private static boolean matchesLdc(AbstractInsnNode insn, Constant constant) {
        if (!(insn instanceof LdcInsnNode)) return false;
        
        LdcInsnNode ldcInsn = (LdcInsnNode) insn;
        Object cst = ldcInsn.cst;
        
        if (constant.intValue() != Integer.MIN_VALUE && cst instanceof Integer) {
            return cst.equals(constant.intValue());
        }
        if (constant.longValue() != Long.MIN_VALUE && cst instanceof Long) {
            return cst.equals(constant.longValue());
        }
        if (constant.floatValue() != Float.NEGATIVE_INFINITY && cst instanceof Float) {
            return cst.equals(constant.floatValue());
        }
        if (constant.doubleValue() != Double.NEGATIVE_INFINITY && cst instanceof Double) {
            return cst.equals(constant.doubleValue());
        }
        if (!constant.stringValue().equals("\u0000UNSET\u0000") && cst instanceof String) {
            return cst.equals(constant.stringValue());
        }
        if (constant.classValue() != Void.class && cst instanceof org.objectweb.asm.Type) {
            return ((org.objectweb.asm.Type) cst).getClassName().equals(constant.classValue().getName());
        }
        
        return true;
    }
    
    private static boolean matchesOpcode(AbstractInsnNode insn, String opcodeName) {
        if (opcodeName.isEmpty()) return false;
        
        try {
            int expectedOpcode = (Integer) Opcodes.class.getField(opcodeName.toUpperCase()).get(null);
            return insn.getOpcode() == expectedOpcode;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean matchesLine(AbstractInsnNode insn, int lineNumber) {
        if (!(insn instanceof LineNumberNode) || lineNumber == -1) return false;
        
        LineNumberNode lineInsn = (LineNumberNode) insn;
        return lineInsn.line == lineNumber;
    }
    
    private static boolean matchesReturn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return opcode == Opcodes.RETURN || opcode == Opcodes.IRETURN || 
               opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN ||
               opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN;
    }
    
    private static boolean matchesJump(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return opcode == Opcodes.GOTO || opcode == Opcodes.JSR ||
               (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IF_ACMPNE);
    }
    
    private static boolean matchesLocalLoad(AbstractInsnNode insn, int localIndex) {
        if (!(insn instanceof VarInsnNode)) return false;
        
        VarInsnNode varInsn = (VarInsnNode) insn;
        int opcode = insn.getOpcode();

        if (localIndex == -1) {
            return opcode == Opcodes.ILOAD || opcode == Opcodes.LLOAD ||
                   opcode == Opcodes.FLOAD || opcode == Opcodes.DLOAD ||
                   opcode == Opcodes.ALOAD;
        }

        boolean isLoadOpcode = opcode == Opcodes.ILOAD || opcode == Opcodes.LLOAD ||
                              opcode == Opcodes.FLOAD || opcode == Opcodes.DLOAD ||
                              opcode == Opcodes.ALOAD;
        
        return isLoadOpcode && varInsn.var == localIndex;
    }
    
    private static boolean matchesLocalStore(AbstractInsnNode insn, int localIndex) {
        if (!(insn instanceof VarInsnNode)) return false;
        
        VarInsnNode varInsn = (VarInsnNode) insn;
        int opcode = insn.getOpcode();

        if (localIndex == -1) {
            return opcode == Opcodes.ISTORE || opcode == Opcodes.LSTORE ||
                   opcode == Opcodes.FSTORE || opcode == Opcodes.DSTORE ||
                   opcode == Opcodes.ASTORE;
        }

        boolean isStoreOpcode = opcode == Opcodes.ISTORE || opcode == Opcodes.LSTORE ||
                               opcode == Opcodes.FSTORE || opcode == Opcodes.DSTORE ||
                               opcode == Opcodes.ASTORE;
        
        return isStoreOpcode && varInsn.var == localIndex;
    }
    
    private static boolean matchesIinc(AbstractInsnNode insn, int localIndex) {
        if (!(insn instanceof IincInsnNode) || insn.getOpcode() != Opcodes.IINC) return false;
        
        IincInsnNode iincInsn = (IincInsnNode) insn;

        if (localIndex == -1) {
            return true;
        }

        return iincInsn.var == localIndex;
    }
    
    private static boolean matchesArrayLoad(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return opcode == Opcodes.IALOAD || opcode == Opcodes.LALOAD ||
               opcode == Opcodes.FALOAD || opcode == Opcodes.DALOAD ||
               opcode == Opcodes.AALOAD || opcode == Opcodes.BALOAD ||
               opcode == Opcodes.CALOAD || opcode == Opcodes.SALOAD;
    }
    
    private static boolean matchesArrayStore(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return opcode == Opcodes.IASTORE || opcode == Opcodes.LASTORE ||
               opcode == Opcodes.FASTORE || opcode == Opcodes.DASTORE ||
               opcode == Opcodes.AASTORE || opcode == Opcodes.BASTORE ||
               opcode == Opcodes.CASTORE || opcode == Opcodes.SASTORE;
    }
    
    private static boolean matchesCheckcast(AbstractInsnNode insn, String owner, JClass mappingContext) {
        if (!(insn instanceof TypeInsnNode) || insn.getOpcode() != Opcodes.CHECKCAST) return false;
        TypeInsnNode typeInsn = (TypeInsnNode) insn;
        if (owner.isEmpty()) return true;
        String resolvedClassName = resolveClassName(owner);
        return typeInsn.desc.equals(resolvedClassName);
    }
    
    private static AbstractInsnNode applyShift(AbstractInsnNode insn, At pattern) {
        Shift shiftType = getShiftType(pattern);

        if (shiftType == Shift.TAIL) {
            return insn;
        }
        return insn.getPrevious();
    }
    
    /**
     * Get the shift type, supporting both enum and legacy string values.
     */
    private static Shift getShiftType(At pattern) {
        try {
            return pattern.shift();
        } catch (ClassCastException e) {
            try {
                Object shiftObj = getRawAnnotationValue(pattern, "shift");
                if (shiftObj instanceof String) {
                    String shiftStr = (String) shiftObj;
                    return Shift.valueOf(shiftStr.toUpperCase());
                }
            } catch (Exception ex) {
                System.err.println("Error getting shift type: " + ex.getMessage());
            }
            // Default fallback
            return Shift.TAIL;
        } catch (Exception e) {
            System.err.println("Error getting shift type: " + e.getMessage());
            e.printStackTrace();
        }

        return Shift.TAIL;
    }
    
    private static class SliceImpl implements Slice {
        @Override
        public At from() { return new AtImpl(); }
        @Override
        public At to() { return new AtImpl(); }
        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() { return Slice.class; }
    }
    
    private static class AtImpl implements At {
        @Override
        public AtTarget value() { return AtTarget.NONE; }
        @Override
        public String target() { return ""; }
        @Override
        public String owner() { return ""; }
        @Override
        public String opcode() { return ""; }
        @Override
        public int line() { return -1; }
        @Override
        public int local() { return -1; }
        @Override
        public Constant constant() { return new ConstantImpl(); }
        @Override
        public Shift shift() { return Shift.TAIL; }
        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() { return At.class; }
    }
    
    private static class ConstantImpl implements Constant {
        @Override
        public int intValue() { return Integer.MIN_VALUE; }
        @Override
        public long longValue() { return Long.MIN_VALUE; }
        @Override
        public float floatValue() { return Float.NEGATIVE_INFINITY; }
        @Override
        public double doubleValue() { return Double.NEGATIVE_INFINITY; }
        @Override
        public String stringValue() { return "\u0000UNSET\u0000"; }
        @Override
        public Class<?> classValue() { return Void.class; }
        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() { return Constant.class; }
    }
    
    /**
     * Resolve class name through mappings, fallback to literal if no mapping exists.
     */
    private static String resolveClassName(String mappedName) {
        JClass jClass = MappingProvider.getClass(mappedName);
        if (jClass != null && jClass.getObfuscatedName() != null) {
            return jClass.getObfuscatedName();
        }
        return mappedName.replace(".", "/");
    }
    
    /**
     * Resolve field name through mappings using context class, fallback to literal if no mapping exists.
     */
    private static String resolveFieldName(JClass mappingContext, String fieldName) {
        if (mappingContext != null) {
            JField jField = MappingProvider.getField(mappingContext, fieldName);
            if (jField != null && jField.getObfuscatedName() != null) {
                return jField.getObfuscatedName();
            }
        }
        
        JField globalField = findFieldInAnyClass(fieldName);
        if (globalField != null && globalField.getObfuscatedName() != null) {
            return globalField.getObfuscatedName();
        }
        
        return fieldName;
    }
    
    /**
     * Resolve method name through mappings using context class, fallback to literal if no mapping exists.
     */
    private static String resolveMethodName(JClass mappingContext, String methodName, String descriptor) {
        if (mappingContext != null) {
            JMethod jMethod = MappingProvider.getMethod(mappingContext, methodName);
            if (jMethod != null && jMethod.getObfuscatedName() != null) {
                if (descriptor == null || jMethod.getDescriptor().equals(descriptor)) {
                    return jMethod.getObfuscatedName();
                }
            }
        }
        return methodName;
    }
    
    /**
     * Find a field by name in any class - used for inheritance support.
     * This searches all classes for a field with the given name.
     */
    private static JField findFieldInAnyClass(String fieldName) {
        for (JClass jClass : MappingProvider.getMappings()) {
            for (JField jField : jClass.getFields()) {
                if (jField.getName() != null && jField.getName().equals(fieldName)) {
                    return jField;
                }
            }
        }
        return null;
    }
}