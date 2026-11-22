package com.tonic.injector.util;

import com.tonic.vitalite.Main;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The classes the client's class loader serves, read as bytes and never loaded, so the injector can check what injected code refers to without defining those classes here.
 */
public final class ClientClasses
{
    private static final Map<String, ClassNode> READ = new HashMap<>();

    private ClientClasses()
    {
    }

    /**
     * @param internalName a class's internal name
     * @return the class's members without code, or null when the client's class loader has no such class
     */
    public static ClassNode get(String internalName)
    {
        if (READ.containsKey(internalName))
        {
            return READ.get(internalName);
        }
        ClassNode node = read(internalName);
        READ.put(internalName, node);
        return node;
    }

    private static ClassNode read(String internalName)
    {
        try (InputStream in = Main.CLASSLOADER.getResourceAsStream(internalName + ".class"))
        {
            if (in == null)
            {
                return null;
            }
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
        catch (IOException e)
        {
            throw new IllegalStateException("could not read " + internalName + ": " + e, e);
        }
    }
}
