package com.tonic.plugins.profiles;

import com.google.inject.Inject;
import com.tonic.plugins.profiles.panel.ProfilesRootPanel;
import com.tonic.plugins.profiles.session.ProfilesSession;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;


@PluginDescriptor(
        name = "# Enhanced Profiles"
)
public class ProfilesPlugin extends Plugin {

    private static final BufferedImage profilesIcon = ImageUtil.loadImageResource(ProfilesPlugin.class, "icon.png");

    @Getter
    @Inject
    private Client client;

    @Getter
    @Inject
    private ClientThread clientThread;

    @Inject
    private ClientToolbar clientToolbar;

    @Getter
    private final ProfilesSession profilesSession = ProfilesSession.getInstance();
    private NavigationButton navigationButton;

    @Override
    protected void startUp() throws Exception {
        ProfilesRootPanel panel = new ProfilesRootPanel(this);
        
        navigationButton = NavigationButton.builder()
                .icon(profilesIcon)
                .panel(panel)
                .tooltip("Profiles")
                .build();

        clientToolbar.addNavigation(navigationButton);
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(navigationButton);
    }
}
