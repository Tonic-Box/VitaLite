package com.tonic.api.game;

import com.tonic.Static;
import com.tonic.data.Tab;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

/**
 * ClientScript API
 */
public class ClientScriptAPI
{
    /**
     * Closes the numeric input dialogue if it is open
     */
    public static void closeNumericInputDialogue()
    {
        Client client = Static.getClient();
        Static.invoke(() -> {
            Widget w = client.getWidget(WidgetInfo.CHATBOX_INPUT);
            Widget w2 = client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT);
            if(w != null || w2 != null)
            {
                client.runScript(138);
            }
        });
    }


    /**
     * Switches to the specified tab
     * @param tab The tab index to switch to
     */
    public static void switchTabs(int tab)
    {
        Client client = Static.getClient();
        Static.invoke(() -> client.runScript(915, tab));
    }

    /**
     * Switches to the specified tab
     * @param tab The tab to switch to
     */
    public static void switchTabs(Tab tab)
    {
        Client client = Static.getClient();
        Static.invoke(() -> client.runScript(915, tab.getTabVarbit()));
    }

    /**
     * Runs a client script with the specified id and arguments

     * @param args The script id followed by arguments
     */
    public static void runScript(Object... args)
    {
        Client client = Static.getClient();
        Static.invoke(() -> client.runScript(args));
    }
}
