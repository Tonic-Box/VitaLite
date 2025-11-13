package com.tonic;

import com.tonic.util.AudioDeviceChecker;
import com.tonic.util.optionsparser.OptionsParser;
import com.tonic.util.optionsparser.annotations.CLIArgument;
import lombok.Getter;

@Getter
public class VitaLiteOptions extends OptionsParser
{
    @CLIArgument(
            name = "rsdump",
            description = "[Optional] Path to dump the gamepack to"
    )
    private String rsdump = null;

    @CLIArgument(
            name = "noPlugins",
            description = "[Optional] Disables loading of core plugins"
    )
    private boolean noPlugins = false;

    @CLIArgument(
            name = "incognito",
            description = "[Optional] Visually display as 'RuneLite' instead of 'VitaLite'"
    )
    private boolean incognito = false;

    @CLIArgument(
            name = "safeLaunch",
            description = ""
    )
    private boolean safeLaunch = false;

    @CLIArgument(
            name = "min",
            description = "Run with minimum memory on jvm (auto enables also -noPlugins and -noMusic)"
    )
    private boolean min = false;

    @CLIArgument(
            name = "noMusic",
            description = "Prevent the loading of music tracks"
    )
    private boolean noMusic = false;

    @CLIArgument(
            name = "proxy",
            description = "Set a proxy server to use (e.g., ip:port or ip:port:username:password)"
    )
    private String proxy = null;

    @CLIArgument(
            name = "launcherCom",
            description = ""
    )
    private String port;

    @CLIArgument(
            name = "disableMouseHook",
            description = "Disable RuneLites mousehook DLL from being loaded or called"
    )
    private boolean disableMouseHook = false;

    @CLIArgument(
            name = "legacyLogin",
            description = "details for logging int (user:pass)"
    )
    private String legacyLogin = null;

    @CLIArgument(
            name = "jagexLogin",
            description = "details for logging int (sessionID:characterID:displayName) or path to runelite credentials file"
    )
    private String jagexLogin = null;

    @CLIArgument(
            name = "runInjector",
            description = "For use with developing mixins to runt he injector on launch"
    )
    private boolean runInjector = false;

    @CLIArgument(
            name = "targetBootstrap",
            description = "Bootstrap a specific runelite version"
    )
    private String targetBootstrap = null;

    @CLIArgument(
            name = "world",
            description = "Set the world"
    )
    private int world = -1;

    public void _checkAudio()
    {
        if(!AudioDeviceChecker.hasAudioDevice())
        {
            noMusic = true;
        }
    }
}
