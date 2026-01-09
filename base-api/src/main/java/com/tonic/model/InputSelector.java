package com.tonic.model;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.util.ResourceUtil;
import javax.swing.SwingUtilities;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.image.BufferedImage;

public class InputSelector
{
    private static final BufferedImage ENABLED = ResourceUtil.getImage(InputSelector.class, "/enabled_small.png");
    private static final BufferedImage DISABLED = ResourceUtil.getImage(InputSelector.class, "/disabled_small.png");

    private volatile boolean enabled = true;
    private Object enableBtn, disableBtn;

    public InputSelector() { startUp(); }

    public void startUp()
    {
        enableBtn = NavButton.builder().icon(ENABLED).tooltip("Enable Input").onClick(() -> set(true)).priority(100).addToNavigation();
        disableBtn = NavButton.builder().icon(DISABLED).tooltip("Disable Input").onClick(() -> set(false)).priority(100).addToNavigation();
        update();
    }

    public void shutDown()
    {
        RLClientUI ui = Static.getRuneLite().getClientUI();
        if (enableBtn != null) ui.removeNavigation(enableBtn);
        if (disableBtn != null) ui.removeNavigation(disableBtn);
    }

    private void set(boolean state)
    {
        enabled = state;
        Logger.info((state ? "Enabling" : "Disabling") + " input");
        SwingUtilities.invokeLater(() -> { updateInput(); update(); });
    }

    private void update()
    {
        RLClientUI ui = Static.getRuneLite().getClientUI();
        if (enableBtn != null) ui.removeNavigation(enableBtn);
        if (disableBtn != null) ui.removeNavigation(disableBtn);
        ui.addNavigation(enabled ? disableBtn : enableBtn);
    }

    private void updateInput()
    {
        Canvas canvas = Static.getRuneLite().getGameApplet().getCanvas();
        if (canvas != null) { canvas.setFocusable(enabled); if (enabled) canvas.requestFocus(); }
        Object client = Static.getClient();
        if (client instanceof Component) ((Component) client).setEnabled(enabled);
    }

    public boolean isEnabled() { return enabled; }
}
