package com.tonic.plugins.profiler;

import com.tonic.model.ui.ProfilerAccessor;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
        name = "# Profiler",
        description = "JVM Profiler for performance monitoring and analysis. Access via Debug > Profiler button in Settings.",
        tags = {"profiler", "jvm", "performance", "monitoring"},
        enabledByDefault = true
)
public class ProfilerPlugin extends Plugin {
    
    @Override
    protected void startUp() throws Exception {
        // Register the profiler toggle callback so VitaLiteOptionsPanel can access it
        ProfilerAccessor.registerToggleCallback(ProfilerWindow::toggle);
    }

    @Override
    protected void shutDown() throws Exception {
        // Unregister the callback when plugin is disabled
        ProfilerAccessor.unregisterToggleCallback();
    }
}
