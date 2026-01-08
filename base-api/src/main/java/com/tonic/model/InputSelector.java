package com.tonic.model;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.util.ReflectBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * InputSelector - Provides enable/disable input buttons for the client toolbar.
 * Similar to Microbot's implementation, this adds navigation buttons to toggle client input.
 */
public class InputSelector
{
    private static final BufferedImage ENABLED_IMAGE;
    private static final BufferedImage DISABLED_IMAGE;

    static
    {
        // Load icon images
        ENABLED_IMAGE = loadImageResource("enabled_small.png");
        DISABLED_IMAGE = loadImageResource("disabled_small.png");
    }

    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private Object enableButton;
    private Object disableButton;

    public InputSelector()
    {
        startUp();
    }

    public void startUp()
    {
        try
        {
            // Create enable button
            enableButton = NavButton.builder()
                    .tooltip("Enable Input")
                    .icon(ENABLED_IMAGE)
                    .onClick(this::enableClick)
                    .priority(100)
                    .addToNavigation();

            // Create disable button
            disableButton = NavButton.builder()
                    .tooltip("Disable Input")
                    .icon(DISABLED_IMAGE)
                    .onClick(this::disableClick)
                    .priority(100)
                    .addToNavigation();

            // Initialize button visibility based on current state
            updateButtons();
        }
        catch (Exception e)
        {
            Logger.error("Failed to initialize InputSelector", e);
        }
    }

    public void shutDown()
    {
        try
        {
            RLClientUI clientUI = Static.getRuneLite().getClientUI();
            if (enableButton != null)
            {
                clientUI.removeNavigation(enableButton);
            }
            if (disableButton != null)
            {
                clientUI.removeNavigation(disableButton);
            }
        }
        catch (Exception e)
        {
            Logger.error("Failed to shutdown InputSelector", e);
        }
    }

    private void enableClick()
    {
        try
        {
            Logger.info("Enabling input");
            setClientInputEnabled(true);
            enabled.set(true);
            updateButtons();
        }
        catch (Exception e)
        {
            Logger.error("Failed to enable input", e);
        }
    }

    private void disableClick()
    {
        try
        {
            Logger.info("Disabling input");
            setClientInputEnabled(false);
            enabled.set(false);
            updateButtons();
        }
        catch (Exception e)
        {
            Logger.error("Failed to disable input", e);
        }
    }

    private void updateButtons()
    {
        SwingUtilities.invokeLater(() ->
        {
            try
            {
                RLClientUI clientUI = Static.getRuneLite().getClientUI();

                // Remove both buttons first
                if (enableButton != null)
                {
                    clientUI.removeNavigation(enableButton);
                }
                if (disableButton != null)
                {
                    clientUI.removeNavigation(disableButton);
                }

                // Add the appropriate button based on current state
                if (!enabled.get())
                {
                    // Input is disabled, show enable button
                    if (enableButton != null)
                    {
                        clientUI.addNavigation(enableButton);
                    }
                }
                else
                {
                    // Input is enabled, show disable button
                    if (disableButton != null)
                    {
                        clientUI.addNavigation(disableButton);
                    }
                }
            }
            catch (Exception e)
            {
                Logger.error("Failed to update buttons", e);
            }
        });
    }

    private void setClientInputEnabled(boolean enable)
    {
        SwingUtilities.invokeLater(() ->
        {
            try
            {
                // Set canvas focusable state - this is the main way to enable/disable input
                Canvas canvas = Static.getRuneLite().getGameApplet().getCanvas();
                if (canvas != null)
                {
                    canvas.setFocusable(enable);
                    if (enable)
                    {
                        canvas.requestFocus();
                    }
                }

                // Also set enabled state on the client component if available
                Object client = Static.getClient();
                if (client instanceof Component)
                {
                    ((Component) client).setEnabled(enable);
                }

                Logger.info("Client input " + (enable ? "enabled" : "disabled"));
            }
            catch (Exception e)
            {
                Logger.error("Failed to set client input enabled state", e);
            }
        });
    }

    public boolean isEnabled()
    {
        return enabled.get();
    }

    private static BufferedImage loadImageResource(String path)
    {
        try
        {
            java.net.URL url = InputSelector.class.getClassLoader().getResource(path);
            if (url == null)
            {
                Logger.warn("Could not find image resource: {}", path);
                return createDefaultIcon();
            }
            return javax.imageio.ImageIO.read(url);
        }
        catch (Exception e)
        {
            Logger.warn("Failed to load image resource: {}", path, e);
            return createDefaultIcon();
        }
    }

    private static BufferedImage createDefaultIcon()
    {
        // Create a simple default icon
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.GRAY);
        g2d.fillRect(0, 0, 16, 16);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(2, 2, 12, 12);
        g2d.dispose();
        return image;
    }
}
