package com.tonic.plugins.codeeval.completion;

import lombok.Getter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Holds information about a field for autocompletion
 */
@Getter
public class FieldInfo {
    private final String name;
    private final String type;
    private final boolean isStatic;
    private final boolean isFinal;
    private final String shortDescription;

    public FieldInfo(Field field) {
        this.name = field.getName();
        this.type = simplifyTypeName(field.getType().getName());
        this.isStatic = Modifier.isStatic(field.getModifiers());
        this.isFinal = Modifier.isFinal(field.getModifiers());
        this.shortDescription = buildShortDescription();
    }

    private String buildShortDescription() {
        StringBuilder sb = new StringBuilder();
        if (isStatic) sb.append("static ");
        if (isFinal) sb.append("final ");
        sb.append(type).append(" ").append(name);
        return sb.toString();
    }

    private static String simplifyTypeName(String typeName) {
        if (typeName == null) return "Object";

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
