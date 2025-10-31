package com.tonic.injector.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates and stores binary patches for modified bytecode.
 * Integrates with Injector to capture diffs during transformation.
 */
public class PatchGenerator {
    private static final Map<String, byte[]> originalGamepack = new HashMap<>();
    private static final Map<String, byte[]> originalRunelite = new HashMap<>();
    private static final Map<String, byte[]> gamepackDiffs = new HashMap<>();
    private static final Map<String, byte[]> runeliteDiffs = new HashMap<>();

    private static boolean captureEnabled = false;

    /**
     * Enable patch capture mode. Must be called before injection starts.
     */
    public static void enableCapture() {
        captureEnabled = true;
        originalGamepack.clear();
        originalRunelite.clear();
        gamepackDiffs.clear();
        runeliteDiffs.clear();
        System.out.println("[PatchGenerator] Capture enabled - will generate diffs");
    }

    /**
     * Disable patch capture mode.
     */
    public static void disableCapture() {
        captureEnabled = false;
    }

    /**
     * Check if capture is enabled.
     */
    public static boolean isCaptureEnabled() {
        return captureEnabled;
    }

    /**
     * Store original gamepack bytecode before modification.
     *
     * @param className The class name
     * @param bytecode The original bytecode
     */
    public static void storeOriginalGamepack(String className, byte[] bytecode) {
        if (!captureEnabled) return;
        originalGamepack.put(className, bytecode);
    }

    /**
     * Store original runelite bytecode before modification.
     *
     * @param className The class name
     * @param bytecode The original bytecode
     */
    public static void storeOriginalRunelite(String className, byte[] bytecode) {
        if (!captureEnabled) return;
        originalRunelite.put(className, bytecode);
    }

    /**
     * Capture modified gamepack bytecode and generate diff.
     *
     * @param className The class name
     * @param modified The modified bytecode
     */
    public static void captureGamepackDiff(String className, byte[] modified) {
        if (!captureEnabled) return;

        byte[] original = originalGamepack.get(className);
        if (original == null) {
            System.err.println("[PatchGenerator] Warning: No original for " + className);
            return;
        }

        // Only create diff if bytecode actually changed
        if (!BytecodePatcher.isIdentical(original, modified)) {
            byte[] diff = BytecodePatcher.createDiff(original, modified);
            gamepackDiffs.put(className, diff);

            //double ratio = BytecodePatcher.estimateCompressionRatio(original, modified);
            //System.out.println("[PatchGenerator] Gamepack: " + className +
            //    " (diff: " + diff.length + " bytes, ratio: " + String.format("%.1f%%", ratio * 100) + ")");
        }
    }

    /**
     * Capture modified runelite bytecode and generate diff.
     *
     * @param className The class name
     * @param modified The modified bytecode
     */
    public static void captureRuneliteDiff(String className, byte[] modified) {
        if (!captureEnabled) return;

        byte[] original = originalRunelite.get(className);
        if (original == null) {
            System.err.println("[PatchGenerator] Warning: No original for " + className);
            return;
        }

        // Only create diff if bytecode actually changed
        if (!BytecodePatcher.isIdentical(original, modified)) {
            byte[] diff = BytecodePatcher.createDiff(original, modified);
            runeliteDiffs.put(className, diff);

            //double ratio = BytecodePatcher.estimateCompressionRatio(original, modified);
            //System.out.println("[PatchGenerator] RuneLite: " + className +
            //    " (diff: " + diff.length + " bytes, ratio: " + String.format("%.1f%%", ratio * 100) + ")");
        }
    }

    /**
     * Write all captured diffs to a zip file in resources.
     * Deletes existing zip if present.
     *
     * @param resourcesPath Path to main module's resources directory
     * @throws Exception if writing fails
     */
    public static void writePatchesZip(String resourcesPath) throws Exception {
        if (!captureEnabled) {
            System.out.println("[PatchGenerator] Capture disabled, skipping zip generation");
            return;
        }

        // Create resources/vitalite directory if needed
        Path vitaliteDir = Paths.get(resourcesPath, "com", "tonic");
        Files.createDirectories(vitaliteDir);

        // Delete old zip if exists
        Path zipPath = vitaliteDir.resolve("patches.zip");
        if (Files.exists(zipPath)) {
            Files.delete(zipPath);
            System.out.println("[PatchGenerator] Deleted old patches.zip");
        }

        // Create new zip with all diffs
        try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // Write gamepack diffs
            for (Map.Entry<String, byte[]> entry : gamepackDiffs.entrySet()) {
                String className = entry.getKey();
                byte[] diff = entry.getValue();

                ZipEntry zipEntry = new ZipEntry("gamepack/" + className.replace('.', '/') + ".diff");
                zos.putNextEntry(zipEntry);
                zos.write(diff);
                zos.closeEntry();
            }

            // Write runelite diffs
            for (Map.Entry<String, byte[]> entry : runeliteDiffs.entrySet()) {
                String className = entry.getKey();
                byte[] diff = entry.getValue();

                ZipEntry zipEntry = new ZipEntry("runelite/" + className.replace('.', '/') + ".diff");
                zos.putNextEntry(zipEntry);
                zos.write(diff);
                zos.closeEntry();
            }

            // Write metadata
            String metadata = generateMetadata();
            ZipEntry metaEntry = new ZipEntry("metadata.properties");
            zos.putNextEntry(metaEntry);
            zos.write(metadata.getBytes());
            zos.closeEntry();
        }

        long zipSize = Files.size(zipPath);
        System.out.println("[PatchGenerator] ✓ Wrote patches.zip: " + zipSize + " bytes");
        System.out.println("[PatchGenerator]   - Gamepack diffs: " + gamepackDiffs.size());
        System.out.println("[PatchGenerator]   - RuneLite diffs: " + runeliteDiffs.size());
        System.out.println("[PatchGenerator]   - Location: " + zipPath.toAbsolutePath());
    }

    /**
     * Generate metadata properties for the patch zip.
     */
    private static String generateMetadata() {
        StringBuilder sb = new StringBuilder();
        sb.append("# VitaLite Binary Patches\n");
        sb.append("# Generated: ").append(System.currentTimeMillis()).append("\n");
        sb.append("gamepack.patches=").append(gamepackDiffs.size()).append("\n");
        sb.append("runelite.patches=").append(runeliteDiffs.size()).append("\n");

        // Calculate total diff size
        long totalSize = 0;
        for (byte[] diff : gamepackDiffs.values()) {
            totalSize += diff.length;
        }
        for (byte[] diff : runeliteDiffs.values()) {
            totalSize += diff.length;
        }
        sb.append("total.size=").append(totalSize).append("\n");

        return sb.toString();
    }

    /**
     * Get statistics about captured patches.
     */
    public static String getStatistics() {
        if (!captureEnabled) {
            return "Capture disabled";
        }

        long totalDiffSize = 0;
        for (byte[] diff : gamepackDiffs.values()) {
            totalDiffSize += diff.length;
        }
        for (byte[] diff : runeliteDiffs.values()) {
            totalDiffSize += diff.length;
        }

        return String.format("Gamepack: %d patches, RuneLite: %d patches, Total: %d bytes",
            gamepackDiffs.size(), runeliteDiffs.size(), totalDiffSize);
    }
}
