package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.game.ClientScriptAPI;
import com.tonic.data.VarrockMuseumAnswer;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.ClickType;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

/**
 * DialogueAPI provides methods to interact with in-game dialogues, including retrieving dialogue text,
 * continuing dialogues, selecting options, and handling various dialogue types.
 */
public class DialogueAPI
{
    private static final int SCRIPT_CLOSE_MESSAGE_LAYER = 101;
    private static final net.runelite.client.config.ConfigManager configManager = Static.getInjector().getInstance(ConfigManager.class);

	/**
     * Retrieves the header of the current dialogue, which may indicate the speaker or context.
     *
     * @return The dialogue header as a String. Possible values include NPC names, "Player", "Select an Option", or "UNKNOWN".
     */
    public static String getDialogueHeader()
    {
        return Static.invoke(() -> {
            if (WidgetAPI.get(InterfaceID.ChatLeft.TEXT) != null) {
                return WidgetAPI.get(InterfaceID.ChatLeft.NAME).getText();
            }
            else if (WidgetAPI.get(InterfaceID.ChatRight.TEXT) != null) {
                return "Player";
            }
            else if (WidgetAPI.get(InterfaceID.Chatmenu.OPTIONS) != null) {
                return "Select an Option";
            }
            return "UNKNOWN";
        });
    }

    /**
     * Retrieves the main text content of the current dialogue.
     *
     * @return The dialogue text as a String. If no dialogue is present, returns an empty string.
     */
    public static String getDialogueText()
    {
        return Static.invoke(() -> {
            if (WidgetAPI.get(InterfaceID.ChatLeft.TEXT) != null) {
                return WidgetAPI.get(InterfaceID.ChatLeft.TEXT).getText();
            }
            else if (WidgetAPI.get(InterfaceID.ChatRight.TEXT) != null) {
                return WidgetAPI.get(InterfaceID.ChatRight.TEXT).getText();
            }
            else if (WidgetAPI.get(InterfaceID.Objectbox.TEXT) != null) {
                return WidgetAPI.get(InterfaceID.Objectbox.TEXT).getText();
            }
            else if (WidgetAPI.get(InterfaceID.ObjectboxDouble.TEXT) != null) {
                return WidgetAPI.get(InterfaceID.ObjectboxDouble.TEXT).getText();
            }
            else if(WidgetAPI.get(InterfaceID.Messagebox.TEXT) != null) {
                return WidgetAPI.get(InterfaceID.Messagebox.TEXT).getText();
            }
            return "";
        });
    }

    /**
     * Continues the current dialogue by clicking the "Continue" button if present.
     *
     * @return true if a continue action was performed, false otherwise.
     */
    public static boolean continueDialogue() {
        TClient client = Static.getClient();
        return Static.invoke(() -> {
            if (WidgetAPI.get(InterfaceID.ChatLeft.CONTINUE) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(InterfaceID.ChatLeft.CONTINUE, -1);
                return true;
            }
            if (WidgetAPI.get(InterfaceID.Graphicbox.UNIVERSE) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(InterfaceID.Graphicbox.UNIVERSE, -1);
                return true;
            }
            if (WidgetAPI.get(InterfaceID.ChatRight.CONTINUE) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(InterfaceID.ChatRight.CONTINUE, -1);
                return true;
            }
            if (WidgetAPI.get(InterfaceID.Objectbox.UNIVERSE) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(InterfaceID.Objectbox.UNIVERSE, 0);
                return true;
            }
            if (WidgetAPI.get(InterfaceID.ObjectboxDouble.UNIVERSE) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(InterfaceID.ObjectboxDouble.PAUSEBUTTON, -1);
                return true;
            }
            if (WidgetAPI.get(InterfaceID.LevelupDisplay.CONTINUE) != null) {
                Widget w = WidgetAPI.get(InterfaceID.LevelupDisplay.CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                {
                    client.getPacketWriter().resumePauseWidgetPacket(InterfaceID.LevelupDisplay.CONTINUE, -1);
                    return true;
                }
            }
            if(WidgetAPI.get(InterfaceID.Messagebox.CONTINUE) != null) {
                Widget w = WidgetAPI.get(InterfaceID.Messagebox.CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                {
                    client.getPacketWriter().resumePauseWidgetPacket(InterfaceID.Messagebox.CONTINUE, -1);
                    return true;
                }
            }
            if(WidgetAPI.get(InterfaceID.Chatbox.MES_TEXT2) != null) {
                Widget w = WidgetAPI.get(InterfaceID.Chatbox.MES_TEXT2);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                {
                    ClientScriptAPI.runScript(SCRIPT_CLOSE_MESSAGE_LAYER, 1);
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Checks if any dialogue is currently present on the screen.
     *
     * @return true if a dialogue is present, false otherwise.
     */
    public static boolean dialoguePresent() {
        return Static.invoke(() -> {
            if (WidgetAPI.get(InterfaceID.ChatLeft.CONTINUE) != null) {
                return true;
            }
            if (WidgetAPI.get(InterfaceID.Graphicbox.UNIVERSE) != null) {
                return true;
            }
            if (WidgetAPI.get(InterfaceID.ChatRight.CONTINUE) != null) {
                return true;
            }
            if (WidgetAPI.get(InterfaceID.Objectbox.UNIVERSE) != null) {
                return true;
            }
            if (WidgetAPI.get(InterfaceID.ObjectboxDouble.UNIVERSE) != null) {
                return true;
            }
            if(WidgetAPI.get(InterfaceID.LevelupDisplay.CONTINUE) != null) {
                Widget w = WidgetAPI.get(InterfaceID.LevelupDisplay.CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                    return true;
            }
            if(WidgetAPI.get(InterfaceID.Messagebox.CONTINUE) != null) {
                Widget w = WidgetAPI.get(InterfaceID.Messagebox.CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                    return true;
            }
            if(WidgetAPI.get(InterfaceID.Chatbox.MES_TEXT2) != null) {
                Widget w = WidgetAPI.get(InterfaceID.Chatbox.MES_TEXT2);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                    return true;
            }
            return WidgetAPI.get(InterfaceID.Chatmenu.OPTIONS) != null;
        });
    }

    /**
     * Selects a dialogue option based on its index.
     *
     * @param option The index of the option to select (0-based).
     */
    public static void selectOption(int option) {
        resumePause(InterfaceID.Chatmenu.OPTIONS, option);
    }

    /**
     * Selects a dialogue option based on its text.
     *
     * @param option The text of the option to select. Case-insensitive and partial matches are supported.
     * @return true if the option was found and selected, false otherwise.
     */
    public static boolean selectOption(String option) {

        return Static.invoke(() -> {
            Widget widget = WidgetAPI.get(InterfaceID.Chatmenu.OPTIONS);
            if(widget == null)
                return false;
            Widget[] dialogOption1kids = widget.getChildren();
            if(dialogOption1kids == null)
                return false;
            if(dialogOption1kids.length < 2)
                return false;
            int i = 0;
            for(Widget w : dialogOption1kids) {
                if(w.getText().toLowerCase().contains(option.toLowerCase())) {
                    selectOption(i);
                    return true;
                }
                i++;
            }
            return false;
        });
    }

    /**
     * Resumes an object dialogue
     *
     * @param id The dialogue object ID
     */
    public static void resumeObjectDialogue(int id)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click(ClickType.WIDGET);
            client.getPacketWriter().resumeObjectDialoguePacket(id);
            ClientScriptAPI.closeNumericInputDialogue();
        });
    }

//    /**
//     * Resumes a name dialogue
//     *
//     * @param text The name to input
//     */
//    public static void resumeNameDialogue(String text)
//    {
//        TClient client = Static.getClient();
//        Static.invoke(() -> {
//            ClickManager.click(PacketInteractionType.WIDGET_INTERACT);
//            client.getPacketWriter().resumeNameDialoguePacket(text);
//            ClientScriptAPI.closeNumericInputDialogue();
//        });
//    }

    /**
     * Resumes a numeric dialogue
     *
     * @param value The number to input
     */
    public static void resumeNumericDialogue(int value)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            client.getPacketWriter().resumeCountDialoguePacket(value);
            ClientScriptAPI.closeNumericInputDialogue();
        });
    }

    /**
     * Sends a resume/pause for a specific widget and option index.
     *
     * @param widgetID    The ID of the widget to interact with.
     * @param optionIndex The index of the option to select within the widget.
     */
    public static void resumePause(int widgetID, int optionIndex) {
        TClient client = Static.getClient();
        Static.invoke(() -> client.getPacketWriter().resumePauseWidgetPacket(widgetID, optionIndex));
    }

    /**
     * Retrieves a list of available dialogue options.
     *
     * @return A list of dialogue option texts. If no options are present, returns an empty list.
     */
    public static List<String> getOptions() {
        return Static.invoke(() -> {
            List<String> options = new ArrayList<>();
            Widget widget = WidgetAPI.get(InterfaceID.Chatmenu.OPTIONS);
            if(widget == null)
                return options;
            Widget[] dialogOption1kids = widget.getChildren();
            if(dialogOption1kids == null)
                return options;
            if(dialogOption1kids.length < 2)
                return options;
            boolean skipZero = true;
            for(Widget w : dialogOption1kids) {
                if(skipZero)
                {
                    skipZero = false;
                    continue;
                }
                else if(w.getText().isBlank())
                {
                    continue;
                }
                options.add(w.getText());
            }
            return options;
        });
    }

    /**
     * Checks if a dialogue with options is present.
     *
     * @return true if a dialogue with options is present, false otherwise.
     */

    public static boolean isViewingOptions() {
        return !getOptions().isEmpty();
    }

    /**
     * Checks if a specific dialogue option is present.
     *
     * @param option The text of the option to check for. Case-insensitive and partial matches are supported.
     * @return true if the option is present, false otherwise.
     */
    public static boolean optionPresent(String option)
    {
        List<String> options = getOptions();
        for(String s : options) {
            if(s.toLowerCase().contains(option.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to continue a quest by selecting the highlighted option.
     * @return true if an option was selected, false otherwise.
     */
    public static boolean continueQuestHelper() {
        Widget widget = WidgetAPI.get(InterfaceID.Chatmenu.OPTIONS);
        if (widget == null)
            return false;
        Widget[] dialogOption1kids = widget.getChildren();
        if (dialogOption1kids == null)
            return false;
        int i = 0;
		String configValue = configManager.getConfiguration("questhelper", "textHighlightColor");
        if (configValue == null || configValue.isEmpty())
            return false;
        int colorCode = Integer.parseInt(configValue);
        for (Widget w : dialogOption1kids) {
            if (w.getTextColor() == colorCode) {
                selectOption(i);
                return true;
            }
            ++i;
        }
        return false;
    }

    /**
     * Continues the Varrock Museum quiz by selecting the correct answer based on the question.
     * @return true if an answer was selected, false otherwise.
     */
    public static boolean continueMuseumQuiz() {
        Widget questionWidget = WidgetAPI.get(InterfaceID.VmNaturalHistory.VM_NATHIS_QUESTION);
        if(questionWidget == null)
            return false;

        final Widget answerWidget = VarrockMuseumAnswer.findCorrect(
                Static.getClient(),
                questionWidget.getText(),
                InterfaceID.VmNaturalHistory.VM_NATHIS_ANSWER_01,
                InterfaceID.VmNaturalHistory.VM_NATHIS_ANSWER_02,
                InterfaceID.VmNaturalHistory.VM_NATHIS_ANSWER_03);

        if (answerWidget == null)
            return false;

        WidgetAPI.interact(1, answerWidget.getId(), -1, -1);
        return true;
    }
}
