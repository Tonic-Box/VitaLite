package com.tonic.injector.util;

import com.tonic.VitaLite;
import com.tonic.vitalite.Main;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Applies pre-generated binary patches to bytecode at runtime.
 * Used in production mode to avoid heavy ASM transformation overhead.
 */
public class PatchApplier {

    /**
     * Load patches.zip from resources and apply all diffs to gamepack and runelite classes.
     * This replaces the full ASM injection pipeline with lightweight patch application.
     *
     * @throws Exception if patches cannot be loaded or applied
     */
    public static void applyPatches() throws Exception {
        System.out.println("[PatchApplier] Loading patches from resources...");

        // Load patches.zip from classpath resources
        Map<String, byte[]> gamepackDiffs = new HashMap<>();
        Map<String, byte[]> runeliteDiffs = new HashMap<>();

        try (InputStream resourceStream = VitaLite.class.getResourceAsStream("patches.zip")) {
            if (resourceStream == null) {
                throw new RuntimeException("patches.zip not found in resources. Run with --runInjector to generate patches.");
            }

            try (ZipInputStream zis = new ZipInputStream(resourceStream)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();

                    // Skip metadata and directories
                    if (name.equals("metadata.properties") || entry.isDirectory()) {
                        zis.closeEntry();
                        continue;
                    }

                    // Read diff bytes
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] chunk = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = zis.read(chunk)) != -1) {
                        buffer.write(chunk, 0, bytesRead);
                    }
                    byte[] diffBytes = buffer.toByteArray();

                    // Parse class name from path (e.g., "gamepack/com/foo/Bar.diff" -> "com.foo.Bar")
                    String className = extractClassName(name);

                    // Store in appropriate map
                    if (name.startsWith("gamepack/")) {
                        gamepackDiffs.put(className, diffBytes);
                    } else if (name.startsWith("runelite/")) {
                        runeliteDiffs.put(className, diffBytes);
                    }

                    zis.closeEntry();
                }
            }
        }

        System.out.println("[PatchApplier] Loaded " + gamepackDiffs.size() + " gamepack patches, " +
                          runeliteDiffs.size() + " runelite patches");

        // Apply gamepack patches
        System.out.println("[PatchApplier] Applying gamepack patches...");
        int gamepackApplied = 0;
        for (Map.Entry<String, byte[]> entry : gamepackDiffs.entrySet()) {
            String className = entry.getKey();
            byte[] diff = entry.getValue();

            byte[] original = Main.LIBS.getGamepack().classes.get(className);
            if (original == null) {
                System.err.println("[PatchApplier] Warning: No original bytecode for " + className);
                continue;
            }

            byte[] modified = BytecodePatcher.applyDiff(original, diff);
            Main.LIBS.getGamepack().classes.put(className, modified);
            gamepackApplied++;
        }

        // Apply runelite patches
        System.out.println("[PatchApplier] Applying runelite patches...");
        int runeliteApplied = 0;
        for (Map.Entry<String, byte[]> entry : runeliteDiffs.entrySet()) {
            String className = entry.getKey();
            byte[] diff = entry.getValue();

            byte[] original = Main.LIBS.getRunelite().classes.get(className);
            if (original == null) {
                System.err.println("[PatchApplier] Warning: No original bytecode for " + className);
                continue;
            }

            byte[] modified = BytecodePatcher.applyDiff(original, diff);
            Main.LIBS.getRunelite().classes.put(className, modified);
            runeliteApplied++;
        }

        System.out.println("[PatchApplier] ✓ Applied " + gamepackApplied + " gamepack patches, " +
                          runeliteApplied + " runelite patches");
    }

    /**
     * Extract class name from zip entry path.
     * Examples:
     *   "gamepack/com/foo/Bar.diff" -> "com.foo.Bar"
     *   "runelite/net/runelite/client/RuneLite.diff" -> "net.runelite.client.RuneLite"
     */
    private static String extractClassName(String zipEntryName) {
        // Remove prefix (gamepack/ or runelite/)
        int firstSlash = zipEntryName.indexOf('/');
        String pathWithoutPrefix = zipEntryName.substring(firstSlash + 1);

        // Remove .diff extension
        String pathWithoutExtension = pathWithoutPrefix.replace(".diff", "");

        // Convert slashes to dots
        return pathWithoutExtension.replace('/', '.');
    }
}
