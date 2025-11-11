package com.tonic.api.handlers;

import static com.tonic.api.widgets.DialogueAPI.*;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.util.DialogueNode;
import com.tonic.util.handler.AbstractHandlerBuilder;

/**
 * A builder for handling in-game dialogues.
 */
public class DialogueBuilder extends AbstractHandlerBuilder<DialogueBuilder>
{
    /**
     * Creates a new instance of DialogueBuilder.
     *
     * @return A new DialogueBuilder instance.
     */
    public static DialogueBuilder get()
    {
        return new DialogueBuilder();
    }

    /**
     * Processes dialogues based on the provided options.
     *
     * @param options The dialogue options to process.
     * @return DialogueBuilder instance
     */
    public DialogueBuilder processDialogues(String... options)
    {
        DialogueNode node = DialogueNode.get(options);
        addDelayUntil(() -> !node.processStep());
        return this;
    }

    /**
     * Waits until a dialogue is present.
     *
     * @return DialogueBuilder instance
     */
    public DialogueBuilder waitForDialogue()
    {
        addDelayUntil(DialogueAPI::dialoguePresent);
        return this;
    }

    /**
     * Continues all types of dialogues including quest helpers and museum quizzes.
     *
     * @return DialogueBuilder instance
     */
    public DialogueBuilder continueAllDialogue()
    {
        addDelayUntil(() -> !continueDialogue() && !continueQuestHelper() && !continueMuseumQuiz());
        return this;
    }
}
