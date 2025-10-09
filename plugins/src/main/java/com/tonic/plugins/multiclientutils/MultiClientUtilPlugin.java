package com.tonic.plugins.multiclientutils;

import com.google.inject.Provides;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.plugins.multiclientutils.dispatchers.CommandDispatcher;
import com.tonic.plugins.multiclientutils.dispatchers.ExtendedMenuDispatcher;
import com.tonic.plugins.multiclientutils.model.MultiMessage;
import com.tonic.plugins.multiclientutils.ui.MultiClientPanel;
import com.tonic.services.ipc.Channel;
import com.tonic.services.ipc.ChannelBuilder;
import com.tonic.services.ipc.Message;
import com.tonic.services.ipc.MessageHandler;
import com.tonic.util.VitaPlugin;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;


@PluginDescriptor(
        name = "# Multi-Client Util",
        description = "Multi-Client tooling",
        tags = {"multi", "qol", "ipc"}
)
@Getter
public class MultiClientUtilPlugin extends VitaPlugin
{
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private MultiClientConfig config;
    private MultiClientPanel panel;
    private NavigationButton navButton;
    private Channel channel;
    @Setter
    private volatile boolean extendedMenus = false;

    @Override
    protected void startUp() throws Exception
    {
        panel = injector.getInstance(MultiClientPanel.class);

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

        navButton = NavigationButton.builder()
                .tooltip("Multi-Client Utils")
                .icon(icon)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        Client client = Static.getClient();
        if(client != null && (client.getGameState() == GameState.LOGGED_IN || client.getGameState() == GameState.LOADING))
        {
            channel = createChannel();
        }
    }

    @Provides
    MultiClientConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MultiClientConfig.class);
    }

    @Override
    protected void shutDown() throws Exception
    {
        clientToolbar.removeNavigation(navButton);
        if(channel != null)
        {
            channel.stop();
            channel = null;
            Logger.info("[IPC] Channel destroyed...");
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        GameState gameState = event.getGameState();
        switch (gameState)
        {
            case LOGGED_IN:
            case LOADING:
                break;
            default:
                if(channel == null)
                    break;
                sendMessage("DESPAWN");
                channel.stop();
                channel = null;
                Logger.info("[IPC] Channel destroyed...");
                break;
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded entry) {
        if(!extendedMenus)
            return;
        ExtendedMenuDispatcher.process(this, entry);
    }

    @Override
    public void loop() throws Exception
    {
        if(channel == null)
        {
            channel = createChannel();
            return;
        }

        sendMessage("PING");
    }

    public void sendMessage(String command, Object... args)
    {
        if(channel == null || !channel.isRunning())
            return;

        Client client = Static.getClient();

        Map<String,Object> argMap = new HashMap<>();
        argMap.put("command", command);
        argMap.put("sender", client.getLocalPlayer().getName());
        for(int i = 0; i < args.length; i++)
        {
            argMap.put("arg" + i, args[i].toString());
        }

        channel.broadcast("greeting", argMap);
    }

    private Channel createChannel()
    {
        Client client = Static.getClient();
        String name = client.getLocalPlayer().getName();
        if(name == null || name.isEmpty())
        {
            Logger.error("Waiting...");
            return null;
        }
        Channel channel = new ChannelBuilder(client.getLocalPlayer().getName())
                .port(13337)
                .build();

        channel.addHandler(new MessageHandler() {
            @Override
            public void onMessage(Message message) {
                if(message.isFromSender(channel.getClientId()))
                    return;
                MultiMessage mm = new MultiMessage(message);
                CommandDispatcher.process(mm);
            }

            @Override
            public void onError(Throwable error) {
                Logger.error(error);
                error.printStackTrace();
            }
        });

        channel.start();
        Logger.info("[IPC] Channel created!");
        return channel;
    }
}
