package com.tonic.injector;

import com.tonic.mixins.TLoginHashMixin;
import com.tonic.patch.BytecodePatcher;
import com.tonic.util.MappingProvider;
import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JMethod;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LoginHashPatchTest
{
    private static final String STRING_METHOD_DESCRIPTOR = "(J)Ljava/lang/String;";

    @Test
    void callStackCheckMapsToTheClientStackBuilder()
    {
        List<JClass> declaringClasses = new ArrayList<>();
        List<JMethod> namedMethods = new ArrayList<>();
        int exactTargetRecords = 0;

        for (JClass mappedClass : MappingProvider.getMappings())
        {
            for (JMethod method : mappedClass.getMethods())
            {
                if ("callStackCheck".equals(method.getName()))
                {
                    declaringClasses.add(mappedClass);
                    namedMethods.add(method);
                }
                if ("client".equals(method.getOwnerObfuscatedName())
                        && "qa".equals(method.getObfuscatedName())
                        && STRING_METHOD_DESCRIPTOR.equals(method.getDescriptor()))
                {
                    exactTargetRecords++;
                }
            }
        }

        assertEquals(1, namedMethods.size(), "callStackCheck must have one mapping");
        assertEquals(1, exactTargetRecords, "client.qa must have one mapping record");
        assertEquals("Client", declaringClasses.get(0).getName());
        assertEquals("client", declaringClasses.get(0).getObfuscatedName());

        JMethod mapping = namedMethods.get(0);
        assertEquals("Client", mapping.getOwner());
        assertEquals("client", mapping.getOwnerObfuscatedName());
        assertEquals("qa", mapping.getObfuscatedName());
        assertEquals(STRING_METHOD_DESCRIPTOR, mapping.getDescriptor());
        assertTrue(mapping.isStatic());
    }

    @Test
    void packagedPatchOverridesTheStackBuilderAndPreservesTheNameDecoder() throws IOException
    {
        byte[] originalClient = readResource("/client.class");
        byte[] patchedClient = applyPackagedPatch("client", originalClient, true);
        MethodNode originalStackBuilder = findMethod(originalClient, "qa", STRING_METHOD_DESCRIPTOR);
        MethodNode patchedStackBuilder = findMethod(patchedClient, "qa", STRING_METHOD_DESCRIPTOR);

        assertTrue(callsGetStackTrace(originalStackBuilder), "revision fixture no longer has the expected stack builder");
        assertFalse(callsGetStackTrace(patchedStackBuilder), "packaged client patch still collects the live stack");
        assertEquals(
                Collections.singletonList(TLoginHashMixin.callStackCheck(0L)),
                loadedStrings(patchedStackBuilder),
                "client.qa must return the configured stack value"
        );
        assertEquals(
                Arrays.asList(Opcodes.LDC, Opcodes.ARETURN),
                executableOpcodes(patchedStackBuilder),
                "client.qa must be fully replaced"
        );
        assertTrue(
                containsCallStackBootstrapHandle(patchedClient),
                "the login fingerprint bootstrap no longer targets client.qa"
        );

        byte[] originalNameDecoderClass = readResource("/pk.class");
        byte[] patchedNameDecoderClass = applyPackagedPatch("pk", originalNameDecoderClass, false);
        MethodNode originalNameDecoder = findMethod(originalNameDecoderClass, "ag", STRING_METHOD_DESCRIPTOR);
        MethodNode patchedNameDecoder = findMethod(patchedNameDecoderClass, "ag", STRING_METHOD_DESCRIPTOR);

        assertTrue(hasOpcode(patchedNameDecoder, Opcodes.LREM));
        assertTrue(hasOpcode(patchedNameDecoder, Opcodes.LDIV));
        assertTrue(loadsLong(patchedNameDecoder, 37L));
        assertEquals(
                executableOpcodes(originalNameDecoder),
                executableOpcodes(patchedNameDecoder),
                "pk.ag must retain its base-37 decoder implementation"
        );
    }

    private static byte[] applyPackagedPatch(String className, byte[] original, boolean required) throws IOException
    {
        byte[] diff = readPatchEntry("gamepack/" + className + ".diff");
        if (diff != null)
        {
            return BytecodePatcher.applyDiff(original, diff);
        }
        if (required)
        {
            fail("Missing packaged patch for " + className);
        }
        return original;
    }

    private static byte[] readPatchEntry(String expectedName) throws IOException
    {
        try (InputStream stream = LoginHashPatchTest.class.getResourceAsStream("/com/tonic/patches.zip"))
        {
            assertNotNull(stream, "Missing packaged patches.zip");
            try (ZipInputStream zip = new ZipInputStream(stream))
            {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null)
                {
                    if (expectedName.equals(entry.getName()))
                    {
                        return zip.readAllBytes();
                    }
                }
            }
        }
        return null;
    }

    private static byte[] readResource(String name) throws IOException
    {
        try (InputStream stream = LoginHashPatchTest.class.getResourceAsStream(name))
        {
            assertNotNull(stream, "Missing test runtime resource " + name);
            return stream.readAllBytes();
        }
    }

    private static MethodNode findMethod(byte[] bytecode, String name, String descriptor)
    {
        ClassNode classNode = readClass(bytecode);
        for (MethodNode method : classNode.methods)
        {
            if (name.equals(method.name) && descriptor.equals(method.desc))
            {
                return method;
            }
        }
        return fail("Missing method " + classNode.name + "." + name + descriptor);
    }

    private static ClassNode readClass(byte[] bytecode)
    {
        ClassNode classNode = new ClassNode();
        new ClassReader(bytecode).accept(classNode, 0);
        return classNode;
    }

    private static boolean callsGetStackTrace(MethodNode method)
    {
        for (AbstractInsnNode instruction : method.instructions)
        {
            if (instruction instanceof MethodInsnNode)
            {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if ("getStackTrace".equals(call.name)
                        && "()[Ljava/lang/StackTraceElement;".equals(call.desc))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> loadedStrings(MethodNode method)
    {
        List<String> strings = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions)
        {
            if (instruction instanceof LdcInsnNode && ((LdcInsnNode) instruction).cst instanceof String)
            {
                strings.add((String) ((LdcInsnNode) instruction).cst);
            }
        }
        return strings;
    }

    private static List<Integer> executableOpcodes(MethodNode method)
    {
        List<Integer> opcodes = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions)
        {
            if (instruction.getOpcode() >= 0)
            {
                opcodes.add(instruction.getOpcode());
            }
        }
        return opcodes;
    }

    private static boolean containsCallStackBootstrapHandle(byte[] bytecode)
    {
        for (MethodNode method : readClass(bytecode).methods)
        {
            for (AbstractInsnNode instruction : method.instructions)
            {
                if (instruction instanceof InvokeDynamicInsnNode)
                {
                    for (Object argument : ((InvokeDynamicInsnNode) instruction).bsmArgs)
                    {
                        if (containsCallStackHandle(argument))
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean containsCallStackHandle(Object value)
    {
        if (value instanceof Handle)
        {
            Handle handle = (Handle) value;
            return handle.getTag() == Opcodes.H_INVOKESTATIC
                    && "client".equals(handle.getOwner())
                    && "qa".equals(handle.getName())
                    && STRING_METHOD_DESCRIPTOR.equals(handle.getDesc());
        }
        if (value instanceof ConstantDynamic)
        {
            ConstantDynamic dynamic = (ConstantDynamic) value;
            for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++)
            {
                if (containsCallStackHandle(dynamic.getBootstrapMethodArgument(i)))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasOpcode(MethodNode method, int opcode)
    {
        return executableOpcodes(method).contains(opcode);
    }

    private static boolean loadsLong(MethodNode method, long expected)
    {
        for (AbstractInsnNode instruction : method.instructions)
        {
            if (instruction instanceof LdcInsnNode
                    && Long.valueOf(expected).equals(((LdcInsnNode) instruction).cst))
            {
                return true;
            }
        }
        return false;
    }
}
