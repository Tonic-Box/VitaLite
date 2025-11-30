package com.tonic.plugins.codeeval.completion;

import org.fife.ui.autocomplete.*;

import javax.swing.text.JTextComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom CompletionProvider for VitaLite CodeEval with IntelliJ-style behavior
 */
public class VitaCompletionProvider extends DefaultCompletionProvider {
    private final ClassCache classCache;
    private final TypeInference typeInference;
    private final ImportScanner importScanner;

    // Patterns for context detection
    private static final Pattern AFTER_DOT = Pattern.compile("([\\w.()]+)\\.$");
    private static final Pattern IDENTIFIER = Pattern.compile("([a-zA-Z_][\\w]*)$");
    private static final Pattern AFTER_NEW = Pattern.compile("new\\s+(\\w*)$");

    // Completion context types
    private enum ContextType {
        AFTER_DOT,      // varName. or expr.
        IDENTIFIER,     // Typing a name (class, var, or method)
        NEW_STATEMENT,  // After "new "
        UNKNOWN
    }

    private static class CompletionContext {
        ContextType type;
        String expression;  // The expression before dot (for AFTER_DOT)
        String prefix;      // What user has typed so far

        CompletionContext(ContextType type, String prefix) {
            this.type = type;
            this.prefix = prefix;
        }

        CompletionContext(ContextType type, String expression, String prefix) {
            this.type = type;
            this.expression = expression;
            this.prefix = prefix;
        }
    }

    public VitaCompletionProvider(ClassCache classCache, TypeInference typeInference, ImportScanner importScanner) {
        this.classCache = classCache;
        this.typeInference = typeInference;
        this.importScanner = importScanner;

        // Configure auto-activation: trigger after dot AND when typing letters
        // First param: activate after letters (for class/variable names)
        // Second param: additional trigger chars (dot for member access)
        setAutoActivationRules(true, ".");
    }

    @Override
    public boolean isAutoActivateOkay(JTextComponent tc) {
        // Always allow auto-activation - the AutoCompletion framework handles
        // trigger characters via setAutoActivationRules(true, ".")
        // We just need to say "yes, it's okay to show completions"
        return true;
    }

    @Override
    public List<Completion> getCompletions(JTextComponent comp) {
        String text = comp.getText();
        int caretPos = comp.getCaretPosition();
        String textBefore = text.substring(0, Math.min(caretPos, text.length()));

        CompletionContext ctx = analyzeContext(textBefore);

        List<Completion> completions = new ArrayList<>();

        switch (ctx.type) {
            case AFTER_DOT:
                completions.addAll(getMemberCompletions(ctx.expression, ctx.prefix, text));
                break;

            case IDENTIFIER:
                completions.addAll(getIdentifierCompletions(ctx.prefix, text));
                break;

            case NEW_STATEMENT:
                completions.addAll(getConstructorCompletions(ctx.prefix));
                break;

            default:
                // Return empty or all classes
                if (ctx.prefix != null && !ctx.prefix.isEmpty()) {
                    completions.addAll(getIdentifierCompletions(ctx.prefix, text));
                }
        }

        return completions;
    }

    /**
     * Analyzes text to determine completion context
     */
    private CompletionContext analyzeContext(String textBefore) {
        // Check for "new ClassName"
        Matcher newMatcher = AFTER_NEW.matcher(textBefore);
        if (newMatcher.find()) {
            return new CompletionContext(ContextType.NEW_STATEMENT, newMatcher.group(1));
        }

        // Check for expression followed by dot (ends with .)
        Matcher dotMatcher = AFTER_DOT.matcher(textBefore);
        if (dotMatcher.find()) {
            String expression = dotMatcher.group(1);
            return new CompletionContext(ContextType.AFTER_DOT, expression, "");
        }

        // Check if we're typing after a dot with partial input
        int lastDot = textBefore.lastIndexOf('.');
        if (lastDot >= 0) {
            String afterDot = textBefore.substring(lastDot + 1);
            // Check if afterDot is just identifier characters (no spaces, operators)
            if (afterDot.matches("\\w*")) {
                String beforeDot = textBefore.substring(0, lastDot);
                Matcher exprMatcher = Pattern.compile("([\\w.()]+)$").matcher(beforeDot);
                if (exprMatcher.find()) {
                    return new CompletionContext(ContextType.AFTER_DOT, exprMatcher.group(1), afterDot);
                }
            }
        }

        // Check for identifier
        Matcher idMatcher = IDENTIFIER.matcher(textBefore);
        if (idMatcher.find()) {
            return new CompletionContext(ContextType.IDENTIFIER, idMatcher.group(1));
        }

        return new CompletionContext(ContextType.UNKNOWN, "");
    }

    /**
     * Gets completions for member access (after a dot)
     */
    private List<Completion> getMemberCompletions(String expression, String prefix, String fullCode) {
        List<Completion> completions = new ArrayList<>();

        // Resolve the type of the expression
        String type = typeInference.resolveExpressionType(expression, fullCode);

        if (type != null) {
            ClassInfo classInfo = classCache.get(type);

            if (classInfo != null) {
                // Determine if this is static or instance access
                boolean isStaticAccess = isStaticAccess(expression);

                // For instance access (local var like "client."), show instance methods
                // For static access (class name like "Static."), show static methods first
                // But always show both since Java allows calling static methods on instances

                // Add instance methods
                for (MethodInfo method : classInfo.getMethods()) {
                    if (method.matchesPrefix(prefix)) {
                        completions.add(createMethodCompletion(method));
                    }
                }

                // Add static methods
                for (MethodInfo method : classInfo.getStaticMethods()) {
                    if (method.matchesPrefix(prefix)) {
                        completions.add(createMethodCompletion(method));
                    }
                }

                // Add instance fields
                for (FieldInfo field : classInfo.getFields()) {
                    if (field.matchesPrefix(prefix)) {
                        completions.add(createFieldCompletion(field));
                    }
                }

                // Add static fields
                for (FieldInfo field : classInfo.getStaticFields()) {
                    if (field.matchesPrefix(prefix)) {
                        completions.add(createFieldCompletion(field));
                    }
                }
            }
        }

        // Sort by name
        completions.sort(Comparator.comparing(Completion::getInputText));
        return completions;
    }

    /**
     * Checks if an expression is a static class reference
     */
    private boolean isStaticAccess(String expression) {
        // If expression doesn't contain parentheses, it might be a class name
        if (!expression.contains("(")) {
            ClassInfo info = classCache.getBySimpleName(expression);
            return info != null;
        }
        return false;
    }

    /**
     * Gets completions for identifiers (class names, variables, methods)
     */
    private List<Completion> getIdentifierCompletions(String prefix, String fullCode) {
        List<Completion> completions = new ArrayList<>();

        // Add local variables from the code
        Map<String, String> localVars = typeInference.extractLocalVariables(fullCode);
        for (Map.Entry<String, String> entry : localVars.entrySet()) {
            String varName = entry.getKey();
            String varType = entry.getValue();
            if (varName.toLowerCase().startsWith(prefix.toLowerCase())) {
                BasicCompletion bc = new BasicCompletion(this, varName);
                // Show simple type name in description
                String simpleType = varType.contains(".") ?
                        varType.substring(varType.lastIndexOf('.') + 1) : varType;
                bc.setShortDescription(simpleType + " (local variable)");
                completions.add(bc);
            }
        }

        // Add matching class names
        for (ClassInfo classInfo : classCache.getClassesMatching(prefix)) {
            completions.add(createClassCompletion(classInfo));
        }

        // Add common keywords
        String[] keywords = {"if", "else", "for", "while", "do", "switch", "case", "break",
                "continue", "return", "try", "catch", "finally", "throw", "new", "this",
                "true", "false", "null", "instanceof"};

        for (String keyword : keywords) {
            if (keyword.startsWith(prefix.toLowerCase())) {
                completions.add(new BasicCompletion(this, keyword));
            }
        }

        // Sort: local vars first, then classes, then keywords
        completions.sort((a, b) -> {
            // Check if it's a local variable by checking if it's a BasicCompletion with our marker
            boolean aIsLocal = (a instanceof BasicCompletion) &&
                    !(a instanceof ShorthandCompletion) &&
                    !(a instanceof FunctionCompletion);
            boolean bIsLocal = (b instanceof BasicCompletion) &&
                    !(b instanceof ShorthandCompletion) &&
                    !(b instanceof FunctionCompletion);
            boolean aIsClass = a instanceof ShorthandCompletion;
            boolean bIsClass = b instanceof ShorthandCompletion;

            // Local vars first
            if (aIsLocal && !bIsLocal) return -1;
            if (!aIsLocal && bIsLocal) return 1;
            // Then classes
            if (aIsClass && !bIsClass) return -1;
            if (!aIsClass && bIsClass) return 1;
            return a.getInputText().compareTo(b.getInputText());
        });

        return completions;
    }

    /**
     * Gets completions for constructors (after "new")
     */
    private List<Completion> getConstructorCompletions(String prefix) {
        List<Completion> completions = new ArrayList<>();

        for (ClassInfo classInfo : classCache.getClassesMatching(prefix)) {
            // Skip interfaces and abstract classes
            if (!classInfo.isInterface()) {
                completions.add(createClassCompletion(classInfo));
            }
        }

        completions.sort(Comparator.comparing(Completion::getInputText));
        return completions;
    }

    /**
     * Creates a completion for a method
     */
    private Completion createMethodCompletion(MethodInfo method) {
        // Use BasicCompletion to insert just the method name (no parentheses/params)
        BasicCompletion bc = new BasicCompletion(this, method.getName());
        bc.setShortDescription(method.getShortDescription());
        return bc;
    }

    /**
     * Creates a completion for a field
     */
    private Completion createFieldCompletion(FieldInfo field) {
        BasicCompletion bc = new BasicCompletion(this, field.getName());
        bc.setShortDescription(field.getShortDescription());
        return bc;
    }

    /**
     * Creates a completion for a class
     */
    private Completion createClassCompletion(ClassInfo classInfo) {
        ShorthandCompletion sc = new ShorthandCompletion(this,
                classInfo.getSimpleName(),
                classInfo.getSimpleName());
        sc.setShortDescription(classInfo.getFullName());
        return sc;
    }

    @Override
    public String getAlreadyEnteredText(JTextComponent comp) {
        String text = comp.getText();
        int caretPos = comp.getCaretPosition();
        String textBefore = text.substring(0, Math.min(caretPos, text.length()));

        // Find the start of current token
        int start = caretPos;
        while (start > 0) {
            char c = textBefore.charAt(start - 1);
            if (!Character.isJavaIdentifierPart(c) && c != '.') {
                break;
            }
            start--;
        }

        // If there's a dot, only return text after the last dot
        String token = textBefore.substring(start);
        int lastDot = token.lastIndexOf('.');
        if (lastDot >= 0) {
            return token.substring(lastDot + 1);
        }

        return token;
    }
}
