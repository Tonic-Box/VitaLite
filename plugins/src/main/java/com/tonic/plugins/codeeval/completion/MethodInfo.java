package com.tonic.plugins.codeeval.completion;

import lombok.Getter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Holds information about a method for autocompletion
 */
@Getter
public class MethodInfo {
    private final String name;
    private final String returnType;           // Full qualified name for type resolution
    private final String returnTypeSimple;     // Simple name for display
    private final String[] parameterTypes;
    private final String[] parameterNames;
    private final boolean isStatic;
    private final String signature;
    private final String shortDescription;

    public MethodInfo(Method method) {
        this.name = method.getName();
        this.returnType = method.getReturnType().getName();  // Keep full name
        this.returnTypeSimple = simplifyTypeName(this.returnType);
        this.isStatic = Modifier.isStatic(method.getModifiers());

        Class<?>[] paramTypes = method.getParameterTypes();
        this.parameterTypes = new String[paramTypes.length];
        this.parameterNames = new String[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            this.parameterTypes[i] = simplifyTypeName(paramTypes[i].getName());
            this.parameterNames[i] = "arg" + i;
        }

        this.signature = buildSignature();
        this.shortDescription = buildShortDescription();
    }

    private String buildSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameterTypes[i]).append(" ").append(parameterNames[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildShortDescription() {
        StringBuilder sb = new StringBuilder();
        if (isStatic) sb.append("static ");
        sb.append(returnTypeSimple).append(" ").append(signature);
        return sb.toString();
    }

    /**
     * Simplifies fully qualified type names (e.g., java.lang.String -> String)
     */
    private static String simplifyTypeName(String typeName) {
        if (typeName == null) return "void";

        // Handle arrays
        if (typeName.startsWith("[")) {
            return parseArrayType(typeName);
        }

        // Handle primitives
        if (!typeName.contains(".")) {
            return typeName;
        }

        // Get simple name
        int lastDot = typeName.lastIndexOf('.');
        return typeName.substring(lastDot + 1);
    }

    private static String parseArrayType(String typeName) {
        int dims = 0;
        int i = 0;
        while (i < typeName.length() && typeName.charAt(i) == '[') {
            dims++;
            i++;
        }

        String baseType;
        char typeChar = typeName.charAt(i);
        switch (typeChar) {
            case 'Z': baseType = "boolean"; break;
            case 'B': baseType = "byte"; break;
            case 'C': baseType = "char"; break;
            case 'D': baseType = "double"; break;
            case 'F': baseType = "float"; break;
            case 'I': baseType = "int"; break;
            case 'J': baseType = "long"; break;
            case 'S': baseType = "short"; break;
            case 'L':
                // Object type: L<classname>;
                baseType = simplifyTypeName(typeName.substring(i + 1, typeName.length() - 1));
                break;
            default:
                baseType = typeName;
        }

        StringBuilder sb = new StringBuilder(baseType);
        for (int d = 0; d < dims; d++) {
            sb.append("[]");
        }
        return sb.toString();
    }

    public boolean matchesPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return true;
        return name.toLowerCase().startsWith(prefix.toLowerCase());
    }
}
