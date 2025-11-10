package com.tonic.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LauncherVersionUtil {

    private static final String DEFAULT_VERSION = RuneliteConfigUtil.getRuneLiteVersion();
    private static final String PROPERTIES_PATH = "net/runelite/launcher/launcher.properties";
    private static final String VERSION_KEY = "runelite.launcher.version=";

    /**
     * Gets the RuneLite launcher version from the installed JAR file.
     *
     * @return The version string (e.g., "2.7.1") or "0.0.0" if not found
     */
    public static String getLauncherVersion() {
        Path jarPath = getRuneLiteJarPath();

        if (!Files.exists(jarPath)) {
            return RuneliteConfigUtil.getLauncherVersion();
        }

        return extractVersionFromJar(jarPath);
    }

    /**
     * Constructs the path to the RuneLite JAR file based on the operating system.
     *
     * @return Path to the RuneLite.jar file
     */
    private static Path getRuneLiteJarPath() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("mac")) {
            return Paths.get("/Applications/RuneLite.app/Contents/Resources/RuneLite.jar");
        } else if (osName.contains("win")) {
            return getWindowsRuneLitePath();
        } else {
            return Paths.get("");
        }
    }

    /**
     * Gets the RuneLite path for Windows systems.
     *
     * @return Path to the RuneLite.jar file on Windows
     */
    private static Path getWindowsRuneLitePath() {
        String appData = System.getenv("LOCALAPPDATA");
        if (appData == null) {
            appData = System.getenv("APPDATA");
            if (appData != null) {
                Path appDataPath = Paths.get(appData).getParent();
                if (appDataPath != null) {
                    appData = appDataPath.resolve("Local").toString();
                }
            }
        }

        if (appData == null) {
            return Paths.get("");
        }

        return Paths.get(appData, "RuneLite", "RuneLite.jar");
    }

    /**
     * Extracts the version string from the RuneLite JAR file.
     *
     * @param jarPath Path to the JAR file
     * @return The version string or "0.0.0" if extraction fails
     */
    private static String extractVersionFromJar(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(PROPERTIES_PATH);

            if (entry == null) {
                return DEFAULT_VERSION;
            }

            try (InputStream is = jarFile.getInputStream(entry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith(VERSION_KEY)) {
                        return line.substring(VERSION_KEY.length()).trim();
                    }
                }
            }
        } catch (IOException e) {
            // Log error if needed, but return default version
            System.err.println("Failed to read RuneLite version: " + e.getMessage());
        }

        return DEFAULT_VERSION;
    }

    /**
     * Gets the detected operating system type.
     *
     * @return A string describing the OS (Windows, macOS, Linux, or Unknown)
     */
    public static String getOperatingSystem() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return "Windows";
        } else if (osName.contains("mac")) {
            return "macOS";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return "Linux";
        } else {
            return "Unknown";
        }
    }
}