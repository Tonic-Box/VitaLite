package com.tonic.api.handlers;

import static com.tonic.api.widgets.DialogueAPI.*;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.util.DialogueNode;
import com.tonic.util.handler.AbstractHandlerBuilder;
import com.tonic.util.handler.HandlerBuilder;

public class DialogueBuilder extends AbstractHandlerBuilder
{
    public static DialogueBuilder get()
    {
        return new DialogueBuilder();
    }

    public DialogueBuilder processDialogues(String... options)
    {
        DialogueNode node = DialogueNode.get(options);
        addDelayUntil(() -> !node.processStep());
        return this;
    }

    public DialogueBuilder waitForDialogue()
    {
        addDelayUntil(DialogueAPI::dialoguePresent);
        return this;
    }

    public DialogueBuilder continueAllDialogue()
    {
        addDelayUntil(() -> !continueDialogue() && !continueQuestHelper() && !continueMuseumQuiz());
        return this;
    }
}
