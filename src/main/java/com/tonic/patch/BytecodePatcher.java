package com.tonic.patch;

import io.sigpipe.jbsdiff.Diff;
import io.sigpipe.jbsdiff.Patch;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Static API for creating and applying delta diffs to bytecode.
 * Uses jbsdiff for efficient binary delta compression.
 */
public class BytecodePatcher {

    // ===== Delta Diff Operations =====

    /**
     * Creates a delta diff between original and modified bytecode using jbsdiff.
     * The diff can be applied to the original to reconstruct the modified version.
     *
     * @param original The original bytecode (before modification)
     * @param modified The modified bytecode (after modification)
     * @return A delta diff that can be applied to original to get modified
     * @throws RuntimeException if diff creation fails
     */
    public static byte[] createDiff(byte[] original, byte[] modified) {
        try {
            ByteArrayOutputStream patchStream = new ByteArrayOutputStream();
            Diff.diff(original, modified, patchStream);
            return patchStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create diff", e);
        }
    }

    /**
     * Applies a delta diff to original bytecode to reconstruct modified version using jbsdiff.
     *
     * @param original The original bytecode
     * @param diff The delta diff created by createDiff()
     * @return The reconstructed modified bytecode
     * @throws RuntimeException if patch application fails
     */
    public static byte[] applyDiff(byte[] original, byte[] diff) {
        try {
            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            Patch.patch(original, diff, resultStream);
            return resultStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply diff", e);
        }
    }

    // ===== Full Replacement Operations (Simple Alternative) =====

    /**
     * Creates a full replacement patch (just stores the modified bytecode).
     * Simpler than delta diff but uses more storage.
     *
     * @param modified The modified bytecode to store
     * @return A patch containing the full modified bytecode
     */
    public static byte[] createFullPatch(byte[] modified) {
        // TODO: Implement full patch creation (may add metadata header)
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Applies a full replacement patch (no original needed).
     *
     * @param patch The full patch created by createFullPatch()
     * @return The modified bytecode
     */
    public static byte[] applyFullPatch(byte[] patch) {
        // TODO: Implement full patch application
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // ===== Validation and Hashing =====

    /**
     * Computes a hash of bytecode for validation and version checking.
     * Used to verify patches are applied to the correct original bytecode.
     *
     * @param bytecode The bytecode to hash
     * @return Hex string representation of SHA-256 hash
     */
    public static String computeHash(byte[] bytecode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytecode);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }

    /**
     * Validates that applying a diff to original produces the expected result.
     *
     * @param original The original bytecode
     * @param diff The delta diff
     * @param expected The expected modified bytecode
     * @return true if applying diff to original produces expected, false otherwise
     */
    public static boolean validateDiff(byte[] original, byte[] diff, byte[] expected) {
        try {
            byte[] result = applyDiff(original, diff);
            return Arrays.equals(result, expected);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates that a full patch produces the expected result.
     *
     * @param patch The full patch
     * @param expected The expected modified bytecode
     * @return true if applying patch produces expected, false otherwise
     */
    public static boolean validateFullPatch(byte[] patch, byte[] expected) {
        try {
            byte[] result = applyFullPatch(patch);
            return Arrays.equals(result, expected);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Computes a hash for multiple bytecode files (e.g., entire gamepack).
     * Useful for validating entire artifact versions.
     *
     * @param bytecodes Array of bytecode arrays to hash together
     * @return Hex string representation of combined hash
     */
    public static String computeCombinedHash(byte[]... bytecodes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (byte[] bytecode : bytecodes) {
                digest.update(bytecode);
            }
            byte[] hash = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute combined hash", e);
        }
    }

    // ===== Utility Methods =====

    /**
     * Estimates the size reduction from using delta diff vs full replacement.
     *
     * @param original Original bytecode
     * @param modified Modified bytecode
     * @return Compression ratio (0.0 to 1.0, lower is better).
     *         For example, 0.2 means diff is 20% the size of the modified bytecode.
     */
    public static double estimateCompressionRatio(byte[] original, byte[] modified) {
        try {
            byte[] diff = createDiff(original, modified);
            return (double) diff.length / (double) modified.length;
        } catch (Exception e) {
            // If diff creation fails, assume no compression
            return 1.0;
        }
    }

    /**
     * Checks if two bytecode arrays are identical.
     *
     * @param a First bytecode
     * @param b Second bytecode
     * @return true if identical, false otherwise
     */
    public static boolean isIdentical(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }
}
