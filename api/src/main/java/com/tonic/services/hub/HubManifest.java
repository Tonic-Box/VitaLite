package com.tonic.services.hub;

import java.util.ArrayList;
import java.util.List;

/**
 * A plugin repository's plugins.json: its name, a sequence that only grows from one publish to the next, and its plugins.
 */
public final class HubManifest {
    public String name;
    public long sequence;
    public List<HubPlugin> plugins = new ArrayList<>();

    /**
     * @param id a plugin id
     * @return the repository's plugin with that id, or null
     */
    public HubPlugin plugin(String id) {
        if (plugins == null) {
            return null;
        }
        for (HubPlugin plugin : plugins) {
            if (id.equals(plugin.id)) {
                return plugin;
            }
        }
        return null;
    }
}
