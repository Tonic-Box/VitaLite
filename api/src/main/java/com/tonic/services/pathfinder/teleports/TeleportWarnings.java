package com.tonic.services.pathfinder.teleports;

import com.tonic.api.widgets.DialogueAPI;

import java.util.List;

/**
 * The warnings a teleport raises before it goes, answered so it goes: the notice that leaving a boat at sea loses it, and choices such as a Wilderness tablet's "Teleport into the wilderness?", the "don't ask again" yes first.
 */
public final class TeleportWarnings
{
    private static final String LEAVING_THE_BOAT = "leaving your boat";
    private static final String YES = "yes";
    private static final String DONT_ASK = "don't ask";
    private static final String TAG = "<[^>]*>";

    private TeleportWarnings()
    {
    }

    /**
     * Carries the notice of leaving a boat on, or picks the choice that lets the teleport go.
     *
     * @return whether an answer was sent
     */
    public static boolean answer()
    {
        String text = DialogueAPI.getDialogueText();
        if (text != null && plain(text).contains(LEAVING_THE_BOAT) && DialogueAPI.continueDialogue())
        {
            return true;
        }
        String choice = goAhead(DialogueAPI.getOptions());
        return choice != null && DialogueAPI.selectOption(choice);
    }

    private static String goAhead(List<String> options)
    {
        if (options == null)
        {
            return null;
        }
        String yes = null;
        for (String option : options)
        {
            String said = plain(option);
            if (said.startsWith(YES) && said.contains(DONT_ASK))
            {
                return option;
            }
            if (yes == null && said.startsWith(YES))
            {
                yes = option;
            }
        }
        return yes;
    }

    private static String plain(String text)
    {
        return text.replaceAll(TAG, "").trim().toLowerCase();
    }
}
