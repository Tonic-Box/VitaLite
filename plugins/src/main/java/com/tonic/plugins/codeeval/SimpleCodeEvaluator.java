package com.tonic.plugins.codeeval;

import com.tonic.api.game.MovementAPI;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.Scanner;

public class SimpleCodeEvaluator {
    private final ClassLoader parentClassLoader;
    private int classCounter = 0;

    public SimpleCodeEvaluator(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
    }

    public ClassLoader getParentClassLoader() {
        return parentClassLoader;
    }

    public Object evaluate(String code) {
        // Save current classloader context
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            // Set the thread context classloader to our parent classloader for execution
            Thread.currentThread().setContextClassLoader(parentClassLoader);

            // Wrap code in a class
            String className = "DynamicCode" + (++classCounter);
            String fullCode = buildClassCode(className, code);

            // Compile the code
            Class<?> compiledClass = compileAndLoad(className, fullCode);

            // Execute the run method
            Method runMethod = compiledClass.getMethod("run");
            Object instance = compiledClass.getDeclaredConstructor().newInstance();
            return runMethod.invoke(instance);

        } catch (Exception e) {
            System.err.println("Error evaluating code: " + e.getMessage());
            return null;
        } finally {
            // Restore original classloader context
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private String buildClassCode(String className, String userCode) {
        try {
            // Load template from resources
            InputStream templateStream = getClass().getResourceAsStream("code_template.java");
            if (templateStream == null) {
                throw new RuntimeException("Could not find code template");
            }

            String template;
            try (Scanner scanner = new Scanner(templateStream, "UTF-8")) {
                scanner.useDelimiter("\\A");
                template = scanner.hasNext() ? scanner.next() : "";
            }

            // Replace placeholders
            return template.replace("%CLASS_NAME%", className).replace("%USER_CODE%", userCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build class code", e);
        }
    }

    private Class<?> compileAndLoad(String className, String code) throws Exception {
        // Get Java compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("Java compiler not available");
        }

        // Create diagnostic collector to capture compilation errors
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        // Create in-memory file manager
        InMemoryFileManager fileManager = new InMemoryFileManager(
            compiler.getStandardFileManager(diagnostics, null, null)
        );

        // Create source file
        JavaFileObject sourceFile = new InMemoryJavaFileObject(className, code);

        // Set compiler options to use comprehensive classpath
        String classpath = buildComprehensiveClasspath();
        List<String> options = Arrays.asList(
            "-cp", classpath
        );

        // Compile
        JavaCompiler.CompilationTask task = compiler.getTask(
            null, fileManager, diagnostics, options, null, Arrays.asList(sourceFile)
        );

        if (!task.call()) {
            StringBuilder errorMessage = new StringBuilder("Compilation failed:\n");
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                errorMessage.append(diagnostic.toString()).append("\n");
            }
            throw new RuntimeException(errorMessage.toString());
        }

        // Load the compiled class using our custom classloader
        CompiledJavaFileObject compiledClass = fileManager.getCompiledClass(className);

        if (compiledClass == null) {
            throw new RuntimeException("No compiled class found for: " + className);
        }

        byte[] classBytes = compiledClass.getBytes();
        // System.out.println("Compiled class " + className + " with " + classBytes.length + " bytes");

        CustomClassLoader classLoader = new CustomClassLoader(parentClassLoader, className, compiledClass);

        return classLoader.loadClass(className);
    }

    private String buildComprehensiveClasspath() {
        StringBuilder classpathBuilder = new StringBuilder();

        // Start with system classpath
        classpathBuilder.append(System.getProperty("java.class.path"));

        // Add URLs from the RLClassLoader if it's a URLClassLoader
        if (parentClassLoader instanceof java.net.URLClassLoader) {
            java.net.URLClassLoader urlClassLoader = (java.net.URLClassLoader) parentClassLoader;
            java.net.URL[] urls = urlClassLoader.getURLs();

            for (java.net.URL url : urls) {
                classpathBuilder.append(System.getProperty("path.separator"));

                // Convert URL to file path
                try {
                    String path = java.nio.file.Paths.get(url.toURI()).toString();
                    classpathBuilder.append(path);
                } catch (Exception e) {
                    // Fallback to URL path if URI conversion fails
                    classpathBuilder.append(url.getPath());
                }
            }
        }

        return classpathBuilder.toString();
    }

    // In-memory Java file object
    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String code;

        public InMemoryJavaFileObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    // Compiled Java file object
    private static class CompiledJavaFileObject extends InMemoryJavaFileObject {
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public CompiledJavaFileObject(String className) {
            super(className, null);
        }

        @Override
        public OutputStream openOutputStream() {
            return baos;
        }

        public byte[] getBytes() {
            return baos.toByteArray();
        }
    }

    // In-memory file manager
    private static class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, InMemoryJavaFileObject> compiledClasses = new HashMap<>();

        public InMemoryFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            CompiledJavaFileObject file = new CompiledJavaFileObject(className);
            compiledClasses.put(className, file);
            return file;
        }

        public CompiledJavaFileObject getCompiledClass(String className) {
            return (CompiledJavaFileObject) compiledClasses.get(className);
        }
    }

    // Custom classloader that integrates with RLClassLoader's child-first mechanism
    private static class CustomClassLoader extends ClassLoader {
        private final String expectedClassName;
        private final CompiledJavaFileObject compiledClass;

        public CustomClassLoader(ClassLoader parent, String expectedClassName, CompiledJavaFileObject compiledClass) {
            super(parent);
            this.expectedClassName = expectedClassName;
            this.compiledClass = compiledClass;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // First check if this is our compiled class
            if (name.equals(expectedClassName)) {
                return findClass(name);
            }

            // For all other classes, delegate to the parent (RLClassLoader)
            // This preserves the child-first loading behavior
            return super.loadClass(name);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.equals(expectedClassName)) {
                byte[] bytes = compiledClass.getBytes();
                if (bytes.length == 0) {
                    throw new ClassNotFoundException("No bytecode available for " + name);
                }
                return defineClass(name, bytes, 0, bytes.length);
            }
            throw new ClassNotFoundException(name);
        }
    }
}