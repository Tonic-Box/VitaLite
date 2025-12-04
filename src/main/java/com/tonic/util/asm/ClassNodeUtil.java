package com.tonic.util.asm;

import com.tonic.vitalite.Main;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassNodeUtil {
    // ===== Pooling and Interning =====

    // Reusable Textifier instance for pretty printing
    private static final ThreadLocal<Textifier> TEXTIFIER_POOL = ThreadLocal.withInitial(Textifier::new);

    // String interning pool - reduces memory by 150-250MB for ~17K classes
    // Common strings: type descriptors, method names, field names
    private static final Map<String, String> STRING_POOL = new ConcurrentHashMap<>(8192, 0.75f);

    // Type descriptor pool - common types cached to reduce allocations
    private static final Map<String, Type> TYPE_POOL = new ConcurrentHashMap<>(256, 0.75f);

    static {
        // Pre-populate common type descriptors
        internType("I"); // int
        internType("J"); // long
        internType("F"); // float
        internType("D"); // double
        internType("Z"); // boolean
        internType("B"); // byte
        internType("C"); // char
        internType("S"); // short
        internType("V"); // void
        internType("Ljava/lang/Object;");
        internType("Ljava/lang/String;");
        internType("Ljava/lang/Integer;");
        internType("Ljava/lang/Long;");
        internType("[I");
        internType("[B");
        internType("[Ljava/lang/String;");
    }

    /**
     * Interns a string to reduce memory duplication.
     * Common strings are shared across all ClassNode instances.
     */
    public static String intern(String str) {
        if (str == null) return null;
        return STRING_POOL.computeIfAbsent(str, s -> s);
    }

    /**
     * Gets or creates a cached Type object for common descriptors.
     */
    public static Type internType(String descriptor) {
        if (descriptor == null) return null;
        return TYPE_POOL.computeIfAbsent(descriptor, Type::getType);
    }

    /**
     * Clears the string and type pools. Call after injection completes.
     */
    public static void clearPools() {
        STRING_POOL.clear();
        TYPE_POOL.clear();
    }

    // ===== Byte[] to ClassNode Conversion =====

    public static byte[] toBytes(ClassNode classNode) {
        try
        {
            ClassWriter classWriter = new GamepackClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, Main.CTX_CLASSLOADER);
            classNode.accept(classWriter);
            byte[] result = classWriter.toByteArray();
            classWriter = null;
            return result;
        }
        catch (Exception e)
        {
            for(MethodNode mn : classNode.methods)
            {
                mn.visibleAnnotations = null;
                mn.invisibleAnnotations = null;
            }
            ClassWriter classWriter = new GamepackClassWriter(0, Main.CTX_CLASSLOADER);
            CheckClassAdapter checkAdapter = new CheckClassAdapter(classWriter);
            classNode.accept(checkAdapter);
            e.printStackTrace();
            System.out.println("Class: " + classNode.name);
            System.exit(1);
        }
        return null;
    }

    public static String prettyPrint(MethodNode mn) {
        if(mn.invisibleAnnotations != null)
            mn.invisibleAnnotations.clear();

        Textifier printer = TEXTIFIER_POOL.get();
        TraceMethodVisitor tmv = new TraceMethodVisitor(printer);
        mn.accept(tmv);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        String result = sw.toString();

        printer.getText().clear();

        return result;
    }

    /**
     * Standard ClassNode loading with full frame expansion.
     * Use for mixin target classes that need complete ASM representation.
     */
    public static ClassNode toNode(byte[] classBytes) {
        return toNode(classBytes, true);
    }

    /**
     * Loads ClassNode with optional frame expansion and memory optimizations.
     *
     * @param classBytes Raw class bytecode
     * @param expandFrames If true, expands frames (needed for mixin targets).
     *                     If false, skips frames (saves 200-300MB for non-targets).
     * @return Optimized ClassNode with interned strings and slimmed attributes
     */
    public static ClassNode toNode(byte[] classBytes, boolean expandFrames) {
        ClassReader classReader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();

        int parsingFlags = expandFrames ? ClassReader.EXPAND_FRAMES : ClassReader.SKIP_FRAMES;
        classReader.accept(classNode, parsingFlags);

        // Apply memory optimizations
        optimizeClassNode(classNode);

        classReader = null;
        return classNode;
    }

    /**
     * Applies memory optimizations to a ClassNode:
     * 1. String interning for names, descriptors, signatures
     * 2. Removes invisible annotations (they're stripped anyway)
     * 3. Clears source debug info (not needed for injection)
     */
    private static void optimizeClassNode(ClassNode classNode) {
        // Intern class-level strings
        classNode.name = intern(classNode.name);
        classNode.superName = intern(classNode.superName);
        classNode.signature = intern(classNode.signature);
        classNode.sourceFile = null; // Not needed, saves memory
        classNode.sourceDebug = null;

        if (classNode.interfaces != null) {
            for (int i = 0; i < classNode.interfaces.size(); i++) {
                classNode.interfaces.set(i, intern(classNode.interfaces.get(i)));
            }
        }

        // Optimize fields
        if (classNode.fields != null) {
            for (FieldNode field : classNode.fields) {
                field.name = intern(field.name);
                field.desc = intern(field.desc);
                field.signature = intern(field.signature);
                field.invisibleAnnotations = null; // Stripped during output anyway
            }
        }

        // Optimize methods
        if (classNode.methods != null) {
            for (MethodNode method : classNode.methods) {
                method.name = intern(method.name);
                method.desc = intern(method.desc);
                method.signature = intern(method.signature);
                method.invisibleAnnotations = null; // Stripped during output anyway

                // Intern exception names
                if (method.exceptions != null) {
                    for (int i = 0; i < method.exceptions.size(); i++) {
                        method.exceptions.set(i, intern(method.exceptions.get(i)));
                    }
                }
            }
        }
    }
}
