package com.tonic.vitalite;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tonic.VitaLite;
import com.tonic.util.RuneliteConfigUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public final class Versioning
{
    private Versioning() {
        // Utility class - prevent instantiation
    }

    public static String getVitaLiteVersion()
    {
        try {
            final Manifest manifest = new Manifest(VitaLite.class.getClassLoader()
                    .getResourceAsStream("META-INF/MANIFEST.MF"));
            final Attributes attrs = manifest.getMainAttributes();
            final String version = attrs.getValue("Implementation-Version");
            if (version == null)
            {
                System.out.println("Could not find manifest, assuming dev environment");
                return RuneliteConfigUtil.getTagValueFromURL("release");
            }
            return version;
        } catch (final IOException e) {
            return "UNKNOWN";
        }
    }

    public static String getLiveRuneliteVersion()
    {
        return RuneliteConfigUtil.getTagValueFromURL("release");
    }

    public static boolean isRunningFromShadedJar() {
        final String jarPath = VitaLite.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();
        return jarPath.contains("shaded") || jarPath.endsWith(".jar");
    }

    /**
     * Fetches the latest VitaLite release tag from GitHub API.
     * @return the latest release tag as a string
     * @throws IOException if the API request fails
     */
    public static String getLatestVitaLiteReleaseTag() throws IOException {
        final String apiUrl = "https://api.github.com/repos/Tonic-Box/VitaLite/releases/latest";
        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", "VitaLite-Versioning/1.0")
                    .header("Accept", "application/vnd.github.v3+json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            final HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("GitHub API request failed: HTTP " + response.statusCode());
            }

            final JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return json.get("tag_name").getAsString();

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request was interrupted", e);
        }
    }
}
