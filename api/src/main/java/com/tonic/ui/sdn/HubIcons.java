package com.tonic.ui.sdn;

import com.tonic.Logger;
import com.tonic.services.hub.HubFetcher;
import com.tonic.services.hub.HubPlugin;
import com.tonic.services.hub.HubState;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plugin icons for the Hub's cards: downloaded once in the background, checked against the hash the manifest gives,
 * cached on disk, and the Vita icon until then or when a plugin has none.
 */
final class HubIcons {
    private static final long MAX_BYTES = 512 * 1024;
    private static final Path CACHE = HubState.DIR.resolve("cache").resolve("icons");
    private static final Map<String, ImageIcon> ICONS = new ConcurrentHashMap<>();
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Vita Hub icons");
        thread.setDaemon(true);
        return thread;
    });
    private static ImageIcon fallback;

    private HubIcons() {
    }

    /**
     * @param listener called, off the Swing thread, when an icon has arrived
     */
    static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    /**
     * @param plugin a plugin a repository offers
     * @return its icon when it has arrived, otherwise the Vita icon; the first call starts the download
     */
    static ImageIcon of(HubPlugin plugin) {
        if (plugin == null || plugin.icon == null || plugin.icon.isEmpty()) {
            return fallback();
        }
        String key = key(plugin);
        ImageIcon icon = ICONS.get(key);
        if (icon != null) {
            return icon;
        }
        if (REQUESTED.add(key)) {
            WORKER.submit(() -> fetch(plugin, key));
        }
        return fallback();
    }

    /** @return the icon of plugins that have none of their own */
    static ImageIcon fallback() {
        if (fallback == null) {
            BufferedImage image = null;
            try {
                image = ImageUtil.loadImageResource(HubIcons.class, "/com/tonic/vitalite/icon-small.png");
            } catch (RuntimeException ignored) {
            }
            if (image == null) {
                image = ImageUtil.loadImageResource(PluginPanel.class, "/net/runelite/client/plugins/config/pluginhub_missingicon.png");
            }
            fallback = HubTheme.icon(image, HubTheme.ICON_SIZE, null);
        }
        return fallback;
    }

    private static void fetch(HubPlugin plugin, String key) {
        Path file = CACHE.resolve(key + ".png");
        try {
            if (!Files.isRegularFile(file) || !matches(file, plugin.iconSha256)) {
                Path temporary = CACHE.resolve(key + ".part");
                HubFetcher.download(plugin.icon, temporary);
                if (Files.size(temporary) > MAX_BYTES || !matches(temporary, plugin.iconSha256)) {
                    Files.deleteIfExists(temporary);
                    Logger.error("Vita Hub: the icon of " + plugin.id + " did not match its published hash");
                    return;
                }
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) {
                return;
            }
            ICONS.put(key, HubTheme.icon(image, HubTheme.ICON_SIZE, null));
            for (Runnable listener : LISTENERS) {
                listener.run();
            }
        } catch (Exception e) {
            Logger.error("Vita Hub: could not load the icon of " + plugin.id + ": " + e.getMessage());
        }
    }

    private static boolean matches(Path file, String sha256) throws Exception {
        return sha256 == null || sha256.isEmpty() || HubFetcher.sha256(file).equalsIgnoreCase(sha256);
    }

    private static String key(HubPlugin plugin) {
        if (plugin.iconSha256 != null && plugin.iconSha256.matches("[0-9a-fA-F]{64}")) {
            return plugin.iconSha256.toLowerCase();
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(plugin.icon.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int index = 0; index < 16; index++) {
                hex.append(String.format("%02x", digest[index]));
            }
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(plugin.icon.hashCode());
        }
    }
}
