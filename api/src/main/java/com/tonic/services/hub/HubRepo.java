package com.tonic.services.hub;

import java.util.Objects;

/**
 * A plugin repository the Hub reads: the address of its plugins.json, whether it is the signed official repository, and
 * whether it is switched on.
 */
public final class HubRepo {
    public static final String OFFICIAL_URL = "https://raw.githubusercontent.com/Tonic-Box/VitaLitePlugins/HEAD/plugins.json";

    public String url;
    public boolean official;
    public boolean enabled = true;

    HubRepo(String url, boolean official) {
        this.url = url;
        this.official = official;
    }

    /**
     * Turns what a user typed into the address of a plugins.json: {@code owner/repo} becomes the GitHub raw address of
     * the repository's default branch, a URL to a folder gets plugins.json appended, and a URL to a json file is kept.
     *
     * @param input owner/repo, or an https or file URL
     * @return the address of the repository's plugins.json
     * @throws IllegalArgumentException if it is neither
     */
    public static String resolve(String input) {
        String text = input == null ? "" : input.trim();
        if (text.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            return "https://raw.githubusercontent.com/" + text + "/HEAD/plugins.json";
        }
        String github = "https://github.com/";
        if (text.startsWith(github) && text.substring(github.length()).replaceAll("/+$", "").matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            return resolve(text.substring(github.length()).replaceAll("/+$", ""));
        }
        if (text.startsWith("https://") || text.startsWith("file:")) {
            return text.endsWith(".json") ? text : text.replaceAll("/+$", "") + "/plugins.json";
        }
        throw new IllegalArgumentException("Enter owner/repo or an https address of a plugins.json");
    }

    /** @return a short name for lists: owner/repo for GitHub repositories, else the address */
    public String label() {
        String prefix = "https://raw.githubusercontent.com/";
        if (url != null && url.startsWith(prefix)) {
            String[] parts = url.substring(prefix.length()).split("/");
            if (parts.length >= 2) {
                return parts[0] + "/" + parts[1];
            }
        }
        return url;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof HubRepo && Objects.equals(((HubRepo) other).url, url);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url);
    }
}
