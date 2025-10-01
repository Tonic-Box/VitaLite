package com.tonic.services.codeeval;

import com.tonic.Static;
import com.tonic.services.GameManager;
import org.fife.ui.autocomplete.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

public class JShellCompletionProvider implements CompletionProvider
{
    private CompletionProvider parent;
    private ListCellRenderer<Object> renderer;
    private char paramListStart = '(';
    private char paramListEnd = ')';
    private String paramListSeparator = ", ";

    @Override
    public void clearParameterizedCompletionParams() {
        // No parameterized completions to clear
    }

    @Override
    public String getAlreadyEnteredText(JTextComponent comp) {
        try {
            int caretPos = comp.getCaretPosition();
            String text = comp.getText(0, caretPos);

            // Find the start of the current word
            int start = caretPos - 1;
            while (start >= 0 && (Character.isJavaIdentifierPart(text.charAt(start)) || text.charAt(start) == '.')) {
                start--;
            }
            start++;

            return text.substring(start, caretPos);
        } catch (BadLocationException e) {
            return "";
        }
    }

    @Override
    public List<Completion> getCompletions(JTextComponent comp) {
        String enteredText = getAlreadyEnteredText(comp);
        if (enteredText == null || enteredText.trim().isEmpty()) {
            return getDefaultCompletions();
        }

        List<Completion> completions = new ArrayList<>();

        // Check if it's a method call on an object (contains dot)
        if (enteredText.contains(".")) {
            completions.addAll(getMethodCompletions(enteredText));
        } else {
            // Get all matching completions
            completions.addAll(getClassCompletions(enteredText));
            completions.addAll(getStaticMethodCompletions(enteredText));
            completions.addAll(getDefaultCompletions(enteredText));
        }

        return completions;
    }

    private List<Completion> getDefaultCompletions() {
        List<Completion> completions = new ArrayList<>();

        // Dynamically discover all available classes and their static methods
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // Get all loaded classes that are likely to be useful
        completions.addAll(discoverStaticMethods(classLoader));
        completions.addAll(discoverAvailableClasses(classLoader));

        return completions;
    }

    private List<Completion> getDefaultCompletions(String prefix) {
        List<Completion> completions = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();

        // Dynamically find classes and methods matching the prefix
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // Search for classes starting with prefix
        completions.addAll(findClassesByPrefix(classLoader, prefix));

        // Search for static methods on known classes
        completions.addAll(findStaticMethodsByPrefix(classLoader, prefix));

        return completions;
    }

    private List<Completion> getClassCompletions(String prefix) {
        return findClassesByPrefix(Thread.currentThread().getContextClassLoader(), prefix);
    }

    private List<Completion> getStaticMethodCompletions(String prefix) {
        return findStaticMethodsByPrefix(Thread.currentThread().getContextClassLoader(), prefix);
    }

    private List<Completion> discoverStaticMethods(ClassLoader classLoader) {
        List<Completion> completions = new ArrayList<>();

        // Known utility classes to scan for static methods
        String[] utilityClasses = {
            "com.tonic.Static",
            "com.tonic.services.GameManager",
            "java.lang.System",
            "java.lang.Math",
            "java.util.Arrays",
            "java.util.Collections"
        };

        for (String className : utilityClasses) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                Method[] methods = clazz.getDeclaredMethods();

                for (Method method : methods) {
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) &&
                        java.lang.reflect.Modifier.isPublic(method.getModifiers())) {

                        String methodCall = clazz.getSimpleName() + "." + method.getName() + "()";
                        String description = buildMethodSignature(method);
                        completions.add(new BasicCompletion(this, methodCall, description));
                    }
                }
            } catch (ClassNotFoundException | SecurityException e) {
                // Skip if class not found or security issue
            }
        }

        return completions;
    }

    private List<Completion> discoverAvailableClasses(ClassLoader classLoader) {
        List<Completion> completions = new ArrayList<>();

        // Common packages to scan
        String[] commonPackages = {
            "java.lang",
            "java.util",
            "java.io",
            "java.nio.file",
            "com.tonic",
            "net.runelite.api"
        };

        for (String packageName : commonPackages) {
            try {
                // This is a simplified approach - in practice you might need package scanning
                Package pkg = Package.getPackage(packageName);
                if (pkg != null) {
                    // Add some common classes from these packages
                    addCommonClassesFromPackage(completions, packageName, classLoader);
                }
            } catch (Exception e) {
                // Skip if package scanning fails
            }
        }

        return completions;
    }

    private void addCommonClassesFromPackage(List<Completion> completions, String packageName, ClassLoader classLoader) {
        // Since Java doesn't provide easy package scanning, we'll check common class names
        String[] commonClassNames = getCommonClassNamesForPackage(packageName);

        for (String className : commonClassNames) {
            try {
                String fullClassName = packageName + "." + className;
                Class<?> clazz = classLoader.loadClass(fullClassName);
                completions.add(new BasicCompletion(this, className, clazz.getName()));
            } catch (ClassNotFoundException e) {
                // Skip if class not found
            }
        }
    }

    private String[] getCommonClassNamesForPackage(String packageName) {
        switch (packageName) {
            case "java.lang":
                return new String[]{"String", "Integer", "Double", "Boolean", "Object", "Class", "System", "Math", "Thread"};
            case "java.util":
                return new String[]{"List", "Map", "Set", "ArrayList", "HashMap", "HashSet", "Arrays", "Collections", "Optional"};
            case "java.io":
                return new String[]{"File", "InputStream", "OutputStream", "FileInputStream", "FileOutputStream"};
            case "java.nio.file":
                return new String[]{"Path", "Paths", "Files"};
            case "com.tonic":
                return new String[]{"Static"};
            case "net.runelite.api":
                return new String[]{"Client", "Player", "NPC", "GameObject", "Item"};
            default:
                return new String[0];
        }
    }

    private List<Completion> findClassesByPrefix(ClassLoader classLoader, String prefix) {
        List<Completion> completions = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();

        // Search through known packages for classes matching prefix
        String[] packages = {"java.lang", "java.util", "java.io", "com.tonic", "net.runelite.api"};

        for (String packageName : packages) {
            String[] classNames = getCommonClassNamesForPackage(packageName);
            for (String className : classNames) {
                if (className.toLowerCase().startsWith(lowerPrefix)) {
                    try {
                        Class<?> clazz = classLoader.loadClass(packageName + "." + className);
                        completions.add(new BasicCompletion(this, className, clazz.getName()));
                    } catch (ClassNotFoundException e) {
                        // Skip
                    }
                }
            }
        }

        return completions;
    }

    private List<Completion> findStaticMethodsByPrefix(ClassLoader classLoader, String prefix) {
        List<Completion> completions = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();

        // Check if prefix matches a class name, then show its static methods
        String[] utilityClasses = {
            "com.tonic.Static",
            "com.tonic.services.GameManager",
            "java.lang.System",
            "java.lang.Math",
            "java.util.Arrays",
            "java.util.Collections"
        };

        for (String className : utilityClasses) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                String simpleClassName = clazz.getSimpleName();

                if (simpleClassName.toLowerCase().startsWith(lowerPrefix)) {
                    // Add all static methods for this class
                    Method[] methods = clazz.getDeclaredMethods();
                    for (Method method : methods) {
                        if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) &&
                            java.lang.reflect.Modifier.isPublic(method.getModifiers())) {

                            String methodCall = simpleClassName + "." + method.getName() + "()";
                            String description = buildMethodSignature(method);
                            completions.add(new BasicCompletion(this, methodCall, description));
                        }
                    }
                }
            } catch (ClassNotFoundException | SecurityException e) {
                // Skip
            }
        }

        return completions;
    }

    private List<Completion> getMethodCompletions(String enteredText) {
        List<Completion> completions = new ArrayList<>();

        try {
            // Parse the expression to get the object type
            int lastDotIndex = enteredText.lastIndexOf('.');
            String objectExpression = enteredText.substring(0, lastDotIndex);
            String methodPrefix = enteredText.substring(lastDotIndex + 1);

            Class<?> targetClass = resolveExpressionType(objectExpression);
            if (targetClass != null) {
                // Get methods from the class
                Method[] methods = targetClass.getMethods();
                for (Method method : methods) {
                    String methodName = method.getName();
                    if (methodName.toLowerCase().startsWith(methodPrefix.toLowerCase())) {
                        String signature = buildMethodSignature(method);
                        completions.add(new BasicCompletion(this, methodName + "()", signature));
                    }
                }

                // Get fields from the class
                Field[] fields = targetClass.getFields();
                for (Field field : fields) {
                    String fieldName = field.getName();
                    if (fieldName.toLowerCase().startsWith(methodPrefix.toLowerCase())) {
                        completions.add(new BasicCompletion(this, fieldName, field.getType().getSimpleName()));
                    }
                }
            }
        } catch (Exception e) {
            // If we can't resolve the type, just return empty list
        }

        return completions;
    }

    private Class<?> resolveExpressionType(String expression) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // If it's a method call, try to resolve the return type dynamically
            if (expression.contains("(") && expression.endsWith(")")) {
                return resolveMethodReturnType(expression, classLoader);
            }

            // If it's a simple class reference, try to load it
            if (!expression.contains(".")) {
                return tryLoadSimpleClassName(expression, classLoader);
            }

            // If it's a field access, try to resolve the field type
            if (expression.contains(".") && !expression.contains("(")) {
                return resolveFieldType(expression, classLoader);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Class<?> resolveMethodReturnType(String methodExpression, ClassLoader classLoader) {
        try {
            // Parse method call: "ClassName.methodName()" or "object.methodName()"
            int lastDot = methodExpression.lastIndexOf('.');
            if (lastDot == -1) return null;

            String objectPart = methodExpression.substring(0, lastDot);
            String methodPart = methodExpression.substring(lastDot + 1);
            String methodName = methodPart.substring(0, methodPart.indexOf('('));

            // Try to resolve the class of the object
            Class<?> objectClass = resolveClassForExpression(objectPart, classLoader);
            if (objectClass == null) return null;

            // Find the method and return its return type
            Method[] methods = objectClass.getMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    return method.getReturnType();
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Class<?> resolveFieldType(String fieldExpression, ClassLoader classLoader) {
        try {
            int lastDot = fieldExpression.lastIndexOf('.');
            if (lastDot == -1) return null;

            String objectPart = fieldExpression.substring(0, lastDot);
            String fieldName = fieldExpression.substring(lastDot + 1);

            Class<?> objectClass = resolveClassForExpression(objectPart, classLoader);
            if (objectClass == null) return null;

            Field[] fields = objectClass.getFields();
            for (Field field : fields) {
                if (field.getName().equals(fieldName)) {
                    return field.getType();
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Class<?> resolveClassForExpression(String expression, ClassLoader classLoader) {
        try {
            // If it's a method call, resolve its return type recursively
            if (expression.contains("(") && expression.endsWith(")")) {
                return resolveMethodReturnType(expression, classLoader);
            }

            // Try as a simple class name
            Class<?> simpleClass = tryLoadSimpleClassName(expression, classLoader);
            if (simpleClass != null) return simpleClass;

            // Try as a fully qualified class name
            try {
                return classLoader.loadClass(expression);
            } catch (ClassNotFoundException e) {
                // Not a class name
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Class<?> tryLoadSimpleClassName(String className, ClassLoader classLoader) {
        // Try common packages for simple class names
        String[] packages = {
            "java.lang",
            "java.util",
            "java.io",
            "java.nio.file",
            "com.tonic",
            "com.tonic.services",
            "net.runelite.api"
        };

        for (String packageName : packages) {
            try {
                return classLoader.loadClass(packageName + "." + className);
            } catch (ClassNotFoundException e) {
                // Try next package
            }
        }

        return null;
    }

    private String buildMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getReturnType().getSimpleName()).append(" ");
        sb.append(method.getName()).append("(");

        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramTypes[i].getSimpleName());
        }

        sb.append(")");
        return sb.toString();
    }

    @Override
    public List<Completion> getCompletionsAt(JTextComponent comp, Point p) {
        return getCompletions(comp);
    }

    @Override
    public ListCellRenderer<Object> getListCellRenderer() {
        return renderer;
    }

    @Override
    public ParameterChoicesProvider getParameterChoicesProvider() {
        return null;
    }

    @Override
    public List<ParameterizedCompletion> getParameterizedCompletions(JTextComponent tc) {
        return new ArrayList<>();
    }

    @Override
    public char getParameterListEnd() {
        return paramListEnd;
    }

    @Override
    public String getParameterListSeparator() {
        return paramListSeparator;
    }

    @Override
    public char getParameterListStart() {
        return paramListStart;
    }

    @Override
    public CompletionProvider getParent() {
        return parent;
    }

    @Override
    public boolean isAutoActivateOkay(JTextComponent tc) {
        return true;
    }

    @Override
    public void setListCellRenderer(ListCellRenderer<Object> r) {
        this.renderer = r;
    }

    @Override
    public void setParameterizedCompletionParams(char listStart, String separator, char listEnd) {
        this.paramListStart = listStart;
        this.paramListSeparator = separator;
        this.paramListEnd = listEnd;
    }

    @Override
    public void setParent(CompletionProvider parent) {
        this.parent = parent;
    }
}
