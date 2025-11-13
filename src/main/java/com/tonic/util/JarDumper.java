package com.tonic.util;


import com.tonic.Static;
import com.tonic.vitalite.Main;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.jar.*;

public class JarDumper
{
    public static void dump(HashMap<String, byte[]> classes) throws IOException {
        String outputPath = Static.getCliArgs().getRsdump();
        if(outputPath == null)
            return;
        ensureFolders(outputPath);
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputPath))) {
            for (HashMap.Entry<String, byte[]> entry : classes.entrySet()) {
                String className = entry.getKey();
                byte[] classBytes = entry.getValue();
                String entryName = className.replace('.', '/') + ".class";
                jos.putNextEntry(new JarEntry(entryName));
                jos.write(classBytes);
                jos.closeEntry();
            }
        }
        System.out.println("Dumped " + classes.size() + " classes to " + outputPath);
    }

    public static void ensureFolders(String filePath) throws IOException
    {
        Path path = Paths.get(filePath).toAbsolutePath();
        Path dir = Files.isDirectory(path) ? path : path.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
    }
}
