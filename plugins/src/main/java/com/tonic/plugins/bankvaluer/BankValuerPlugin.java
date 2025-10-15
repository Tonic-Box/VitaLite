package com.tonic.plugins.bankvaluer;

import com.tonic.Static;
import com.tonic.api.threaded.Delays;
import com.tonic.events.BankCacheChanged;
import com.tonic.util.ThreadPool;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@PluginDescriptor(
        name = "# Bank Valuer",
        description = "Shows you where the value in your bank is.",
        tags = {"bank", "value", "valuer", "prices", "items", "worth"}
)
public class BankValuerPlugin extends Plugin
{
    @Inject
    private ClientToolbar clientToolbar;
    private BankValuerPanel panel;
    private NavigationButton navButton;
    @Override
    protected void startUp() throws Exception
    {
        panel = injector.getInstance(BankValuerPanel.class);

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "money.png");

        navButton = NavigationButton.builder()
                .tooltip("Bank Valuer")
                .icon(icon)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception
    {
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            ThreadPool.submit(panel::refresh);
        }
    }

    @Subscribe
    public void onBankCacheChanged(BankCacheChanged event)
    {
        ThreadPool.submit(panel::refresh);
    }
}
