package com.tonic.injector.util;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationUtil {
    /**
     * Checks if a method has any visible annotations.
     * 
     * @param mn the method node to check
     * @return true if the method has any visible annotations, false otherwise
     */
    public static boolean hasAnyAnnotation(MethodNode mn) {
        return mn.visibleAnnotations != null && !mn.visibleAnnotations.isEmpty();
    }

    /**
     * Checks if a class has a specific annotation.
     * 
     * @param cn the class node to check
     * @param annotation the annotation class to look for
     * @return true if the class has the specified annotation, false otherwise
     */
    public static boolean hasAnnotation(ClassNode cn, Class<?> annotation) {
        if (cn.visibleAnnotations != null) {
            for (AnnotationNode an : cn.visibleAnnotations) {
                if (an.desc.equals(Type.getDescriptor(annotation))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a method has a specific annotation.
     * 
     * @param cn the method node to check
     * @param annotation the annotation class to look for
     * @return true if the method has the specified annotation, false otherwise
     */
    public static boolean hasAnnotation(MethodNode cn, Class<?> annotation) {
        if (cn.visibleAnnotations != null) {
            for (AnnotationNode an : cn.visibleAnnotations) {
                if (an.desc.equals(Type.getDescriptor(annotation))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a field has a specific annotation.
     * 
     * @param cn the field node to check
     * @param annotation the annotation class to look for
     * @return true if the field has the specified annotation, false otherwise
     */
    public static boolean hasAnnotation(FieldNode cn, Class<?> annotation) {
        if (cn.visibleAnnotations != null) {
            for (AnnotationNode an : cn.visibleAnnotations) {
                if (an.desc.equals(Type.getDescriptor(annotation))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets a specific value from an annotation on a class.
     * 
     * @param <T> the type of the annotation value
     * @param cn the class node to check
     * @param annotation the annotation class to look for
     * @param value the name of the annotation parameter
     * @return the annotation value, or null if not found
     */
    public static <T> T getAnnotation(ClassNode cn, Class<?> annotation, String value) {
        if (cn.visibleAnnotations != null) {
            for (AnnotationNode an : cn.visibleAnnotations) {
                if (an.desc.equals(Type.getDescriptor(annotation))) {
                    Map<String, Object> params = AnnotationUtil.parseAnnotationValues(an);
                    return (T) params.get(value);
                }
            }
        }
        return null;
    }

    /**
     * Gets a specific value from an annotation on a method.
     * 
     * @param <T> the type of the annotation value
     * @param cn the method node to check
     * @param annotation the annotation class to look for
     * @param value the name of the annotation parameter
     * @return the annotation value, or null if not found
     */
    public static <T> T getAnnotation(MethodNode cn, Class<?> annotation, String value) {
        if (cn.visibleAnnotations != null) {
            for (AnnotationNode an : cn.visibleAnnotations) {
                if (an.desc.equals(Type.getDescriptor(annotation))) {
                    Map<String, Object> params = AnnotationUtil.parseAnnotationValues(an);
                    return (T) params.get(value);
                }
            }
        }
        return null;
    }

    /**
     * Gets a specific value from an annotation on a field.
     * 
     * @param <T> the type of the annotation value
     * @param cn the field node to check
     * @param annotation the annotation class to look for
     * @param value the name of the annotation parameter
     * @return the annotation value, or null if not found
     */
    public static <T> T getAnnotation(FieldNode cn, Class<?> annotation, String value) {
        if (cn.visibleAnnotations != null) {
            for (AnnotationNode an : cn.visibleAnnotations) {
                if (an.desc.equals(Type.getDescriptor(annotation))) {
                    Map<String, Object> params = AnnotationUtil.parseAnnotationValues(an);
                    return (T) params.get(value);
                }
            }
        }
        return null;
    }

    /**
     * Parses annotation values into a map.
     */
    public static Map<String, Object> parseAnnotationValues(AnnotationNode annotation) {
        Map<String, Object> params = new HashMap<>();

        if (annotation.values != null) {
            for (int i = 0; i < annotation.values.size(); i += 2) {
                String name = (String) annotation.values.get(i);
                Object value = annotation.values.get(i + 1);
                params.put(name, parseValue(value));
            }
        }

        return params;
    }

    /**
     * Parses individual annotation values, handling different types.
     */
    private static Object parseValue(Object value) {
        if (value instanceof Type) {
            return ((Type) value).getClassName();
        } else if (value instanceof String[]) {
            String[] enumValue = (String[]) value;
            return enumValue.length > 1 ? enumValue[1] : enumValue[0];
        } else if (value instanceof AnnotationNode) {
            return value;
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<Object> parsedList = new ArrayList<>();
            for (Object item : list) {
                parsedList.add(parseValue(item));
            }
            return parsedList;
        }
        return value;
    }

    /**
     * Creates a proxy instance of an annotation interface from an AnnotationNode.
     * This allows runtime access to annotation values with proper type handling.
     */
    @SuppressWarnings("unchecked")
    public static <T> T createAnnotationProxy(Class<T> annotationType, AnnotationNode annotationNode) {
        Map<String, Object> values = parseAnnotationValues(annotationNode);

        return (T) java.lang.reflect.Proxy.newProxyInstance(
                annotationType.getClassLoader(),
                new Class<?>[]{annotationType},
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    if ("annotationType".equals(methodName)) {
                        return annotationType;
                    }

                    Object value = values.get(methodName);
                    if (value != null) {
                        if (value instanceof AnnotationNode) {
                            AnnotationNode nestedAnnotation = (AnnotationNode) value;
                            Class<?> returnType = method.getReturnType();
                            if (returnType.isAnnotation()) {
                                @SuppressWarnings("unchecked")
                                Class<Object> annotationClass = (Class<Object>) returnType;
                                return createAnnotationProxy(annotationClass, nestedAnnotation);
                            }
                        }
                        return value;
                    }

                    return method.getDefaultValue();
                }
        );
    }

    /**
     * Reads a method's annotation as an instance of its interface.
     *
     * @param method the annotated method
     * @param annotationType the annotation's type
     * @return the annotation
     * @throws IllegalStateException if the method does not carry it
     */
    public static <T> T annotation(MethodNode method, Class<T> annotationType) {
        String desc = Type.getDescriptor(annotationType);
        if (method.visibleAnnotations != null) {
            for (AnnotationNode annotation : method.visibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    return createAnnotationProxy(annotationType, annotation);
                }
            }
        }
        throw new IllegalStateException(method.name + " has no " + annotationType.getSimpleName() + " annotation");
    }
}
