package com.tonic.services.streamer;

import com.tonic.Logger;
import com.tonic.Static;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;

/**
 * Turns streamer mode on and off: the mirror window, the frames handed to it and the title bar button that toggles it. Nothing of it exists while it is off.
 */
public final class StreamerMode
{
    private static final int BUTTON_PRIORITY = 101;
    private static final BufferedImage ICON_ON = ClientUI.ICON_16;
    private static final BufferedImage ICON_OFF = ImageUtil.grayscaleImage(ClientUI.ICON_16);

    private static NavigationButton button;
    private static StreamerMirrorWindow window;

    private StreamerMode()
    {
    }

    /**
     * Makes streamer mode available: registers how settings switch it and, once RuneLite's window is built, adds the title bar button.
     */
    public static void install()
    {
        Streamer.setSwitcher(StreamerMode::set);
        MirrorMenu.install();
        MirrorChat.install();
        MirrorNameText.install();
        StreamerPresence.install();
        StreamerName.setFake(Static.getVitaConfig().getStreamerName());
        Streamer.frameRate = Static.getVitaConfig().getStreamerFrameRate().getFramesPerSecond();
        Streamer.whenUiReady(() -> SwingUtilities.invokeLater(() -> updateButton(Streamer.active)));
    }

    /**
     * Turns streamer mode on or off, on the event dispatch thread.
     *
     * @param on whether it should be on
     */
    public static void set(boolean on)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            apply(on);
        }
        else
        {
            SwingUtilities.invokeLater(() -> apply(on));
        }
    }

    private static void apply(boolean on)
    {
        if (on == Streamer.active)
        {
            return;
        }

        if (on)
        {
            try
            {
                window = new StreamerMirrorWindow();
                Static.invoke(StreamerMode::resetDrawState);
                MirrorFrames.open(window::frameReady);
                Streamer.active = true;
                HdMirror.start();
                MirrorChat.start();
                MirrorNames.start();
                window.open();
            }
            catch (RuntimeException e)
            {
                Logger.error("Streamer mode could not start: " + e);
                stop();
                on = false;
            }
        }
        else
        {
            stop();
        }
        SidebarOrder.apply(on);
        updateButton(on);
        Streamer.changed(on);
    }

    private static void stop()
    {
        Streamer.active = false;
        Static.invoke(StreamerMode::resetDrawState);
        MirrorChat.stop();
        MirrorNames.stop();
        MirrorFrames.close();
        if (window != null)
        {
            window.close();
            window = null;
        }
    }

    private static void resetDrawState()
    {
        Streamer.mirroring = false;
        MirrorHiding.reset();
        MirrorOverlays.reset();
        MirrorWorldMap.reset();
        MirrorMenu.reset();
    }

    private static void updateButton(boolean on)
    {
        ClientToolbar toolbar = Static.getInjector().getInstance(ClientToolbar.class);
        if (button != null)
        {
            toolbar.removeNavigation(button);
        }
        button = NavigationButton.builder()
                .tooltip(on ? "Streamer mode: on" : "Streamer mode: off")
                .icon(on ? ICON_ON : ICON_OFF)
                .priority(BUTTON_PRIORITY)
                .onClick(() -> set(!Streamer.active))
                .build();
        toolbar.addNavigation(button);
    }
}
