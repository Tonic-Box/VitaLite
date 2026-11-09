package com.tonic.services.hub;

import com.tonic.Logger;
import com.tonic.services.hotswapper.PluginReloader;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * The Hub: the plugins the enabled repositories offer, and installing, updating and uninstalling them. Plugins are
 * loaded into the running client as soon as they are installed; a plugin that cannot be unloaded straight away is removed
 * at the next start. Everything runs on one worker thread, in order.
 */
public final class HubService {
    private static final HubService INSTANCE = new HubService();

    /** One plugin as the Hub lists it. */
    public static final class Entry {
        public final HubRepo repo;
        public final HubPlugin plugin;
        public final HubState.Installed installed;

        Entry(HubRepo repo, HubPlugin plugin, HubState.Installed installed) {
            this.repo = repo;
            this.plugin = plugin;
            this.installed = installed;
        }

        /** @return whether this plugin is installed from this repository */
        public boolean isInstalled() {
            return installed != null && repo.url.equals(installed.repo);
        }

        /** @return whether the repository offers a newer version than the one installed */
        public boolean hasUpdate() {
            return isInstalled() && HubVersions.compare(plugin.version, installed.version) > 0;
        }

        /** @return whether the running VitaLite is new enough for it */
        public boolean isCompatible() {
            return HubVersions.runs(plugin.minVita);
        }
    }

    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Vita Hub");
        thread.setDaemon(true);
        return thread;
    });
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, String> operations = new ConcurrentHashMap<>();
    private volatile Map<String, HubFetcher.Result> results = new LinkedHashMap<>();
    private volatile String message = "";
    private volatile String error;
    @Getter
    private volatile boolean busy;

    private HubService() {
    }

    /** @return the Hub */
    public static HubService get() {
        return INSTANCE;
    }

    /**
     * @param listener called, on the Hub's worker thread, whenever the listing, an operation or the status changes
     */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /**
     * @param listener a listener added before
     */
    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    /** @return whether any repository has been fetched since the client started */
    public boolean hasFetched() {
        return !results.isEmpty();
    }

    /** @return the installed plugins that no enabled repository lists, so they can still be uninstalled */
    public List<HubState.Installed> unlisted() {
        Set<String> listed = new LinkedHashSet<>();
        for (Entry entry : entries()) {
            if (entry.isInstalled()) {
                listed.add(entry.plugin.id);
            }
        }
        List<HubState.Installed> all;
        HubState state = HubState.get();
        synchronized (state) {
            all = new ArrayList<>(state.installed.values());
        }
        List<HubState.Installed> unlisted = new ArrayList<>();
        for (HubState.Installed installed : all) {
            if (!listed.contains(installed.id)) {
                unlisted.add(installed);
            }
        }
        return unlisted;
    }

    /**
     * @param id a plugin
     * @return what is being done to it, such as "Installing", or null when nothing is queued or running for it
     */
    public String operation(String id) {
        return operations.get(id);
    }

    /** @return why the last operation failed, or null when it did not */
    public String error() {
        return error;
    }

    /** Forgets the last failure once it has been shown and dismissed. */
    public void clearError() {
        error = null;
        notifyListeners();
    }

    /** @return the last status message */
    public String message() {
        return message;
    }

    /** @return each enabled repository's fetch result, official first */
    public List<HubFetcher.Result> results() {
        return new ArrayList<>(results.values());
    }

    /** @return every plugin the enabled repositories offer, official first */
    public List<Entry> entries() {
        HubState state = HubState.get();
        List<Entry> entries = new ArrayList<>();
        for (HubFetcher.Result result : results.values()) {
            if (result.manifest == null) {
                continue;
            }
            for (HubPlugin plugin : result.manifest.plugins) {
                HubState.Installed installed;
                synchronized (state) {
                    installed = state.installed.get(plugin.id);
                }
                entries.add(new Entry(result.repo, plugin, installed));
            }
        }
        return entries;
    }

    /** @return how many installed plugins have a newer version in their repository */
    public int updateCount() {
        int count = 0;
        for (Entry entry : entries()) {
            if (entry.hasUpdate()) {
                count++;
            }
        }
        return count;
    }

    /** Fetches every enabled repository's manifest again. */
    public void refresh() {
        submit("Refreshing", this::fetchAll);
    }

    /**
     * Adds a repository after checking that it serves a readable plugins.json.
     *
     * @param input owner/repo or an address
     */
    public void addRepo(String input) {
        submit("Adding repository", () -> {
            String url = HubRepo.resolve(input);
            HubState state = HubState.get();
            if (state.repo(url) != null) {
                throw new IllegalStateException("That repository is already added");
            }
            HubRepo repo = new HubRepo(url, false);
            HubFetcher.Result result = HubFetcher.fetch(repo);
            if (result.manifest == null) {
                throw new IllegalStateException("No usable plugins.json at " + url + ": " + result.error);
            }
            synchronized (state) {
                state.repos.add(repo);
                state.save();
            }
            fetchAll();
            message = "Added " + repo.label() + " (" + result.manifest.plugins.size() + " plugins)";
        });
    }

    /**
     * @param url a repository's address; the official repository cannot be removed
     */
    public void removeRepo(String url) {
        submit("Removing repository", () -> {
            HubState state = HubState.get();
            synchronized (state) {
                state.repos.removeIf(repo -> !repo.official && repo.url.equals(url));
                state.save();
            }
            fetchAll();
        });
    }

    /**
     * @param url     a repository's address
     * @param enabled whether the Hub reads it
     */
    public void setEnabled(String url, boolean enabled) {
        submit("Updating repositories", () -> {
            HubState state = HubState.get();
            synchronized (state) {
                HubRepo repo = state.repo(url);
                if (repo != null) {
                    repo.enabled = enabled;
                    state.save();
                }
            }
            fetchAll();
        });
    }

    /**
     * Installs a plugin and whatever it requires that is not installed yet.
     *
     * @param repoUrl the repository it is listed in
     * @param id      the plugin
     */
    public void install(String repoUrl, String id) {
        submit("Installing " + id, Map.of(id, "Installing"), () -> {
            List<HubPlugin> order = plan(id, lookup(repoUrl));
            List<String> done = new ArrayList<>();
            for (HubPlugin plugin : order) {
                if (HubState.get().installed.containsKey(plugin.id)) {
                    continue;
                }
                put(repoOf(repoUrl, plugin), plugin);
                done.add(plugin.displayName());
            }
            PluginReloader.forceRebuildPluginList();
            message = "Installed " + String.join(", ", done);
        });
    }

    /**
     * Updates an installed plugin to the version its repository offers, reloading the plugins that require it.
     *
     * @param id the plugin
     */
    public void update(String id) {
        submit("Updating " + id, Map.of(id, "Updating"), () -> {
            update(id, true);
            PluginReloader.forceRebuildPluginList();
        });
    }

    /** Updates every installed plugin that has a newer version. */
    public void updateAll() {
        Map<String, String> updating = new LinkedHashMap<>();
        for (Entry entry : entries()) {
            if (entry.hasUpdate()) {
                updating.put(entry.plugin.id, "Updating");
            }
        }
        submit("Updating all", updating, () -> {
            List<String> updated = new ArrayList<>();
            for (Entry entry : entries()) {
                if (entry.hasUpdate()) {
                    update(entry.plugin.id, false);
                    updated.add(entry.plugin.displayName());
                }
            }
            PluginReloader.forceRebuildPluginList();
            message = updated.isEmpty() ? "Everything is up to date" : "Updated " + String.join(", ", updated);
        });
    }

    /**
     * Uninstalls a plugin.
     *
     * @param id         the plugin
     * @param dependents whether to uninstall the plugins that require it too; when false and some do, nothing changes
     */
    public void uninstall(String id, boolean dependents) {
        Map<String, String> removing = new LinkedHashMap<>();
        removing.put(id, "Removing");
        if (dependents) {
            for (String dependent : dependentsOf(id)) {
                removing.put(dependent, "Removing");
            }
        }
        submit("Uninstalling " + id, removing, () -> {
            HubState state = HubState.get();
            List<String> requiring = dependents(id, state.installed.values());
            if (!requiring.isEmpty() && !dependents) {
                throw new IllegalStateException(id + " is required by " + String.join(", ", requiring));
            }
            List<String> removed = new ArrayList<>(requiring);
            removed.add(id);
            boolean later = false;
            for (String each : removed) {
                HubState.Installed installed = state.installed.get(each);
                if (installed == null) {
                    continue;
                }
                later |= !remove(installed);
                synchronized (state) {
                    state.installed.remove(each);
                    state.save();
                }
            }
            PluginReloader.forceRebuildPluginList();
            message = "Uninstalled " + String.join(", ", removed) + (later ? "; finishes when the client restarts" : "");
        });
    }

    /**
     * @param id the plugin
     * @return the installed plugins that require it, directly or through others, those deepest in the chain first
     */
    public List<String> dependentsOf(String id) {
        HubState state = HubState.get();
        synchronized (state) {
            return dependents(id, new ArrayList<>(state.installed.values()));
        }
    }

    private void update(String id, boolean report) throws Exception {
        HubState state = HubState.get();
        HubState.Installed installed = state.installed.get(id);
        if (installed == null) {
            throw new IllegalStateException(id + " is not installed");
        }
        HubPlugin plugin = lookup(installed.repo).apply(id);
        if (plugin == null || HubVersions.compare(plugin.version, installed.version) <= 0) {
            message = id + " is up to date";
            return;
        }
        for (HubPlugin required : plan(id, lookup(installed.repo))) {
            if (!required.id.equals(id) && !state.installed.containsKey(required.id)) {
                put(repoOf(installed.repo, required), required);
            }
        }
        List<String> reload = dependents(id, state.installed.values());
        for (String dependent : reload) {
            PluginReloader.unloadPlugin(new File(state.installed.get(dependent).file));
        }
        if (!PluginReloader.unloadPlugin(new File(installed.file))) {
            for (int index = reload.size() - 1; index >= 0; index--) {
                load(state.installed.get(reload.get(index)));
            }
            throw new IllegalStateException(plugin.displayName() + " would not stop, so it was not updated");
        }
        boolean later = !remove(installed);
        put(installed.repo, plugin);
        for (int index = reload.size() - 1; index >= 0; index--) {
            load(state.installed.get(reload.get(index)));
        }
        if (report) {
            message = "Updated " + plugin.displayName() + " to " + plugin.version + (later ? "; the old version is removed when the client restarts" : "");
        }
    }

    private void put(String repoUrl, HubPlugin plugin) throws Exception {
        if (!HubVersions.runs(plugin.minVita)) {
            throw new IllegalStateException(plugin.displayName() + " needs VitaLite " + plugin.minVita + " or newer");
        }
        Path temporary = HubState.DIR.resolve("download").resolve(plugin.id + ".part");
        HubFetcher.download(plugin.url, temporary);
        if (plugin.size > 0 && Files.size(temporary) != plugin.size || !HubFetcher.sha256(temporary).equalsIgnoreCase(plugin.sha256)) {
            Files.deleteIfExists(temporary);
            throw new IllegalStateException(plugin.displayName() + " did not match its published hash; nothing was installed");
        }
        Path jar = HubState.PLUGINS.resolve(plugin.id + "-" + plugin.version + ".jar");
        Files.createDirectories(jar.getParent());
        Files.move(temporary, jar, StandardCopyOption.REPLACE_EXISTING);
        HubState.Installed installed = new HubState.Installed();
        installed.id = plugin.id;
        installed.repo = repoUrl;
        installed.version = plugin.version;
        installed.sha256 = plugin.sha256;
        installed.file = jar.toAbsolutePath().toString();
        installed.requires = new ArrayList<>(plugin.required());
        HubState state = HubState.get();
        synchronized (state) {
            state.installed.put(plugin.id, installed);
            state.save();
        }
        load(installed);
    }

    private void load(HubState.Installed installed) {
        try {
            PluginReloader.loadPlugin(new File(installed.file), HubLoader.dependencyLoaders(installed), true);
        } catch (Throwable e) {
            Logger.error("Vita Hub: could not load " + installed.id + ": " + e);
            throw new IllegalStateException(installed.id + " is installed but could not be started: " + e.getMessage(), e);
        }
    }

    private boolean remove(HubState.Installed installed) {
        File jar = new File(installed.file);
        boolean unloaded = PluginReloader.unloadPlugin(jar);
        boolean deleted;
        try {
            deleted = Files.deleteIfExists(jar.toPath());
        } catch (IOException e) {
            deleted = false;
        }
        if (!deleted && jar.exists()) {
            HubState state = HubState.get();
            synchronized (state) {
                if (!state.pendingDeletes.contains(installed.file)) {
                    state.pendingDeletes.add(installed.file);
                }
                state.save();
            }
            return false;
        }
        return unloaded;
    }

    private String repoOf(String preferred, HubPlugin plugin) {
        HubFetcher.Result result = results.get(preferred);
        if (result != null && result.manifest != null && result.manifest.plugin(plugin.id) == plugin) {
            return preferred;
        }
        return HubState.get().official().url;
    }

    private Function<String, HubPlugin> lookup(String repoUrl) {
        return id -> {
            HubFetcher.Result own = results.get(repoUrl);
            HubPlugin plugin = own == null || own.manifest == null ? null : own.manifest.plugin(id);
            if (plugin == null) {
                HubFetcher.Result official = results.get(HubState.get().official().url);
                plugin = official == null || official.manifest == null ? null : official.manifest.plugin(id);
            }
            return plugin;
        };
    }

    /**
     * @param id     a plugin
     * @param lookup finds a plugin by id: its own repository first, then the official one
     * @return the plugin and everything it requires, each after what it requires
     * @throws IllegalStateException if a required plugin is missing or the requirements form a cycle
     */
    static List<HubPlugin> plan(String id, Function<String, HubPlugin> lookup) {
        List<HubPlugin> order = new ArrayList<>();
        visit(id, lookup, new LinkedHashSet<>(), new LinkedHashSet<>(), order, null);
        return order;
    }

    private static void visit(String id, Function<String, HubPlugin> lookup, Set<String> visiting, Set<String> done, List<HubPlugin> order, String requiredBy) {
        if (done.contains(id)) {
            return;
        }
        if (!visiting.add(id)) {
            throw new IllegalStateException("plugins require each other in a cycle: " + String.join(" -> ", visiting) + " -> " + id);
        }
        HubPlugin plugin = lookup.apply(id);
        if (plugin == null) {
            throw new IllegalStateException(requiredBy == null ? id + " is not offered by any enabled repository" : requiredBy + " requires " + id + ", which no enabled repository offers");
        }
        for (String required : plugin.required()) {
            visit(required, lookup, visiting, done, order, plugin.displayName());
        }
        visiting.remove(id);
        done.add(id);
        order.add(plugin);
    }

    /**
     * @param id        a plugin
     * @param installed the installed plugins
     * @return those that require it, directly or through others, deepest in the chain first
     */
    static List<String> dependents(String id, Collection<HubState.Installed> installed) {
        List<String> found = new ArrayList<>();
        collect(id, installed, found);
        List<String> deepestFirst = new ArrayList<>();
        for (int index = found.size() - 1; index >= 0; index--) {
            deepestFirst.add(found.get(index));
        }
        return deepestFirst;
    }

    private static void collect(String id, Collection<HubState.Installed> installed, List<String> found) {
        for (HubState.Installed each : installed) {
            if (each.required().contains(id) && !found.contains(each.id)) {
                found.add(each.id);
                collect(each.id, installed, found);
            }
        }
    }

    private void fetchAll() {
        Map<String, HubFetcher.Result> fetched = new LinkedHashMap<>();
        for (HubRepo repo : new ArrayList<>(HubState.get().repos)) {
            if (repo.enabled) {
                fetched.put(repo.url, HubFetcher.fetch(repo));
            }
        }
        results = fetched;
        List<String> problems = new ArrayList<>();
        for (HubFetcher.Result result : fetched.values()) {
            if (result.error != null) {
                problems.add(result.repo.label() + ": " + result.error);
            }
        }
        message = problems.isEmpty() ? "Up to date" : String.join("; ", problems);
    }

    private interface Task {
        void run() throws Exception;
    }

    private void submit(String working, Task task) {
        submit(working, Map.of(), task);
    }

    private void submit(String working, Map<String, String> affected, Task task) {
        operations.putAll(affected);
        notifyListeners();
        worker.submit(() -> {
            busy = true;
            error = null;
            message = working + "...";
            notifyListeners();
            try {
                task.run();
            } catch (Throwable e) {
                message = e.getMessage() == null ? e.toString() : e.getMessage();
                error = message;
                Logger.error("Vita Hub: " + message);
            } finally {
                operations.keySet().removeAll(affected.keySet());
                busy = false;
                notifyListeners();
            }
        });
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                Logger.error("Vita Hub listener failed: " + e.getMessage());
            }
        }
    }
}
