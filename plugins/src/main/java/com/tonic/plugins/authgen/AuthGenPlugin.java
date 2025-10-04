package com.tonic.plugins.authgen;

import com.google.inject.Inject;
import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import java.util.Timer;
import java.util.TimerTask;

@PluginDescriptor(
        name = "# Authenticator Gen",
        description = "Simple, phone-free way to register 2FA for osrs accounts",
        tags = {"totp", "auth"}
)
public class AuthGenPlugin extends Plugin
{
    @Inject
    private AuthGenConfig config;
    private Timer timer;

    @Provides
    AuthGenConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AuthGenConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        config.setSecret("");
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(config.secret().isEmpty())
                    return;
                config.setCode(TOTP.getCode(config.secret()));
            }
        }, 0, 1000);
    }

    @Override
    protected void shutDown() throws Exception
    {
        if(timer != null)
            timer.cancel();
        timer = null;
    }
}
