package com.tonic.api.handlers;

import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.util.DialogueNode;
import com.tonic.util.handler.AbstractHandlerBuilder;
import net.runelite.api.coords.WorldPoint;

/**
 * Builder for handling interactions with tile objects in the game world.
 */
public class TileObjectBuilder extends AbstractHandlerBuilder<TileObjectBuilder>
{
    /**
     * Creates a new instance of TileObjectBuilder.
     *
     * @return A new TileObjectBuilder instance.
     */
    public static TileObjectBuilder get()
    {
        return new TileObjectBuilder();
    }

    /**
     * Walks to the specified world point and interacts with the specified object.
     *
     * @param worldPoint The world point to walk to.
     * @param objectName The name of the object to interact with.
     * @param action The action to perform on the object.
     * @param node The dialogue node to handle after interaction.
     * @return TileObjectBuilder instance
     */
    public TileObjectBuilder visit(WorldPoint worldPoint, String objectName, String action, DialogueNode node)
    {
        walkTo(worldPoint);
        return interact(objectName, action, node);
    }

    /**
     * Walks to the specified world point and interacts with the specified object.
     *
     * @param worldPoint The world point to walk to.
     * @param objectName The name of the object to interact with.
     * @param action The action to perform on the object.
     * @param dialogueOptions The dialogue options to handle after interaction.
     * @return TileObjectBuilder instance
     */
    public TileObjectBuilder visit(WorldPoint worldPoint, String objectName, String action, String ... dialogueOptions)
    {
        DialogueNode node = DialogueNode.get(dialogueOptions);
        return visit(worldPoint, objectName, action, node);
    }

    /**
     * Walks to the specified world point and interacts with the specified object.
     *
     * @param worldPoint The world point to walk to.
     * @param objectName The name of the object to interact with.
     * @param action The action to perform on the object.
     * @return TileObjectBuilder instance
     */
    public TileObjectBuilder visit(WorldPoint worldPoint, String objectName, String action)
    {
        return visit(worldPoint, objectName, action, (DialogueNode) null);
    }

    /**
     * Interacts with the specified object using the given action.
     *
     * @param objectName The name of the object to interact with.
     * @param action The action to perform on the object.
     * @return TileObjectBuilder instance
     */
    public TileObjectBuilder interact(String objectName, String action)
    {
        return interact(objectName, action, (DialogueNode) null);
    }

    /**
     * Interacts with the specified object using the given action and handles dialogue.
     *
     * @param objectName The name of the object to interact with.
     * @param action The action to perform on the object.
     * @param node The dialogue node to handle after interaction.
     * @return TileObjectBuilder instance
     */
    public TileObjectBuilder interact(String objectName, String action, DialogueNode node)
    {
        int step = currentStep;
        add(() -> {
            TileObjectEx object = TileObjectAPI.search()
                    .withNameContains(objectName)
                    .withPartialAction(action)
                    .nearest();
            if (object != null) {
                TileObjectAPI.interact(object, action);
                return step + 1;
            } else {
                return step;
            }
        });
        addDelayUntil(() -> PlayerAPI.isIdle());
        if(node != null)
        {
            addDelayUntil(() -> !node.processStep());
        }
        return this;
    }

    /**
     * Interacts with the specified object using the given action and handles dialogue.
     *
     * @param objectName The name of the object to interact with.
     * @param action The action to perform on the object.
     * @param dialogueOptions The dialogue options to handle after interaction.
     * @return TileObjectBuilder instance
     */
    public TileObjectBuilder interact(String objectName, String action, String... dialogueOptions)
    {
        DialogueNode node = DialogueNode.get(dialogueOptions);
        return interact(objectName, action, node);
    }
}
