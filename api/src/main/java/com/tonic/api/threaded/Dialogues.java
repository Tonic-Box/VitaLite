package com.tonic.api.threaded;

import static com.tonic.api.widgets.DialogueAPI.*;

/**
 * Threaded dialogue related automation
 */
public class Dialogues
{
    /**
     * Processes all dialogues until there are no more left
     */
    public static void processDialogues()
    {
        while(continueDialogue())
        {
            Delays.tick();
        }
    }

    /**
     * Processed all sialogues handling options in the order supplied until none are left.
     * @param options the options to select in order
     */
    public static void processDialogues(String... options)
    {
        DialogueNode.get(options).process();
    }

    /**
     * Waits until a dialogue is present
     */
    public static void waitForDialogues()
    {
        while(!dialoguePresent())
        {
            Delays.tick();
        }
    }

    /**
     * Continues all dialogues, quest helpers, and museum quizzes until none are left
     */
    public static void continueAllDialogue()
    {
        while(true)
        {
            if(!continueDialogue())
            {
                if(!continueQuestHelper())
                {
                    if(!continueMuseumQuiz())
                    {
                        Delays.tick();
                        break;
                    }
                }
            }
            Delays.tick();
        }
    }
}
