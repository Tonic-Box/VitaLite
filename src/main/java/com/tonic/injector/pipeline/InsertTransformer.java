package com.tonic.injector.pipeline;

import com.tonic.injector.util.MethodUtil;
import com.tonic.util.dto.JClass;
import com.tonic.util.MappingProvider;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shift;
import com.tonic.injector.types.InstructionMatcher;
import com.tonic.injector.util.AnnotationUtil;
import com.tonic.injector.util.Gates;
import com.tonic.injector.util.InjectionFailures;
import com.tonic.injector.util.TransformerUtil;
import com.tonic.util.ReflectBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Transforms insert annotations to inject method calls at specified locations.
 */
public class InsertTransformer {
    
    /**
     * Processes @Insert annotated method to inject calls.
     * @param mixin mixin class containing insert method
     * @param method method annotated with @Insert
     */
    public static void patch(ClassNode mixin, MethodNode method) {
        String hook = "@Insert " + mixin.name + "." + method.name;
        try {
            String gamepackName = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
            String targetMethodName = AnnotationUtil.getAnnotation(method, Insert.class, "method");
            String targetMethodDesc = AnnotationUtil.getAnnotation(method, Insert.class, "desc");
            boolean gamepack = !gamepackName.contains("/");

            JClass jClass = MappingProvider.getClass(gamepackName);
            ClassNode gamepackClass = TransformerUtil.getMethodClass(mixin, targetMethodName);

            Insert insertAnnotation = AnnotationUtil.annotation(method, Insert.class);

            if(!insertAnnotation.raw())
                InjectTransformer.patch(gamepackClass, mixin, method);

            MethodNode targetMethod = TransformerUtil.requireTargetMethod(hook, mixin, targetMethodName, targetMethodDesc);
            if(targetMethod == null) {
                return;
            }

            List<InstructionMatcher.MatchResult> matches = InstructionMatcher.findMatches(
                    targetMethod,
                    insertAnnotation.at(),
                    insertAnnotation.slice(),
                    jClass,
                    gamepack
            );

            matches = filterMatchesByContext(matches, insertAnnotation.at(), gamepackClass);

            List<InstructionMatcher.MatchResult> targetMatches = TransformerUtil.select(hook + " in " + targetMethod.name + targetMethod.desc, matches, insertAnnotation.expectedMatches(), insertAnnotation.all(), insertAnnotation.ordinal());
            if (targetMatches == null) {
                return;
            }

            if(insertAnnotation.raw())
            {
                Class<?> clazz = InsertTransformer.class.getClassLoader().loadClass(mixin.name.replace("/", "."));
                boolean withClass = Type.getArgumentTypes(method.desc).length > 2;
                for (int i = targetMatches.size() - 1; i >= 0; i--) {
                    AbstractInsnNode point = targetMatches.get(i).instruction;
                    if (withClass) {
                        ReflectBuilder.of(clazz).staticMethod(method.name, new Class[]{ClassNode.class, MethodNode.class, AbstractInsnNode.class}, new Object[]{gamepackClass, targetMethod, point}).get();
                    } else {
                        ReflectBuilder.of(clazz).staticMethod(method.name, new Class[]{MethodNode.class, AbstractInsnNode.class}, new Object[]{targetMethod, point}).get();
                    }
                }
                System.out.println("Successfully invoked raw insert method: " + method.name + " on " + targetMethod.name);
                return;
            }

            for (int i = targetMatches.size() - 1; i >= 0; i--) {
                InstructionMatcher.MatchResult match = targetMatches.get(i);
                insertMethodCall(targetMethod, match.instruction, gamepackClass, method, insertAnnotation);
            }

            System.out.println("Successfully inserted " + targetMatches.size() + " call(s) to " + method.name + " in " + targetMethod.name);

        } catch (Exception | LinkageError e) {
            InjectionFailures.record(hook + ": " + e);
            e.printStackTrace();
        }
    }

    /**
     * Inserts method call at specified instruction point, always before one that leaves the method.
     * @param targetMethod method to inject into
     * @param insertPoint instruction to insert at
     * @param gamepackClass class containing injected method
     * @param injectedMethod method to inject
     * @param insert the insert's annotation
     */
    private static void insertMethodCall(MethodNode targetMethod, AbstractInsnNode insertPoint, ClassNode gamepackClass, MethodNode injectedMethod, Insert insert) {
        At pattern = insert.at();
        int parameter = insert.storeIntoParameter();
        InsnList call = MethodUtil.generateContextAwareInvoke(gamepackClass, targetMethod, injectedMethod, parameter < 0);
        if (parameter >= 0) {
            call.add(storeIntoParameter(targetMethod, injectedMethod, parameter));
        }
        InsnList code = Gates.around(insert.gate(), call);

        int opcode = insertPoint.getOpcode();
        boolean exits = (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW;
        if (getTargetTypeString(pattern).equals("HEAD") || getShiftType(pattern) == Shift.HEAD || exits) {
            targetMethod.instructions.insertBefore(insertPoint, code);
        } else {
            targetMethod.instructions.insert(insertPoint, code);
        }
    }

    private static InsnList storeIntoParameter(MethodNode targetMethod, MethodNode injectedMethod, int parameter) {
        Type[] parameters = Type.getArgumentTypes(targetMethod.desc);
        if (parameter >= parameters.length) {
            throw new IllegalArgumentException("storeIntoParameter " + parameter + " but " + targetMethod.name + targetMethod.desc + " has " + parameters.length + " parameters");
        }
        Type returned = Type.getReturnType(injectedMethod.desc);
        Type type = parameters[parameter];
        if (returned.getSort() != type.getSort()) {
            throw new IllegalArgumentException("the hook returns " + returned + " but parameter " + parameter + " of " + targetMethod.name + targetMethod.desc + " is " + type);
        }
        int local = (targetMethod.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
        for (int i = 0; i < parameter; i++) {
            local += parameters[i].getSize();
        }
        InsnList store = new InsnList();
        if (type.getSort() == Type.OBJECT && !returned.equals(type)) {
            store.add(new TypeInsnNode(Opcodes.CHECKCAST, type.getInternalName()));
        }
        store.add(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), local));
        return store;
    }

    /**
     * Gets shift type from pattern.
     * @param pattern pattern specification
     * @return shift type
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
        }
    }
    
    /**
     * Gets target type string from pattern.
     * @param pattern pattern specification
     * @return target type string
     */
    private static String getTargetTypeString(At pattern) {
        try {
            return pattern.value().name();
        } catch (ClassCastException e) {
            try {
                Object valueObj = getRawAnnotationValue(pattern, "value");
                if (valueObj instanceof String) {
                    return ((String) valueObj).toUpperCase();
                }
            } catch (Exception ex) {
                System.err.println("Error getting target type: " + ex.getMessage());
            }
            return "INVOKE";
        }
    }
    
    /**
     * Gets raw annotation value for transition period.
     * @param proxy annotation proxy
     * @param methodName method name
     * @return raw value
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
     * Filters matches by context to avoid field name ambiguity.
     * @param matches instruction matches
     * @param pattern injection pattern
     * @param targetClass target class
     * @return filtered matches
     */
    private static List<InstructionMatcher.MatchResult> filterMatchesByContext(
            List<InstructionMatcher.MatchResult> matches, 
            At pattern, 
            ClassNode targetClass) {
        
        String value = getTargetTypeString(pattern);
        String target = pattern.target();
        
        if (target.contains(".")) {
            return matches;
        }
        
        if ((value.equals("GETFIELD") || value.equals("PUTFIELD") || 
             value.equals("GETSTATIC") || value.equals("PUTSTATIC")) &&
            pattern.owner().isEmpty()) {
            
            return matches.stream()
                    .filter(match -> {
                        if (match.instruction instanceof FieldInsnNode) {
                            FieldInsnNode fieldInsn = (FieldInsnNode) match.instruction;
                            
                            return fieldInsn.owner.equals(targetClass.name) ||
                                   isInheritedField(fieldInsn.owner, targetClass);
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }
        
        return matches;
    }
    
    /**
     * Checks if field owner is inherited by target class.
     * @param fieldOwner field owner class
     * @param targetClass target class
     * @return true if inherited
     */
    private static boolean isInheritedField(String fieldOwner, ClassNode targetClass) {
        if (targetClass.superName != null && targetClass.superName.equals(fieldOwner)) {
            return true;
        }
        
        if (targetClass.interfaces != null) {
            return targetClass.interfaces.contains(fieldOwner);
        }
        
        return false;
    }
}