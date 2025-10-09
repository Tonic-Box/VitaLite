package com.tonic.plugins.codeeval;

import com.tonic.Static;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@PluginDescriptor(
        name = "# Code Eval",
        description = "Java code evaluation shell",
        tags = {"java", "code", "eval"}
)
public class CodeEvalPlugin extends Plugin
{
    @Inject
    private ClientToolbar clientToolbar;
    private final BufferedImage iconImage = ImageUtil.loadImageResource(CodeEvalFrame.class, "jshell.png");
    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception
    {
        navButton = NavigationButton.builder()
                .tooltip("JavaShell")
                .icon(iconImage)
                .onClick(() -> CodeEvalFrame.get().toggle())
                .build();
        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception
    {
        clientToolbar.removeNavigation(navButton);
    }
}
