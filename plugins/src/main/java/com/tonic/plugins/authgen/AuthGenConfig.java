package com.tonic.plugins.authgen;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("authgen")
public interface AuthGenConfig extends Config {
    @ConfigItem(
            name = "Auth Secret",
            keyName = "secret",
            description = "",
            position = 0
    )
    default String secret()
    {
        return "";
    }

    @ConfigItem(
            name = "Auth Code",
            keyName = "code",
            description = "",
            position = 2
    )
    default String code()
    {
        return "";
    }

    @ConfigItem(
            name = "",
            keyName = "code",
            description = "",
            position = 3
    )
    default void setCode(String code)
    {
    }

    @ConfigItem(
            name = "",
            keyName = "secret",
            description = "",
            position = 4
    )
    default void setSecret(String secret)
    {
    }
}