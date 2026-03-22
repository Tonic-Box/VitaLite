package com.tonic.plugins.walkassistant;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("walkassistant")
public interface WalkAssistantConfig extends Config
{
    @ConfigItem(
        keyName = "questDestHotkey",
        name = "Walk to Quest/Clue Destination",
        description = "Walks to the active Quest Helper step or Clue Scroll destination. Requires Quest Helper or the ClueScroll plugin to be active.",
        position = 0
    )
    default Keybind questDestHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "cancelWalkHotkey",
        name = "Cancel Walk",
        description = "Immediately cancels any active Walk Assistant path.",
        position = 1
    )
    default Keybind cancelWalkHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "nearestBankHotkey",
        name = "Walk to Nearest Bank",
        description = "Walks to the nearest accessible bank, respecting quest and skill access requirements.",
        position = 2
    )
    default Keybind nearestBankHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "debugLogging",
        name = "Debug Logging",
        description = "Print debug messages to the console when resolving destinations and walking.",
        position = 3
    )
    default boolean debugLogging()
    {
        return false;
    }
}
