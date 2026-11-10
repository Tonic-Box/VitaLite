package com.tonic.services.hub;

import java.util.ArrayList;
import java.util.List;

/**
 * One plugin a repository offers: what it is, the jar to download and the hash it must have, the oldest VitaLite it runs
 * on and the plugins it requires.
 */
public final class HubPlugin {
    public String id;
    public String name;
    public String author;
    public String description;
    public List<String> tags = new ArrayList<>();
    public String version;
    public String minVita;
    public List<String> requires = new ArrayList<>();
    public String url;
    public String sha256;
    public long size;
    public String source;
    public String readme;
    public String icon;
    public String iconSha256;

    /** @return its name, or its id when the manifest gives none */
    public String displayName() {
        return name == null || name.isEmpty() ? id : name;
    }

    /** @return the plugins it requires, never null */
    public List<String> required() {
        return requires == null ? List.of() : requires;
    }
}
