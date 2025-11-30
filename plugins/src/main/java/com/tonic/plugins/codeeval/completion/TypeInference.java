package com.tonic.plugins.codeeval.completion;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uses JavaParser to infer types for autocompletion, especially for method chains
 */
public class TypeInference {
    private final ClassCache classCache;
    private final ImportScanner importScanner;

    // Known template context (e.g., "out" -> "java.io.PrintStream")
    private final Map<String, String> templateContext = new HashMap<>();

    // Pattern to extract the expression before the dot
    private static final Pattern BEFORE_DOT = Pattern.compile("([\\w.()]+)\\.$");
    private static final Pattern METHOD_CALL = Pattern.compile("(\\w+)\\s*\\(");

    public TypeInference(ClassCache classCache, ImportScanner importScanner) {
        this.classCache = classCache;
        this.importScanner = importScanner;

        // Pre-populate known template context
        templateContext.put("out", "java.io.PrintStream");
    }

    /**
     * Resolves the type of an expression at the cursor position
     * Returns the fully qualified class name
     */
    public String resolveType(String code, int cursorPosition) {
        // Get text before cursor
        String textBefore = code.substring(0, Math.min(cursorPosition, code.length()));

        // Find the expression before the dot
        Matcher matcher = BEFORE_DOT.matcher(textBefore);
        if (!matcher.find()) {
            return null;
        }

        String expression = matcher.group(1);
        return resolveExpressionType(expression, code);
    }

    /**
     * Resolves the type of a given expression string
     */
    public String resolveExpressionType(String expression, String fullCode) {
        if (expression == null || expression.isEmpty()) return null;

        // Check template context first
        if (templateContext.containsKey(expression)) {
            return templateContext.get(expression);
        }

        // Try to resolve as a class name (static access like Static.getClient())
        ClassInfo classInfo = classCache.getBySimpleName(expression);
        if (classInfo != null) {
            return classInfo.getFullName();
        }

        // Try to resolve through imports
        String resolved = importScanner.resolveClassName(expression);
        if (resolved != null) {
            return resolved;
        }

        // Check if it's a method call chain
        if (expression.contains("(")) {
            return resolveMethodChain(expression, fullCode);
        }

        // Try to find variable declaration in code
        String varType = findVariableType(expression, fullCode);
        if (varType != null) {
            return resolveTypeName(varType);
        }

        return null;
    }

    /**
     * Resolves a method call chain like Static.getClient().getLocalPlayer()
     */
    private String resolveMethodChain(String expression, String fullCode) {
        // Split by dots, respecting parentheses
        String[] parts = splitMethodChain(expression);

        if (parts.length == 0) return null;

        String currentType = null;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            if (part.contains("(")) {
                // It's a method call
                String methodName = part.substring(0, part.indexOf('('));

                if (currentType == null) {
                    // First part - could be a static method on a class
                    // Check if previous part was a class name
                    continue;
                }

                // Get the return type of this method
                ClassInfo classInfo = classCache.get(currentType);
                if (classInfo != null) {
                    MethodInfo method = classInfo.getMethod(methodName);
                    if (method != null) {
                        String returnType = method.getReturnType();
                        currentType = returnType;
                        // Load the return type class on-demand
                        classCache.get(currentType);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                // It's a class or variable name
                if (currentType == null) {
                    // Try as class name first
                    ClassInfo classInfo = classCache.getBySimpleName(part);
                    if (classInfo != null) {
                        currentType = classInfo.getFullName();
                    } else {
                        // Try as variable
                        String varType = findVariableType(part, fullCode);
                        if (varType != null) {
                            currentType = resolveTypeName(varType);
                        } else {
                            // Check template context
                            currentType = templateContext.get(part);
                        }
                    }
                } else {
                    // Field access
                    ClassInfo classInfo = classCache.get(currentType);
                    if (classInfo != null) {
                        for (FieldInfo field : classInfo.getFields()) {
                            if (field.getName().equals(part)) {
                                currentType = resolveTypeName(field.getType());
                                break;
                            }
                        }
                    }
                }
            }
        }

        return currentType;
    }

    /**
     * Splits a method chain respecting parentheses
     */
    private String[] splitMethodChain(String expression) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;

        for (char c : expression.toCharArray()) {
            if (c == '(') {
                parenDepth++;
                current.append(c);
            } else if (c == ')') {
                parenDepth--;
                current.append(c);
            } else if (c == '.' && parenDepth == 0) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts.toArray(new String[0]);
    }

    /**
     * Finds the type of a variable from its declaration in the code
     */
    private String findVariableType(String varName, String fullCode) {
        try {
            // Wrap code to make it parseable
            String wrappedCode = wrapInClass(fullCode);
            CompilationUnit cu = StaticJavaParser.parse(wrappedCode);

            // Find variable declarations
            return cu.findAll(VariableDeclarator.class).stream()
                    .filter(vd -> vd.getNameAsString().equals(varName))
                    .findFirst()
                    .map(vd -> vd.getType().asString())
                    .orElse(null);

        } catch (Exception e) {
            // Fallback to regex-based parsing
            return findVariableTypeRegex(varName, fullCode);
        }
    }

    /**
     * Fallback regex-based variable type finding
     */
    private String findVariableTypeRegex(String varName, String code) {
        // Pattern: Type varName = ...
        Pattern pattern = Pattern.compile("(\\w+(?:<[^>]+>)?)\\s+" + Pattern.quote(varName) + "\\s*[=;]");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Wraps user code in a class structure for parsing
     */
    private String wrapInClass(String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("public class Temp {\n");
        sb.append("  public void run() {\n");
        sb.append(code);
        sb.append("\n  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Resolves a type name to its fully qualified form
     */
    public String resolveTypeName(String typeName) {
        if (typeName == null) return null;

        // Handle generic types - extract base type
        if (typeName.contains("<")) {
            typeName = typeName.substring(0, typeName.indexOf('<'));
        }

        // Handle arrays
        String arraySuffix = "";
        while (typeName.endsWith("[]")) {
            arraySuffix += "[]";
            typeName = typeName.substring(0, typeName.length() - 2);
        }

        // Primitives
        if (isPrimitive(typeName)) {
            return typeName + arraySuffix;
        }

        // Already fully qualified? Verify it exists
        if (typeName.contains(".")) {
            ClassInfo info = classCache.get(typeName);
            if (info != null) {
                return info.getFullName() + arraySuffix;
            }
            return typeName + arraySuffix;
        }

        // Try java.lang
        try {
            Class.forName("java.lang." + typeName);
            return "java.lang." + typeName + arraySuffix;
        } catch (ClassNotFoundException ignored) {}

        // Try imports
        String resolved = importScanner.resolveClassName(typeName);
        if (resolved != null) {
            return resolved + arraySuffix;
        }

        // Try class cache - this will attempt on-demand loading
        ClassInfo info = classCache.get(typeName);
        if (info != null) {
            return info.getFullName() + arraySuffix;
        }

        return typeName + arraySuffix;
    }

    private boolean isPrimitive(String type) {
        switch (type) {
            case "int": case "long": case "short": case "byte":
            case "float": case "double": case "boolean": case "char":
            case "void":
                return true;
            default:
                return false;
        }
    }

    /**
     * Adds a known variable to the template context
     */
    public void addToContext(String varName, String typeName) {
        templateContext.put(varName, typeName);
    }

    /**
     * Extracts all local variable declarations from the code
     * Returns a map of variable name -> type name
     */
    public Map<String, String> extractLocalVariables(String code) {
        Map<String, String> variables = new HashMap<>();

        // First try JavaParser
        try {
            String wrappedCode = wrapInClass(code);
            CompilationUnit cu = StaticJavaParser.parse(wrappedCode);

            cu.findAll(VariableDeclarator.class).forEach(vd -> {
                String varName = vd.getNameAsString();
                String typeName = vd.getType().asString();
                String resolvedType = resolveTypeName(typeName);
                if (resolvedType != null) {
                    variables.put(varName, resolvedType);
                }
            });
        } catch (Exception e) {
            // Fallback to regex
            extractLocalVariablesRegex(code, variables);
        }

        return variables;
    }

    /**
     * Regex fallback for extracting local variables
     */
    private void extractLocalVariablesRegex(String code, Map<String, String> variables) {
        // Pattern: Type varName = ... or Type varName;
        // Handles: Client client = ..., int x = 5, List<String> items = ...
        Pattern pattern = Pattern.compile("(?:^|[;{}\\s])([A-Z]\\w*(?:<[^>]+>)?)\\s+(\\w+)\\s*[=;]", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            String typeName = matcher.group(1);
            String varName = matcher.group(2);

            // Skip common keywords that might match
            if (isKeyword(varName)) continue;

            String resolvedType = resolveTypeName(typeName);
            if (resolvedType != null) {
                variables.put(varName, resolvedType);
            }
        }

        // Also match primitive declarations
        Pattern primitivePattern = Pattern.compile("(?:^|[;{}\\s])(int|long|short|byte|float|double|boolean|char)\\s+(\\w+)\\s*[=;]", Pattern.MULTILINE);
        Matcher primitiveMatcher = primitivePattern.matcher(code);

        while (primitiveMatcher.find()) {
            String typeName = primitiveMatcher.group(1);
            String varName = primitiveMatcher.group(2);
            if (!isKeyword(varName)) {
                variables.put(varName, typeName);
            }
        }
    }

    private boolean isKeyword(String word) {
        switch (word) {
            case "if": case "else": case "for": case "while": case "do":
            case "switch": case "case": case "break": case "continue":
            case "return": case "try": case "catch": case "finally":
            case "throw": case "new": case "this": case "super":
            case "class": case "interface": case "enum": case "extends":
            case "implements": case "import": case "package": case "public":
            case "private": case "protected": case "static": case "final":
            case "void": case "null": case "true": case "false":
                return true;
            default:
                return false;
        }
    }

    /**
     * Clears and rebuilds context from static imports
     */
    public void rebuildContext() {
        templateContext.clear();
        templateContext.put("out", "java.io.PrintStream");

        // Add static imports
        for (ImportScanner.StaticImport si : importScanner.getStaticImports()) {
            if (!si.isWildcard) {
                // Get the type of the static member
                ClassInfo classInfo = classCache.get(si.className);
                if (classInfo != null) {
                    for (FieldInfo field : classInfo.getStaticFields()) {
                        if (field.getName().equals(si.memberName)) {
                            templateContext.put(si.memberName, resolveTypeName(field.getType()));
                            break;
                        }
                    }
                }
            }
        }
    }
}
