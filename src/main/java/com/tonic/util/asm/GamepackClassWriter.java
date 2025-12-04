package com.tonic.util.asm;

import org.objectweb.asm.ClassWriter;

public class GamepackClassWriter extends ClassWriter {
    private final ClassLoader loader;

    public GamepackClassWriter(int flags, ClassLoader loader) {
        super(flags);
        this.loader = loader;
    }

    @Override
    protected String getCommonSuperClass(String t1, String t2) {
        try {
            Class<?> c1 = Class.forName(t1.replace('/','.'), false, loader);
            Class<?> c2 = Class.forName(t2.replace('/','.'), false, loader);
            if (c1.isAssignableFrom(c2)) return t1;
            if (c2.isAssignableFrom(c1)) return t2;
            while ((c1 = c1.getSuperclass()) != null)
                if (c1.isAssignableFrom(c2)) return c1.getName().replace('.', '/');
        }
        catch (ClassNotFoundException ignored) {}
        return "java/lang/Object";
    }
}