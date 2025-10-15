package com.tonic.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class JdkVersionUtil {

    private static final String JDK_URL = "https://raw.githubusercontent.com/runelite/launcher/master/.jdk-versions.sh";
    private static final Path JRE_PATH = Path.of(System.getProperty("user.home"), "AppData", "Local", "RuneLite", "jre");

    public static String calculateJreVersion() {
        Path releaseFile = JRE_PATH.resolve("release");

        if (!Files.exists(releaseFile)) {
            return extractJavaVersionForCurrentSystem();
        }

        try (Stream<String> lines = Files.lines(releaseFile)) {
            return lines
                    .filter(line -> line.startsWith("JAVA_VERSION="))
                    .findFirst()
                    .map(line -> line.split("=")[1].replace("\"", "").trim())
                    .orElse(extractJavaVersionForCurrentSystem());
        } catch (IOException e) {
            return extractJavaVersionForCurrentSystem();
        }
    }

    public enum Platform {
        WIN64("WIN64_VERSION"),
        WIN32("WIN32_VERSION"),
        WIN_AARCH64("WIN_AARCH64_VERSION"),
        MAC_AMD64("MAC_AMD64_VERSION"),
        MAC_AARCH64("MAC_AARCH64_VERSION"),
        LINUX_AMD64("LINUX_AMD64_VERSION"),
        LINUX_AARCH64("LINUX_AARCH64_VERSION");

        private final String versionKey;

        Platform(String versionKey) {
            this.versionKey = versionKey;
        }

        public String getVersionKey() {
            return versionKey;
        }
    }

    /**
     * Detects the current system platform and extracts the appropriate Java version.
     *
     * @return The Java version string for the current platform, or null if unable to determine
     */
    private static String extractJavaVersionForCurrentSystem() {
        Platform platform = detectCurrentPlatform();
        if (platform == null) {
            System.err.println("Unable to determine current platform");
            return null;
        }

        System.out.println("Detected platform: " + platform);
        return extractJavaVersion(platform);
    }

    /**
     * Detects the current platform based on system properties.
     *
     * @return The detected Platform enum value, or null if unable to determine
     */
    private static Platform detectCurrentPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        // Normalize architecture names
        String arch = normalizeArchitecture(osArch);

        if (osName.contains("win")) {
            // Windows
            switch (arch) {
                case "amd64":
                case "x86_64":
                    return Platform.WIN64;
                case "x86":
                case "i386":
                case "i686":
                    return Platform.WIN32;
                case "aarch64":
                case "arm64":
                    return Platform.WIN_AARCH64;
                default:
                    // Default to WIN64 for unknown Windows architectures
                    System.err.println("Unknown Windows architecture: " + osArch + ", defaulting to WIN64");
                    return Platform.WIN64;
            }
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            // macOS
            switch (arch) {
                case "amd64":
                case "x86_64":
                    return Platform.MAC_AMD64;
                case "aarch64":
                case "arm64":
                    return Platform.MAC_AARCH64;
                default:
                    // Check if running on Apple Silicon with Rosetta
                    if (isAppleSilicon()) {
                        return Platform.MAC_AARCH64;
                    }
                    // Default to MAC_AMD64 for unknown Mac architectures
                    System.err.println("Unknown Mac architecture: " + osArch + ", defaulting to MAC_AMD64");
                    return Platform.MAC_AMD64;
            }
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            // Linux/Unix
            switch (arch) {
                case "amd64":
                case "x86_64":
                    return Platform.LINUX_AMD64;
                case "aarch64":
                case "arm64":
                    return Platform.LINUX_AARCH64;
                default:
                    // Default to LINUX_AMD64 for unknown Linux architectures
                    System.err.println("Unknown Linux architecture: " + osArch + ", defaulting to LINUX_AMD64");
                    return Platform.LINUX_AMD64;
            }
        } else {
            System.err.println("Unknown operating system: " + osName);
            return null;
        }
    }

    /**
     * Normalizes architecture names to a common format.
     *
     * @param arch The architecture string from system properties
     * @return Normalized architecture string
     */
    private static String normalizeArchitecture(String arch) {
        // AMD64/x86_64 variations
        if (arch.contains("amd64") || arch.contains("x86_64") || arch.contains("x64")) {
            return "amd64";
        }
        // 32-bit x86 variations
        if (arch.contains("x86") || arch.contains("i386") || arch.contains("i486")
                || arch.contains("i586") || arch.contains("i686")) {
            return "x86";
        }
        // ARM64 variations
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "aarch64";
        }

        return arch;
    }

    /**
     * Checks if running on Apple Silicon (M1/M2/M3 chips).
     * This is a heuristic check for cases where the JVM might report x86_64 when running under Rosetta.
     *
     * @return true if likely running on Apple Silicon
     */
    private static boolean isAppleSilicon() {
        try {
            // Check for Apple Silicon specific system property
            String osArchData = System.getProperty("os.arch.data.model");
            if (osArchData != null && osArchData.equals("64")) {
                // Additional check: try to detect if running native or under Rosetta
                ProcessBuilder pb = new ProcessBuilder("sysctl", "-n", "hw.optional.arm64");
                Process process = pb.start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    return "1".equals(line);
                }
            }
        } catch (Exception e) {
            // Silently ignore - not critical for operation
        }
        return false;
    }

    /**
     * Fetches and extracts the Java version for a specific platform.
     *
     * @param platform The platform to get the version for
     * @return The Java version string without build number, or null if unable to fetch/parse
     */
    public static String extractJavaVersion(Platform platform) {
        Map<String, String> versions = fetchAllVersions();
        return versions.get(platform.getVersionKey());
    }

    /**
     * Fetches all platform versions from the JDK versions file.
     *
     * @return Map of platform version keys to version strings (without build numbers)
     */
    private static Map<String, String> fetchAllVersions() {
        Map<String, String> versions = new HashMap<>();
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(JDK_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed to fetch JDK versions: HTTP " + responseCode);
                return versions;
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Look for VERSION= lines
                if (line.contains("_VERSION=")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String versionWithBuild = parts[1].trim();

                        // Remove the build number (everything after and including the '+')
                        int plusIndex = versionWithBuild.indexOf('+');
                        String version = plusIndex != -1
                                ? versionWithBuild.substring(0, plusIndex)
                                : versionWithBuild;

                        versions.put(key, version);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error fetching JDK versions: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        return versions;
    }
}