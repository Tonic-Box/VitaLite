package com.tonic.plugins.codeeval.completion;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses template imports to determine which packages/classes to scan for autocompletion
 */
public class ImportScanner {
    private static final Pattern WILDCARD_IMPORT = Pattern.compile("import\\s+([\\w.]+)\\.\\*;");
    private static final Pattern CLASS_IMPORT = Pattern.compile("import\\s+([\\w.]+);");
    private static final Pattern STATIC_IMPORT = Pattern.compile("import\\s+static\\s+([\\w.]+)\\.([\\w]+);");
    private static final Pattern STATIC_WILDCARD_IMPORT = Pattern.compile("import\\s+static\\s+([\\w.]+)\\.\\*;");

    private final Set<String> importedPackages = new HashSet<>();
    private final Set<String> importedClasses = new HashSet<>();
    private final Set<StaticImport> staticImports = new HashSet<>();

    /**
     * Represents a static import (class + member)
     */
    public static class StaticImport {
        public final String className;
        public final String memberName;
        public final boolean isWildcard;

        public StaticImport(String className, String memberName, boolean isWildcard) {
            this.className = className;
            this.memberName = memberName;
            this.isWildcard = isWildcard;
        }
    }

    /**
     * Parses imports from a template input stream
     */
    public void parseTemplate(InputStream templateStream) {
        if (templateStream == null) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(templateStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line);
            }
        } catch (Exception e) {
            System.err.println("Error parsing template imports: " + e.getMessage());
        }
    }

    /**
     * Parses imports from template content string
     */
    public void parseTemplateString(String templateContent) {
        if (templateContent == null) return;

        for (String line : templateContent.split("\n")) {
            parseLine(line);
        }
    }

    private void parseLine(String line) {
        line = line.trim();
        if (!line.startsWith("import")) return;

        // Check for static wildcard import: import static java.lang.System.*;
        Matcher staticWildcardMatcher = STATIC_WILDCARD_IMPORT.matcher(line);
        if (staticWildcardMatcher.find()) {
            String className = staticWildcardMatcher.group(1);
            staticImports.add(new StaticImport(className, "*", true));
            return;
        }

        // Check for static import: import static java.lang.System.out;
        Matcher staticMatcher = STATIC_IMPORT.matcher(line);
        if (staticMatcher.find()) {
            String className = staticMatcher.group(1);
            String memberName = staticMatcher.group(2);
            staticImports.add(new StaticImport(className, memberName, false));
            return;
        }

        // Check for wildcard import: import com.tonic.api.*;
        Matcher wildcardMatcher = WILDCARD_IMPORT.matcher(line);
        if (wildcardMatcher.find()) {
            importedPackages.add(wildcardMatcher.group(1));
            return;
        }

        // Check for class import: import com.tonic.Static;
        Matcher classMatcher = CLASS_IMPORT.matcher(line);
        if (classMatcher.find()) {
            String fullClassName = classMatcher.group(1);
            importedClasses.add(fullClassName);

            // Also add the package for broader scanning
            int lastDot = fullClassName.lastIndexOf('.');
            if (lastDot > 0) {
                importedPackages.add(fullClassName.substring(0, lastDot));
            }
        }
    }

    /**
     * Gets all packages that should be scanned for classes
     */
    public Set<String> getPackagesToScan() {
        return new HashSet<>(importedPackages);
    }

    /**
     * Gets explicitly imported class names (fully qualified)
     */
    public Set<String> getExplicitClasses() {
        return new HashSet<>(importedClasses);
    }

    /**
     * Gets static imports for pre-populating completion context
     */
    public Set<StaticImport> getStaticImports() {
        return new HashSet<>(staticImports);
    }

    /**
     * Checks if a fully qualified class name is in one of the imported packages
     */
    public boolean isInImportedPackage(String fullClassName) {
        if (importedClasses.contains(fullClassName)) {
            return true;
        }

        int lastDot = fullClassName.lastIndexOf('.');
        if (lastDot > 0) {
            String packageName = fullClassName.substring(0, lastDot);
            return importedPackages.contains(packageName);
        }

        return false;
    }

    /**
     * Resolves a simple class name to its fully qualified name using imports
     */
    public String resolveClassName(String simpleName) {
        // Check explicit imports first
        for (String fullName : importedClasses) {
            if (fullName.endsWith("." + simpleName)) {
                return fullName;
            }
        }

        // Check java.lang (always imported)
        try {
            Class.forName("java.lang." + simpleName);
            return "java.lang." + simpleName;
        } catch (ClassNotFoundException ignored) {}

        // Search wildcard-imported packages (e.g., import net.runelite.api.*;)
        for (String pkg : importedPackages) {
            try {
                Class.forName(pkg + "." + simpleName);
                return pkg + "." + simpleName;
            } catch (ClassNotFoundException ignored) {}
        }

        return null;
    }

    @Override
    public String toString() {
        return "ImportScanner{" +
                "packages=" + importedPackages.size() +
                ", classes=" + importedClasses.size() +
                ", staticImports=" + staticImports.size() +
                '}';
    }
}
