package com.tonic.headless;

import com.tonic.Logger;
import com.tonic.util.ReflectBuilder;
import javax.swing.*;
import java.awt.*;

public class HeadlessMode {
    private static final Object clientUI;
    private static final JPanel clientPanel;
    private static final JFrame frame;
    private static final JTabbedPane sidebar;
    private static RestoreSize clientPanelSize;

    static
    {
        clientUI = ReflectBuilder.runelite()
                .staticField("rlInstance")
                .field("clientUI")
                .get();

        clientPanel = ReflectBuilder.of(clientUI)
                .field("clientPanel")
                .get();

        frame = ReflectBuilder.of(clientUI)
                .field("frame")
                .get();

        sidebar = ReflectBuilder.of(clientUI)
                .field("sidebar")
                .get();
    }

    public static void toggleHeadless(boolean headless) {
        if (clientUI == null || clientPanel == null || frame == null || sidebar == null) {
            return;
        }

        clientPanel.setVisible(!headless);
        if(headless)
        {

            if(!sidebar.isVisible() || sidebar.getSelectedIndex() < 0)
            {
                ReflectBuilder.of(clientUI)
                        .method("togglePluginPanel", null, null);
            }
            clientPanelSize = new RestoreSize(clientPanel);
            clientPanelSize.hide(clientPanel);
        }
        else
        {
            clientPanelSize.restore(clientPanel);
        }

        frame.pack();
        Dimension minSize = frame.getLayout().minimumLayoutSize(frame);
        frame.setMinimumSize(minSize);

        Logger.info("Headless mode turned " + (headless ? "on" : "off"));
    }
}
