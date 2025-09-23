package com.tonic.injector.pipeline;

import com.tonic.injector.util.MethodUtil;
import com.tonic.util.dto.JClass;
import com.tonic.injector.util.MappingProvider;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shift;
import com.tonic.injector.types.InstructionMatcher;
import com.tonic.injector.util.AnnotationUtil;
import com.tonic.injector.util.TransformerUtil;
import com.tonic.util.ReflectBuilder;
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
        try {
            String gamepackName = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
            String targetMethodName = AnnotationUtil.getAnnotation(method, Insert.class, "method");
            String targetMethodDesc = AnnotationUtil.getAnnotation(method, Insert.class, "desc");

            JClass jClass = MappingProvider.getClass(gamepackName);
            ClassNode gamepackClass = TransformerUtil.getMethodClass(mixin, targetMethodName);

            Insert insertAnnotation = getInsertAnnotation(method);
            if (insertAnnotation == null) {
                System.err.println("No @Insert annotation found on method: " + method.name);
                return;
            }

            if(!insertAnnotation.raw())
                InjectTransformer.patch(gamepackClass, mixin, method);

            MethodNode targetMethod = TransformerUtil.getTargetMethod(mixin, targetMethodName, targetMethodDesc);

            if(targetMethod == null) {
                System.err.println("Target method not found: " + targetMethodName + " in class " + gamepackClass.name);
                return;
            }
            
            List<InstructionMatcher.MatchResult> matches = InstructionMatcher.findMatches(
                    targetMethod, 
                    insertAnnotation.at(), 
                    insertAnnotation.slice(),
                    jClass
            );

            matches = filterMatchesByContext(matches, insertAnnotation.at(), gamepackClass);
            
            if (matches.isEmpty()) {
                System.err.println("No matching instructions found for pattern in method: " + targetMethod.name + " :: " + targetMethodName + targetMethod.desc);
                return;
            }

            List<InstructionMatcher.MatchResult> targetMatches;
            if (insertAnnotation.all()) {
                targetMatches = matches;
            } else {
                int ordinal = insertAnnotation.ordinal();
                if (ordinal == -1) {
                    if (!matches.isEmpty()) {
                        targetMatches = List.of(matches.get(matches.size() - 1));
                    } else {
                        return;
                    }
                } else if (ordinal >= 0 && ordinal < matches.size()) {
                    targetMatches = List.of(matches.get(ordinal));
                } else {
                    return;
                }
            }

            if(insertAnnotation.raw())
            {
                String name = mixin.name.replace("/", ".");
                Type type = Type.getMethodType(method.desc);
                Type[] params = type.getArgumentTypes();
                if(params.length > 2)
                {
                    Class<?> clazz = InsertTransformer.class.getClassLoader()
                            .loadClass(name);

                    ReflectBuilder.of(clazz)
                            .staticMethod(
                                    method.name,
                                    new Class[]{ClassNode.class, MethodNode.class, AbstractInsnNode.class},
                                    new Object[]{gamepackClass, targetMethod, targetMatches.get(0).instruction}
                            )
                            .get();
                }
                else
                {
                    Class<?> clazz = InsertTransformer.class.getClassLoader()
                            .loadClass(name);

                    ReflectBuilder.of(clazz)
                            .staticMethod(
                                    method.name,
                                    new Class[]{MethodNode.class, AbstractInsnNode.class},
                                    new Object[]{targetMethod, targetMatches.get(0).instruction}
                            )
                            .get();
                }
                System.out.println("Successfully invoked raw insert method: " + method.name + " on " + targetMethod.name);
                return;
            }
            
            for (int i = targetMatches.size() - 1; i >= 0; i--) {
                InstructionMatcher.MatchResult match = targetMatches.get(i);
                insertMethodCall(targetMethod, match.instruction, gamepackClass, method, insertAnnotation.at());
            }
            
            System.out.println("Successfully inserted " + targetMatches.size() + " call(s) to " + method.name + " in " + targetMethod.name);
            
        } catch (Exception e) {
            System.err.println("Error processing @Insert on method " + method.name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Extracts Insert annotation from method.
     * @param method method to check
     * @return Insert annotation or null
     */
    private static Insert getInsertAnnotation(MethodNode method) {
        if (method.visibleAnnotations == null) return null;
        
        for (AnnotationNode annotation : method.visibleAnnotations) {
            if (annotation.desc.equals("L" + Insert.class.getName().replace('.', '/') + ";")) {
                return AnnotationUtil.createAnnotationProxy(Insert.class, annotation);
            }
        }
        return null;
    }
    
    /**
     * Inserts method call at specified instruction point.
     * @param targetMethod method to inject into
     * @param insertPoint instruction to insert after
     * @param gamepackClass class containing injected method
     * @param injectedMethod method to inject
     * @param pattern injection pattern
     */
    private static void insertMethodCall(MethodNode targetMethod, AbstractInsnNode insertPoint, 
                                       ClassNode gamepackClass, MethodNode injectedMethod, At pattern) {
        
        InsnList callInstructions = MethodUtil.generateContextAwareInvoke(gamepackClass, targetMethod, injectedMethod, true);
        
        Shift shiftType = getShiftType(pattern);
        if (shiftType == Shift.HEAD) {
            targetMethod.instructions.insertBefore(insertPoint, callInstructions);
        } else {
            targetMethod.instructions.insert(insertPoint, callInstructions);
        }
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
        } catch (Exception e) {
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