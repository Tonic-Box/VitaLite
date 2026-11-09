package com.tonic.services.hub;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tonic.Logger;
import com.tonic.Static;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the Hub remembers between runs, in ~/.runelite/vitalite/hub/state.json: the repositories, the installed plugins,
 * the newest signed sequence seen per repository, jars to delete at next start and whether the third-party warning was
 * shown.
 */
public final class HubState {
    public static final Path DIR = Static.VITA_DIR.resolve("hub");
    public static final Path PLUGINS = DIR.resolve("plugins");
    private static final Path FILE = DIR.resolve("state.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static HubState instance;

    /** A plugin the Hub installed. */
    public static final class Installed {
        public String id;
        public String repo;
        public String version;
        public String sha256;
        public String file;
        public List<String> requires = new ArrayList<>();

        /** @return the plugins it requires, never null */
        public List<String> required() {
            return requires == null ? List.of() : requires;
        }
    }

    public List<HubRepo> repos = new ArrayList<>();
    public Map<String, Installed> installed = new LinkedHashMap<>();
    public Map<String, Long> sequences = new LinkedHashMap<>();
    public List<String> pendingDeletes = new ArrayList<>();
    public boolean thirdPartyWarned;

    /** @return the state, read from disk the first time */
    public static synchronized HubState get() {
        if (instance == null) {
            instance = read();
        }
        return instance;
    }

    private static HubState read() {
        HubState state = null;
        try {
            if (Files.isRegularFile(FILE)) {
                state = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), HubState.class);
            }
        } catch (IOException | RuntimeException e) {
            Logger.error("Vita Hub: could not read " + FILE + ": " + e.getMessage());
        }
        if (state == null) {
            state = new HubState();
        }
        state.normalize();
        return state;
    }

    private void normalize() {
        if (repos == null) {
            repos = new ArrayList<>();
        }
        if (installed == null) {
            installed = new LinkedHashMap<>();
        }
        if (sequences == null) {
            sequences = new LinkedHashMap<>();
        }
        if (pendingDeletes == null) {
            pendingDeletes = new ArrayList<>();
        }
        repos.removeIf(repo -> repo == null || repo.url == null || repo.official && !HubRepo.OFFICIAL_URL.equals(repo.url));
        HubRepo official = null;
        for (HubRepo repo : repos) {
            if (HubRepo.OFFICIAL_URL.equals(repo.url)) {
                official = repo;
            }
        }
        if (official == null) {
            official = new HubRepo(HubRepo.OFFICIAL_URL, true);
        } else {
            repos.remove(official);
        }
        official.official = true;
        repos.add(0, official);
    }

    /** Writes the state to disk. */
    public synchronized void save() {
        try {
            Files.createDirectories(DIR);
            Path temporary = DIR.resolve("state.json.tmp");
            Files.writeString(temporary, GSON.toJson(this), StandardCharsets.UTF_8);
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Logger.error("Vita Hub: could not save " + FILE + ": " + e.getMessage());
        }
    }

    /**
     * @param url a repository's plugins.json address
     * @return the repository with that address, or null
     */
    public synchronized HubRepo repo(String url) {
        for (HubRepo repo : repos) {
            if (repo.url.equals(url)) {
                return repo;
            }
        }
        return null;
    }

    /** @return the official repository */
    public synchronized HubRepo official() {
        return repos.get(0);
    }
}
