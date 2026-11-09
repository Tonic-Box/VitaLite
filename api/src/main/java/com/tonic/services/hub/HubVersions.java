package com.tonic.services.hub;

import com.tonic.Static;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

/**
 * Version strings as plugins and VitaLite use them: numbers separated by dots, underscores or dashes, compared number by
 * number.
 */
public final class HubVersions {
    private static String running;
    private static boolean read;

    private HubVersions() {
    }

    /**
     * @param first  a version
     * @param second another version
     * @return negative, zero or positive as the first is older than, the same as or newer than the second
     */
    public static int compare(String first, String second) {
        String[] a = parts(first);
        String[] b = parts(second);
        for (int index = 0; index < Math.max(a.length, b.length); index++) {
            long x = index < a.length ? number(a[index]) : 0;
            long y = index < b.length ? number(b[index]) : 0;
            if (x != y) {
                return Long.compare(x, y);
            }
        }
        return 0;
    }

    /**
     * @param minimum the oldest VitaLite version a plugin runs on, or null
     * @return whether the running VitaLite is at least that version; true when either is unknown
     */
    public static boolean runs(String minimum) {
        String version = running();
        return minimum == null || minimum.isEmpty() || version == null || compare(version, minimum) >= 0;
    }

    /** @return the running VitaLite's version from its jar manifest, or null when running outside the release jar */
    public static synchronized String running() {
        if (!read) {
            read = true;
            try (InputStream in = Static.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
                running = in == null ? null : new Manifest(in).getMainAttributes().getValue("Implementation-Version");
            } catch (IOException e) {
                running = null;
            }
        }
        return running;
    }

    private static String[] parts(String version) {
        return version == null ? new String[0] : version.trim().split("[._-]");
    }

    private static long number(String part) {
        String digits = part.replaceAll("\\D.*$", "");
        return digits.isEmpty() ? 0 : Long.parseLong(digits);
    }
}
