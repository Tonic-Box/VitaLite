package com.tonic.bootstrap;

import com.google.gson.Gson;
import com.tonic.bootstrap.beans.Artifact;
import com.tonic.bootstrap.beans.Bootstrap;
import com.tonic.util.HashUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static com.tonic.vitalite.Main.REPOSITORY_DIR;

public class RLUpdater
{
    private static Map<String, String> properties;
    private static HttpClient httpClient;

    public static void main(String[] args) throws Exception
    {
        run();
    }

    public static void run() throws IOException, InterruptedException, NoSuchAlgorithmException
    {
        properties = Properties.fetch();

        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest bootstrapReq = HttpRequest.newBuilder()
                .uri(URI.create(properties.get("runelite.bootstrap")))
                .header("User-Agent", "RuneLite/" + properties.get("runelite.launcher.version"))
                .GET()
                .build();

        HttpResponse<String> bootstrapRes = httpClient.send(bootstrapReq,
                HttpResponse.BodyHandlers.ofString());

        if (bootstrapRes.statusCode() != 200) {
            throw new IOException("Failed to fetch bootstrap JSON (status=" + bootstrapRes.statusCode() + ")");
        }

        Bootstrap bootstrap = new Gson().fromJson(bootstrapRes.body(), Bootstrap.class);
        Artifact[] artifacts = bootstrap.getArtifacts();

        if (!Files.exists(REPOSITORY_DIR)) {
            Files.createDirectories(REPOSITORY_DIR);
        }

        // Phase 1: Check if any artifact needs updating
        boolean needsUpdate = false;
        for (Artifact art : artifacts) {
            Path localFile = REPOSITORY_DIR.resolve(art.getName());

            if (!Files.exists(localFile)) {
                needsUpdate = true;
                System.out.println("Missing artifact: " + art.getName());
                break;
            }

            String localHash = HashUtil.computeSha256(localFile);
            if (!localHash.equalsIgnoreCase(art.getHash())) {
                needsUpdate = true;
                System.out.println("Hash mismatch for " + art.getName());
                break;
            }
        }

        // Phase 2: If update needed, nuke repository and re-download everything
        if (needsUpdate) {
            System.out.println("Updates detected, cleaning repository...");

            // Delete all files in repository directory
            if (Files.exists(REPOSITORY_DIR)) {
                Files.list(REPOSITORY_DIR).forEach(file -> {
                    try {
                        Files.delete(file);
                        System.out.println("Deleted: " + file.getFileName());
                    } catch (IOException e) {
                        System.err.println("Failed to delete " + file.getFileName() + ": " + e.getMessage());
                    }
                });
            }

            // Download all artifacts fresh
            for (Artifact art : artifacts) {
                Path localFile = REPOSITORY_DIR.resolve(art.getName());

                System.out.println("Downloading " + art.getName());
                downloadFile(art.getPath(), localFile);

                String downloadedHash = HashUtil.computeSha256(localFile);
                if (!downloadedHash.equalsIgnoreCase(art.getHash())) {
                    throw new IOException("Hash mismatch for " + art.getName()
                            + " (expected " + art.getHash()
                            + ", got " + downloadedHash + ")");
                }
            }

            System.out.println("Repository updated successfully!");
        } else {
            System.out.println("Repository is up to date!");
        }
    }

    private static void downloadFile(String url, Path destination)
            throws IOException, InterruptedException
    {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "RuneLite/" + properties.get("runelite.launcher.version"))
                .GET()
                .build();

        HttpResponse<InputStream> res = httpClient.send(req,
                HttpResponse.BodyHandlers.ofInputStream());

        if (res.statusCode() != 200) {
            throw new IOException("Failed to download " + url
                    + " (status=" + res.statusCode() + ")");
        }

        try (InputStream in = res.body();
             OutputStream out = Files.newOutputStream(destination)) {
            in.transferTo(out);
        }
    }
}
