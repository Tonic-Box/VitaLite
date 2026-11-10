package com.tonic.api.game;

import com.tonic.Static;
import com.tonic.data.Tab;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;

/**
 * ClientScript API
 */
public class ClientScriptAPI
{
    private static final int SCRIPT_CLOSE_INTERFACE = 29;
    private static final int SCRIPT_CLOSE_CHATBOX_INPUT = 138;
    private static final int SCRIPT_SWITCH_TAB = 915;

    /**
     * Closes the currently open interface
     */
    public static void closeInterface()
    {
        runScript(SCRIPT_CLOSE_INTERFACE);
    }

    /**
     * Closes the numeric input dialogue if it is open
     */
    public static void closeNumericInputDialogue()
    {
        Client client = Static.getClient();
        Static.invoke(() -> {
            Widget w = client.getWidget(InterfaceID.Chatbox.INPUT);
            Widget w2 = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
            if(w != null || w2 != null)
            {
                ClientScriptAPI.runScript(SCRIPT_CLOSE_CHATBOX_INPUT);
            }
        });
    }


    /**
     * Switches to the specified tab
     * @param tab The tab index to switch to
     */
    public static void switchTabs(int tab)
    {
        runScript(SCRIPT_SWITCH_TAB, tab);
    }

    /**
     * Switches to the specified tab
     * @param tab The tab to switch to
     */
    public static void switchTabs(Tab tab)
    {
        runScript(SCRIPT_SWITCH_TAB, tab.getTabVarbit());
    }

    /**
     * Runs a client script with the specified id and arguments

     * @param args The script id followed by arguments
     */
    public static void runScript(Object... args)
    {
        Client client = Static.getClient();
        Static.invoke(() -> client.createScriptEventBuilder(args).build().setCanSendPackets(true).run());
    }
}
