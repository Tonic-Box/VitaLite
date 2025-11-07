package com.tonic.util;

import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.DialogueAPI;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that represents a dialogue tree node structure for conversational paths.
 */
public class DialogueNode
{
    /**
     * Creates a new instance of a DialogueNode.
     * @return A new instance of a DialogueNode.
     */
    public static DialogueNode get()
    {
        return new DialogueNode();
    }

    /**
     * Creates a new instance of a DialogueNode with the given options.
     * @param options The options to add to the DialogueNode.
     * @return A new instance of a DialogueNode with the given options.
     */
    public static DialogueNode get(Object[] options)
    {
        return get().node(options);
    }

    private final Map<Integer, Object> nodes = new HashMap<>();

    // State machine fields
    private int counter = 0;
    private DialogueNode activeChild = null;

    private DialogueNode()
    {
    }

    /**
     * Adds a new node to the dialogue tree.
     * @param options The options to add to the dialogue tree.
     * @return The current instance of the DialogueNode.
     */
    public DialogueNode node(Object... options) {
        DialogueNode node = new DialogueNode();
        AtomicInteger index = new AtomicInteger(0);

        if(options == null || options.length == 0)
        {
            return this;
        }

        Arrays.stream(options)
                .filter(option -> option instanceof DialogueNode || option instanceof String)
                .forEach(option -> node.nodes.put(index.getAndIncrement(), option));

        nodes.put(nodes.size(), node);
        return this;
    }

    /**
     * Processes the dialogue tree (blocking).
     */
    public void process()
    {
        int counter = 0;
        Object node;
        DialogueNode dialogueNode;
        while(true)
        {
            if(!DialogueAPI.continueDialogue())
            {
                node = nodes.get(counter);

                if(node == null)
                {
                    return;
                }

                if(node instanceof String)
                {
                    if(DialogueAPI.selectOption((String)node))
                    {
                        counter++;
                    }
                    else
                    {
                        return;
                    }
                }
                else if(node instanceof DialogueNode)
                {
                    dialogueNode = (DialogueNode) node;
                    if(dialogueNode.test())
                    {
                        dialogueNode.process();
                        break;
                    }
                    counter++;
                }

                if(!DialogueAPI.dialoguePresent())
                {
                    break;
                }
            }
            Delays.tick();
        }
        Delays.tick();
    }

    /**
     * Resets the state machine for reuse.
     * Call this before using processStep() on a DialogueNode instance.
     */
    public void reset()
    {
        counter = 0;
        activeChild = null;
    }

    /**
     * Processes one step of the dialogue tree for state machine usage.
     * Call repeatedly until it returns false.
     *
     * Usage:
     * <pre>
     * dialogueNode.reset();
     * while (dialogueNode.processStep()) {
     *     Delays.tick();
     * }
     * Delays.tick(); // Final delay
     * </pre>
     *
     * @return true if processing should continue (call again next tick), false when complete or failed
     */
    public boolean processStep()
    {
        if (activeChild != null)
        {
            if (!activeChild.processStep())
            {
                activeChild = null;
                reset();
                return false;
            }
            return true;
        }

        if (DialogueAPI.continueDialogue())
        {
            return true;
        }

        Object node = nodes.get(counter);

        if (node == null)
        {
            reset();
            return false;
        }

        if (node instanceof String)
        {
            if (DialogueAPI.selectOption((String)node))
            {
                counter++;
            }
            else
            {
                reset();
                return false;
            }
        }
        else if (node instanceof DialogueNode)
        {
            DialogueNode dialogueNode = (DialogueNode) node;
            if (dialogueNode.test())
            {
                activeChild = dialogueNode;
                activeChild.reset();
                return true;
            }
            counter++;
        }

        if(!DialogueAPI.dialoguePresent())
        {
            reset();
            return false;
        }

        return true;
    }

    /**
     * Tests the dialogue tree.
     * @return True if the dialogue tree is valid, false otherwise.
     */
    private boolean test()
    {
        Object node = nodes.get(0);

        if(node instanceof String)
        {
            return DialogueAPI.optionPresent((String) node);
        }
        else if(node instanceof DialogueNode)
        {
            return ((DialogueNode) nodes.get(0)).test();
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("DialogueNode\n{\n");

        for (int i = 0; i < nodes.size(); i++)
        {
            Object node = nodes.get(i);
            if (node instanceof String)
            {
                result.append("\t\"").append(node).append("\"\n");
            }
            else if (node instanceof DialogueNode)
            {
                result.append(TextUtil.indent(node.toString()));
            }
        }

        return result.append("}\n").toString();
    }
}