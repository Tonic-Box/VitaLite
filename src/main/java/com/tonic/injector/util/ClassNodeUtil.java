package com.tonic.injector.util;

import com.tonic.vitalite.Main;
import com.tonic.injector.types.GamepackClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ClassNodeUtil {
    // Reusable Textifier instance for pretty printing (reduces allocation churn)
    private static final ThreadLocal<Textifier> TEXTIFIER_POOL = ThreadLocal.withInitial(Textifier::new);

    public static byte[] toBytes(ClassNode classNode) {
        try
        {
            // Note: GamepackClassWriter cannot be easily pooled due to ClassLoader param
            // But we reduce allocations by optimizing the calling pattern
            ClassWriter classWriter = new GamepackClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, Main.CTX_CLASSLOADER);
            classNode.accept(classWriter);
            byte[] result = classWriter.toByteArray();
            // Help GC by clearing reference (streaming pattern)
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

        // Use pooled Textifier instance
        Textifier printer = TEXTIFIER_POOL.get();
        TraceMethodVisitor tmv = new TraceMethodVisitor(printer);
        mn.accept(tmv);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        String result = sw.toString();

        // Clear for reuse
        printer.getText().clear();

        return result;
    }

    public static ClassNode toNode(byte[] classBytes) {
        // ClassReader is lightweight and primarily wraps the byte array
        // Creating new instances is actually more efficient than pooling due to byte[] reference
        ClassReader classReader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
        // Help GC by clearing reference (streaming pattern)
        classReader = null;
        return classNode;
    }
}
