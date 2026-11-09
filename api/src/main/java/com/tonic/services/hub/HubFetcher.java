package com.tonic.services.hub;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

/**
 * Downloads repositories' manifests and plugin jars. The official manifest must carry a valid signature and a sequence no
 * older than the newest seen before; every manifest that passes is cached, so the Hub still lists plugins offline.
 */
public final class HubFetcher {
    private static final Path CACHE = HubState.DIR.resolve("cache");
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** A repository's manifest as fetched, or why it could not be. */
    public static final class Result {
        public final HubRepo repo;
        public final HubManifest manifest;
        public final String error;
        public final boolean cached;

        Result(HubRepo repo, HubManifest manifest, String error, boolean cached) {
            this.repo = repo;
            this.manifest = manifest;
            this.error = error;
            this.cached = cached;
        }
    }

    private HubFetcher() {
    }

    /**
     * @param repo a repository
     * @return its manifest, freshly fetched or, when that fails, the last good copy with the reason it was not refreshed
     */
    public static Result fetch(HubRepo repo) {
        String failure;
        try {
            byte[] bytes = download(repo.url);
            String signature = repo.official ? new String(download(repo.url + ".sig"), StandardCharsets.US_ASCII) : null;
            HubManifest manifest = check(repo, bytes, signature, true);
            cache(repo, bytes, signature);
            return new Result(repo, manifest, null, false);
        } catch (IOException | InterruptedException e) {
            failure = describe(e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (IllegalStateException e) {
            failure = e.getMessage();
        }
        try {
            Path file = cacheFile(repo, ".json");
            if (Files.isRegularFile(file)) {
                Path signatureFile = cacheFile(repo, ".sig");
                String signature = Files.isRegularFile(signatureFile) ? Files.readString(signatureFile, StandardCharsets.US_ASCII) : null;
                HubManifest manifest = check(repo, Files.readAllBytes(file), signature, false);
                return new Result(repo, manifest, failure + " (showing the last good copy)", true);
            }
        } catch (IOException | IllegalStateException ignored) {
        }
        return new Result(repo, null, failure, false);
    }

    private static String describe(Exception e) {
        String message = e.getMessage();
        if (message != null && message.startsWith("HTTP 404")) {
            return "no plugins.json there (HTTP 404)";
        }
        if (message != null && message.startsWith("HTTP ")) {
            return "the server answered " + message;
        }
        return "couldn't connect" + (message == null || message.isEmpty() ? "" : " (" + message + ")");
    }

    private static HubManifest check(HubRepo repo, byte[] bytes, String signature, boolean fresh) {
        if (repo.official && !HubSignature.official(bytes, signature)) {
            throw new IllegalStateException("the official manifest's signature is not valid; it was not used");
        }
        HubManifest manifest;
        try {
            manifest = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), HubManifest.class);
        } catch (RuntimeException e) {
            throw new IllegalStateException("plugins.json is not valid: " + e.getMessage());
        }
        if (manifest == null || manifest.plugins == null) {
            throw new IllegalStateException("plugins.json lists no plugins");
        }
        manifest.plugins.removeIf(plugin -> plugin == null || plugin.id == null || plugin.url == null || plugin.sha256 == null || plugin.version == null);
        if (repo.official) {
            HubState state = HubState.get();
            synchronized (state) {
                long newest = state.sequences.getOrDefault(repo.url, 0L);
                if (manifest.sequence < newest) {
                    throw new IllegalStateException("the official manifest is older than one already seen; it was not used");
                }
                if (fresh && manifest.sequence > newest) {
                    state.sequences.put(repo.url, manifest.sequence);
                    state.save();
                }
            }
        }
        return manifest;
    }

    /**
     * @param url  an https or file address
     * @param file where to write what it holds
     * @throws IOException          if it cannot be downloaded or written
     * @throws InterruptedException if the download is interrupted
     */
    public static void download(String url, Path file) throws IOException, InterruptedException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        if (url.startsWith("file:")) {
            Files.copy(Paths.get(URI.create(url)), file, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        HttpResponse<InputStream> response = HTTP.send(request(url), HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("HTTP " + response.statusCode());
        }
        try (InputStream in = response.body()) {
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static byte[] download(String url) throws IOException, InterruptedException {
        if (url.startsWith("file:")) {
            return Files.readAllBytes(Paths.get(URI.create(url)));
        }
        HttpResponse<byte[]> response = HTTP.send(request(url), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "VitaLite-Hub")
                .header("Cache-Control", "no-cache")
                .build();
    }

    private static void cache(HubRepo repo, byte[] bytes, String signature) {
        try {
            Files.createDirectories(CACHE);
            Files.write(cacheFile(repo, ".json"), bytes);
            if (signature != null) {
                Files.writeString(cacheFile(repo, ".sig"), signature, StandardCharsets.US_ASCII);
            }
        } catch (IOException ignored) {
        }
    }

    private static Path cacheFile(HubRepo repo, String extension) {
        return CACHE.resolve(sha256(repo.url.getBytes(StandardCharsets.UTF_8)).substring(0, 16) + extension);
    }

    /**
     * @param file a file
     * @return its SHA-256, lowercase hex
     * @throws IOException if it cannot be read
     */
    public static String sha256(Path file) throws IOException {
        MessageDigest digest = digest();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[65536];
            for (int read = in.read(buffer); read > 0; read = in.read(buffer)) {
                digest.update(buffer, 0, read);
            }
        }
        return hex(digest.digest());
    }

    private static String sha256(byte[] data) {
        return hex(digest().digest(data));
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
