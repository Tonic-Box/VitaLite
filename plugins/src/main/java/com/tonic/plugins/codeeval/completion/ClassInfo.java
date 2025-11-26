package com.tonic.plugins.codeeval.completion;

import lombok.Getter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Caches information about a class for autocompletion
 */
@Getter
public class ClassInfo {
    private final String fullName;
    private final String simpleName;
    private final List<MethodInfo> methods;
    private final List<MethodInfo> staticMethods;
    private final List<FieldInfo> fields;
    private final List<FieldInfo> staticFields;
    private final boolean isInterface;
    private final boolean isEnum;

    public ClassInfo(Class<?> clazz) {
        this.fullName = clazz.getName();
        this.simpleName = clazz.getSimpleName();
        this.isInterface = clazz.isInterface();
        this.isEnum = clazz.isEnum();

        this.methods = new ArrayList<>();
        this.staticMethods = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.staticFields = new ArrayList<>();

        // Cache public methods
        for (Method method : clazz.getMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) continue;
            // Skip Object methods for cleaner completions
            if (method.getDeclaringClass() == Object.class) continue;

            MethodInfo methodInfo = new MethodInfo(method);
            if (methodInfo.isStatic()) {
                staticMethods.add(methodInfo);
            } else {
                methods.add(methodInfo);
            }
        }

        // Cache public fields
        for (Field field : clazz.getFields()) {
            if (!Modifier.isPublic(field.getModifiers())) continue;

            FieldInfo fieldInfo = new FieldInfo(field);
            if (fieldInfo.isStatic()) {
                staticFields.add(fieldInfo);
            } else {
                fields.add(fieldInfo);
            }
        }
    }

    /**
     * Gets all instance members (methods + fields)
     */
    public List<Object> getInstanceMembers() {
        List<Object> members = new ArrayList<>();
        members.addAll(methods);
        members.addAll(fields);
        return members;
    }

    /**
     * Gets all static members (methods + fields)
     */
    public List<Object> getStaticMembers() {
        List<Object> members = new ArrayList<>();
        members.addAll(staticMethods);
        members.addAll(staticFields);
        return members;
    }

    /**
     * Gets a method by name (first match)
     */
    public MethodInfo getMethod(String name) {
        for (MethodInfo method : methods) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        for (MethodInfo method : staticMethods) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Gets all methods matching a prefix
     */
    public List<MethodInfo> getMethodsMatching(String prefix) {
        List<MethodInfo> matching = new ArrayList<>();
        for (MethodInfo method : methods) {
            if (method.matchesPrefix(prefix)) {
                matching.add(method);
            }
        }
        for (MethodInfo method : staticMethods) {
            if (method.matchesPrefix(prefix)) {
                matching.add(method);
            }
        }
        return matching;
    }

    public boolean matchesPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return true;
        return simpleName.toLowerCase().startsWith(prefix.toLowerCase());
    }
}
