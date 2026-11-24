package com.tonic.services.worldselect;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.game.WorldsAPI;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import com.tonic.util.ThreadPool;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.WorldsFetch;
import net.runelite.client.game.WorldService;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * VitaLite's login world select: opened in place of the client's, painted over the whole canvas each frame and given every mouse and key event while open.
 */
public final class WorldSelect
{
    private static final int SWITCH_WORLD_PAGE = 34;
    private static final int LOGIN_WIDTH = 765;
    private static final int LOGIN_HEIGHT = 503;
    private static final long PING_RESORT_MILLIS = 500;

    private static final WorldPinger pinger = new WorldPinger();
    private static final WorldSelectModel model = new WorldSelectModel(pinger::ping);
    private static final WorldSelectPainter painter = new WorldSelectPainter(model, pinger);
    private static final WorldSelectInput input = new WorldSelectInput(painter, model);

    private static volatile boolean clearing;
    private static volatile boolean pinged;
    private static EventBus.Subscriber fetches;
    private static boolean pingsChanged;
    private static long lastPingResort;

    static
    {
        WorldSelectBridge.onDisabled(WorldSelect::close);
    }

    private WorldSelect()
    {
    }

    /**
     * Drops the client's world select the moment it is opened and opens VitaLite's instead. Called by the client before it draws the login screen, while VitaLite's is in use.
     */
    public static void intercept()
    {
        TClient client = Static.getClient();
        if (client == null || !client.isWorldSelectOpen())
        {
            return;
        }
        client.closeWorldSelect();
        open();
    }

    /**
     * Opens the world select, unless it is open already, with the saved filters and the client's world selected. Call on the client thread.
     */
    public static synchronized void open()
    {
        if (WorldSelectBridge.open && !clearing)
        {
            return;
        }
        Client client = Static.getClient();
        clearing = false;
        pinged = false;
        model.opened(client.getWorld());
        painter.opened();
        WorldService worlds = worldService();
        fetches = Static.getInjector().getInstance(EventBus.class).register(WorldsFetch.class, fetch -> receive(fetch.getWorldResult()), 0);
        ThreadPool.submit(() -> receive(worlds.getWorlds()));
        worlds.refresh();
        input.register();
        WorldSelectBridge.open = true;
    }

    /**
     * Closes the world select without changing world, and takes the login screen back to its form when it was opened from the "switch world" page. It is painted out one frame later, when the canvas around the login screen, which the client does not redraw, is cleared.
     */
    public static synchronized void close()
    {
        if (!WorldSelectBridge.open || clearing)
        {
            return;
        }
        clearing = true;
        input.unregister();
        Static.getInjector().getInstance(EventBus.class).unregister(fetches);
        pinger.stop();
        Static.invoke(WorldSelect::leaveSwitchWorldPage);
    }

    /**
     * Changes to the selected world, when it is not the client's own, and closes the world select.
     */
    public static synchronized void confirm()
    {
        if (!WorldSelectBridge.open || clearing)
        {
            return;
        }
        World target = model.world(model.getSelected());
        if (target != null && target.getId() != model.getCurrent())
        {
            Static.invoke(() ->
            {
                Client client = Static.getClient();
                client.changeWorld(WorldsAPI.toClientWorld(target));
            });
        }
        close();
    }

    /**
     * Paints the world select over the canvas, on the layer RuneLite draws last, or clears the canvas around the login screen once it has been closed, and closes it once the client has left the login screen. Called each frame while it is open.
     *
     * @param graphics the frame's graphics
     * @param layer the RuneLite overlay layer being drawn
     */
    public static void paint(Graphics2D graphics, Object layer)
    {
        if (layer != OverlayLayer.ALWAYS_ON_TOP)
        {
            return;
        }
        Client client = Static.getClient();
        GameState state = client.getGameState();
        if (state != GameState.LOGIN_SCREEN && state != GameState.LOGIN_SCREEN_AUTHENTICATOR)
        {
            close();
            finishClosing();
            return;
        }
        if (clearing)
        {
            clearAroundLoginScreen(graphics, client.getCanvasWidth(), client.getCanvasHeight());
            finishClosing();
            return;
        }
        resortForPings();
        painter.paint(graphics, client.getCanvasWidth(), client.getCanvasHeight());
    }

    private static synchronized void receive(WorldResult result)
    {
        if (!WorldSelectBridge.open || clearing || result == null || result.getWorlds() == null)
        {
            return;
        }
        model.offer(result);
        if (!pinged)
        {
            pinged = true;
            pinger.pingAll(result.getWorlds());
        }
    }

    private static void resortForPings()
    {
        pingsChanged |= pinger.takeChanged();
        long now = System.currentTimeMillis();
        if (pingsChanged && now - lastPingResort >= PING_RESORT_MILLIS)
        {
            pingsChanged = false;
            lastPingResort = now;
            model.rebuild();
        }
    }

    private static WorldService worldService()
    {
        return Static.getInjector().getInstance(WorldService.class);
    }

    private static synchronized void finishClosing()
    {
        if (clearing)
        {
            clearing = false;
            WorldSelectBridge.open = false;
        }
    }

    private static void clearAroundLoginScreen(Graphics2D graphics, int width, int height)
    {
        int left = (width - LOGIN_WIDTH) / 2;
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, left, height);
        graphics.fillRect(left + LOGIN_WIDTH, 0, width - left - LOGIN_WIDTH, height);
        graphics.fillRect(left, LOGIN_HEIGHT, LOGIN_WIDTH, height - LOGIN_HEIGHT);
    }

    private static void leaveSwitchWorldPage()
    {
        Client client = Static.getClient();
        if (client.getLoginIndex() == SWITCH_WORLD_PAGE)
        {
            ((TClient) client).leaveWorldSelect();
        }
    }
}
