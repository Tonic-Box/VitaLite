package com.tonic.api.handlers;

import static com.tonic.api.widgets.DialogueAPI.*;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.util.DialogueNode;
import com.tonic.util.handler.HandlerBuilder;

public class DialogueBuilder extends HandlerBuilder
{
    public static DialogueBuilder get()
    {
        return new DialogueBuilder();
    }

    private int currentStep = 0;

    public DialogueBuilder processDialogues(String... options)
    {
        DialogueNode node = DialogueNode.get(options);
        addDelayUntil(currentStep++, () -> !node.processStep());
        return this;
    }

    public DialogueBuilder waitForDialogue()
    {
        addDelayUntil(currentStep++, DialogueAPI::dialoguePresent);
        return this;
    }

    public DialogueBuilder continueAllDialogue()
    {
        addDelayUntil(currentStep++, () -> !continueDialogue() && !continueQuestHelper() && !continueMuseumQuiz());
        return this;
    }
}
