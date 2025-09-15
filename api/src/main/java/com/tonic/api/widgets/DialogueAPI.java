package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.game.ClientScriptAPI;
import com.tonic.data.VarrockMuseumAnswer;
import com.tonic.data.WidgetInfoExtended;
import com.tonic.services.ClickManager;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * DialogueAPI provides methods to interact with in-game dialogues, including retrieving dialogue text,
 * continuing dialogues, selecting options, and handling various dialogue types.
 */
public class DialogueAPI
{
    /**
     * Retrieves the header of the current dialogue, which may indicate the speaker or context.
     *
     * @return The dialogue header as a String. Possible values include NPC names, "Player", "Select an Option", or "UNKNOWN".
     */
    public static String getDialogueHeader()
    {
        return Static.invoke(() -> {
            if (WidgetAPI.get(WidgetInfo.DIALOG_NPC_TEXT) != null) {
                return WidgetAPI.get(WidgetInfo.DIALOG_NPC_NAME).getText();
            }
            else if (WidgetAPI.get(WidgetInfo.DIALOG_PLAYER_TEXT) != null) {
                return "Player";
            }
            else if (WidgetAPI.get(WidgetInfoExtended.DIALOG_OPTION_OPTION1) != null) {
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
            if (WidgetAPI.get(WidgetInfo.DIALOG_NPC_TEXT) != null) {
                return WidgetAPI.get(WidgetInfo.DIALOG_NPC_TEXT).getText();
            }
            else if (WidgetAPI.get(WidgetInfo.DIALOG_PLAYER_TEXT) != null) {
                return WidgetAPI.get(WidgetInfo.DIALOG_PLAYER_TEXT).getText();
            }
            else if (WidgetAPI.get(WidgetInfo.DIALOG_SPRITE_TEXT) != null) {
                return WidgetAPI.get(WidgetInfo.DIALOG_SPRITE_TEXT).getText();
            }
            else if (WidgetAPI.get(WidgetInfoExtended.DIALOG2_SPRITE_TEXT) != null) {
                return WidgetAPI.get(WidgetInfoExtended.DIALOG2_SPRITE_TEXT).getText();
            }

            else if (WidgetAPI.get(WidgetInfoExtended.MINIGAME_DIALOG_TEXT) != null) {
                return WidgetAPI.get(WidgetInfoExtended.MINIGAME_DIALOG_TEXT).getText();
            }
            else if (WidgetAPI.get(WidgetInfoExtended.DIALOG_NOTIFICATION_TEXT) != null) {
                return WidgetAPI.get(WidgetInfoExtended.DIALOG_NOTIFICATION_TEXT).getText();
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
            if (WidgetAPI.get(WidgetInfoExtended.DIALOG_NPC_CONTINUE) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(WidgetInfoExtended.DIALOG_NPC_CONTINUE.getId(), -1);
                return true;
            }
            else if (WidgetAPI.get(633, 0) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(WidgetInfo.PACK(633, 0), -1);
                return true;
            }
            else if (WidgetAPI.get(WidgetInfoExtended.DIALOG_PLAYER_CONTINUE) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(WidgetInfoExtended.DIALOG_PLAYER_CONTINUE.getId(), -1);
                return true;
            }
            else if (WidgetAPI.get(WidgetInfo.DIALOG_SPRITE) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(WidgetInfo.DIALOG_SPRITE.getId(), 0);
                return true;
            }
            else if (WidgetAPI.get(WidgetInfoExtended.DIALOG2_SPRITE) != null) {
                client.getPacketWriter().resumePauseWidgetPacket(WidgetInfoExtended.DIALOG2_SPRITE_CONTINUE.getId(), -1);
                return true;
            }
            else if (WidgetAPI.get(WidgetInfoExtended.MINIGAME_DIALOG_CONTINUE) != null) {
                Widget w = WidgetAPI.get(WidgetInfoExtended.MINIGAME_DIALOG_CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                {
                    client.getPacketWriter().resumePauseWidgetPacket(WidgetInfoExtended.MINIGAME_DIALOG_CONTINUE.getId(), -1);
                    return true;
                }
            }
            else if (WidgetAPI.get(WidgetInfoExtended.DIALOG_NOTIFICATION_CONTINUE) != null) {
                Widget w = WidgetAPI.get(WidgetInfoExtended.DIALOG_NOTIFICATION_CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                {
                    client.getPacketWriter().resumePauseWidgetPacket(WidgetInfoExtended.DIALOG_NOTIFICATION_CONTINUE.getId(), -1);
                    return true;
                }
            }
            else if (WidgetAPI.get(WidgetInfoExtended.LEVEL_UP_CONTINUE) != null) {
                Widget w = WidgetAPI.get(WidgetInfoExtended.LEVEL_UP_CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                {
                    client.getPacketWriter().resumePauseWidgetPacket(WidgetInfoExtended.LEVEL_UP_CONTINUE.getId(), -1);
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
            if (WidgetAPI.get(WidgetInfoExtended.DIALOG_NPC_CONTINUE) != null) {
                return true;
            }
            else if (WidgetAPI.get(633, 0) != null) {
                return true;
            }
            else if (WidgetAPI.get(WidgetInfoExtended.DIALOG_PLAYER_CONTINUE) != null) {
                return true;
            }
            else if (WidgetAPI.get(WidgetInfo.DIALOG_SPRITE) != null) {
                return true;
            }
            else if (WidgetAPI.get(WidgetInfoExtended.DIALOG2_SPRITE) != null) {
                return true;
            }
            else if (WidgetAPI.get(WidgetInfoExtended.MINIGAME_DIALOG_CONTINUE) != null) {
                Widget w = WidgetAPI.get(WidgetInfoExtended.MINIGAME_DIALOG_CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                    return true;
            }
            else if (WidgetAPI.get(WidgetInfoExtended.DIALOG_NOTIFICATION_CONTINUE) != null) {
                Widget w = WidgetAPI.get(WidgetInfoExtended.DIALOG_NOTIFICATION_CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                    return true;
            }
            else if(WidgetAPI.get(WidgetInfoExtended.LEVEL_UP_CONTINUE) != null) {
                Widget w = WidgetAPI.get(WidgetInfoExtended.LEVEL_UP_CONTINUE);
                if(w != null && w.getText() != null && w.getText().equals("Click here to continue"))
                    return true;
            }

            return WidgetAPI.get(WidgetInfoExtended.DIALOG_OPTION_OPTION1) != null || WidgetAPI.get(WidgetInfo.DIALOG_OPTION_OPTIONS) != null;
        });
    }

    /**
     * Selects a dialogue option based on its index.
     *
     * @param option The index of the option to select (0-based).
     */
    public static void selectOption(int option) {
        resumePause(WidgetInfoExtended.DIALOG_OPTION_OPTION1.getId(), option);
    }

    /**
     * Selects a dialogue option based on its text.
     *
     * @param option The text of the option to select. Case-insensitive and partial matches are supported.
     * @return true if the option was found and selected, false otherwise.
     */
    public static boolean selectOption(String option) {

        return Static.invoke(() -> {
            Widget widget = WidgetAPI.get(WidgetInfoExtended.DIALOG_OPTION_OPTION1);
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
            ClickManager.click();
            client.getPacketWriter().resumeObjectDialoguePacket(id);
            ClientScriptAPI.closeNumericInputDialogue();
        });
    }

    /**
     * Resumes a name dialogue
     *
     * @param text The name to input
     */
    public static void resumeNameDialogue(String text)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click();
            client.getPacketWriter().resumeNameDialoguePacket(text);
            ClientScriptAPI.closeNumericInputDialogue();
        });
    }

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
     * Sends a "make X" dialogue input with the specified quantity.
     *
     * @param quantity The quantity to input in the "make X" dialogue.
     */
    public static void makeX(int quantity)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click();
            client.getPacketWriter().resumePauseWidgetPacket(17694734, quantity);
        });
    }

    /**
     * Retrieves a list of available dialogue options.
     *
     * @return A list of dialogue option texts. If no options are present, returns an empty list.
     */
    public static List<String> getOptions() {
        return Static.invoke(() -> {
            List<String> options = new ArrayList<>();
            Widget widget = WidgetAPI.get(WidgetInfoExtended.DIALOG_OPTION_OPTION1);
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

    public static boolean continueQuestHelper() {
        Widget widget = WidgetAPI.get(WidgetInfoExtended.DIALOG_OPTION_OPTION1);
        if(widget == null)
            return false;
        Widget[] dialogOption1kids = widget.getChildren();
        if(dialogOption1kids == null)
            return false;
        int i = 0;
        for(Widget w : dialogOption1kids) {
            if(w.getTextColor() == -0xff4d4e) {
                selectOption(i);
                return true;
            }
            ++i;
        }
        return false;
    }

    public static boolean continueMuseumQuiz() {
        Widget questionWidget = WidgetAPI.get(WidgetInfo.VARROCK_MUSEUM_QUESTION);
        if(questionWidget == null)
            return false;

        final Widget answerWidget = VarrockMuseumAnswer.findCorrect(
                Static.getClient(),
                questionWidget.getText(),
                WidgetInfo.VARROCK_MUSEUM_FIRST_ANSWER,
                WidgetInfo.VARROCK_MUSEUM_SECOND_ANSWER,
                WidgetInfo.VARROCK_MUSEUM_THIRD_ANSWER);

        if (answerWidget == null)
            return false;

        WidgetAPI.interact(1, answerWidget.getId(), -1, -1);
        return true;
    }
}
