package com.tonic.injector.util;

import com.tonic.VitaLite;
import com.tonic.vitalite.Main;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Applies pre-generated binary patches to bytecode at runtime.
 * Used in production mode to avoid heavy ASM transformation overhead.
 */
public class PatchApplier {

    /**
     * Stream patches.zip from resources and apply diffs one-by-one to minimize memory usage.
     * This streaming approach avoids loading all patches into memory at once, reducing peak memory.
     *
     * @throws Exception if patches cannot be loaded or applied
     */
    public static void applyPatches() throws Exception {
        System.out.println("[PatchApplier] Streaming patches from resources...");

        int gamepackApplied = 0;
        int runeliteApplied = 0;

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

                    // Read diff bytes for this entry only
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] chunk = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = zis.read(chunk)) != -1) {
                        buffer.write(chunk, 0, bytesRead);
                    }
                    byte[] diffBytes = buffer.toByteArray();

                    // Parse class name from path (e.g., "gamepack/com/foo/Bar.diff" -> "com.foo.Bar")
                    String className = extractClassName(name);

                    // Apply patch immediately based on type
                    if (name.startsWith("gamepack/")) {
                        byte[] original = Main.LIBS.getGamepack().classes.get(className);
                        if (original == null) {
                            System.err.println("[PatchApplier] Warning: No original bytecode for " + className);
                        } else {
                            byte[] modified = BytecodePatcher.applyDiff(original, diffBytes);
                            Main.LIBS.getGamepack().classes.put(className, modified);
                            gamepackApplied++;
                        }
                    } else if (name.startsWith("runelite/")) {
                        byte[] original = Main.LIBS.getRunelite().classes.get(className);
                        if (original == null) {
                            System.err.println("[PatchApplier] Warning: No original bytecode for " + className);
                        } else {
                            byte[] modified = BytecodePatcher.applyDiff(original, diffBytes);
                            Main.LIBS.getRunelite().classes.put(className, modified);
                            runeliteApplied++;
                        }
                    }

                    // diffBytes will be GC'd before next iteration, keeping memory low
                    zis.closeEntry();
                }
            }
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
