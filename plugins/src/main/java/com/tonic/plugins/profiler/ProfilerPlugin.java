package com.tonic.plugins.profiler;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@PluginDescriptor(
        name = "Profiler",
        description = "JVM Profiler for performance monitoring and analysis",
        tags = {"profiler", "jvm", "performance", "monitoring"},
        enabledByDefault = true
)
public class ProfilerPlugin extends Plugin {
    @Inject
    private ClientToolbar clientToolbar;

    private ProfilerPanel panel;
    private final BufferedImage iconImage = ImageUtil.loadImageResource(ProfilerPlugin.class, "icon.png");
    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception {
        panel = new ProfilerPanel();
        navButton = NavigationButton.builder()
                .tooltip("Profiler")
                .icon(iconImage)
                .priority(5)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(navButton);
        panel = null;
    }
}
