package com.tonic.services.streamer;

import net.runelite.discord.DiscordEventHandlers;
import net.runelite.discord.DiscordRPC;
import net.runelite.discord.DiscordRichPresence;

/**
 * RuneLite's Discord presence, shown only while streamer mode is on: Discord is connected when the mode turns on, given the latest presence, and disconnected when it turns off.
 */
public final class StreamerPresence
{
    private static DiscordRPC rpc;
    private static String appId;
    private static DiscordEventHandlers handlers;
    private static boolean autoRegister;
    private static String steamId;
    private static DiscordRichPresence latest;
    private static boolean connected;

    private StreamerPresence()
    {
    }

    /**
     * Connects and disconnects Discord as streamer mode turns on and off.
     */
    static void install()
    {
        Streamer.addListener(StreamerPresence::changed);
    }

    /**
     * Keeps RuneLite's connection to Discord, connecting now if streamer mode is on.
     *
     * @param library the Discord library
     * @param application RuneLite's Discord application id
     * @param callbacks RuneLite's Discord callbacks
     * @param register whether Discord registers the application
     * @param steam the Steam id, if any
     */
    public static synchronized void initialize(DiscordRPC library, String application, DiscordEventHandlers callbacks, boolean register, String steam)
    {
        rpc = library;
        appId = application;
        handlers = callbacks;
        autoRegister = register;
        steamId = steam;
        if (Streamer.active)
        {
            connect();
        }
    }

    /**
     * Keeps the latest presence, sending it while connected.
     *
     * @param library the Discord library
     * @param presence the presence
     */
    public static synchronized void update(DiscordRPC library, DiscordRichPresence presence)
    {
        latest = presence;
        if (connected)
        {
            library.Discord_UpdatePresence(presence);
        }
    }

    /**
     * Drops the latest presence, clearing it while connected.
     *
     * @param library the Discord library
     */
    public static synchronized void clear(DiscordRPC library)
    {
        latest = null;
        if (connected)
        {
            library.Discord_ClearPresence();
        }
    }

    private static synchronized void changed(boolean on)
    {
        if (on)
        {
            connect();
        }
        else if (connected)
        {
            rpc.Discord_Shutdown();
            connected = false;
        }
    }

    private static void connect()
    {
        if (connected || rpc == null)
        {
            return;
        }

        rpc.Discord_Initialize(appId, handlers, autoRegister, steamId);
        connected = true;
        if (latest != null)
        {
            rpc.Discord_UpdatePresence(latest);
        }
    }
}
