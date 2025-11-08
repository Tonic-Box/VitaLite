package com.tonic.api.handlers;

import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.data.TileObjectEx;
import com.tonic.services.pathfinder.model.WalkerPath;
import com.tonic.util.DialogueNode;
import com.tonic.util.handler.AbstractHandlerBuilder;
import com.tonic.util.handler.HandlerBuilder;
import net.runelite.api.coords.WorldPoint;

public class TileObjectBuilder extends AbstractHandlerBuilder
{
    public static HandlerBuilder get()
    {
        return new HandlerBuilder();
    }

    public TileObjectBuilder visit(WorldPoint worldPoint, String objectName, String action, DialogueNode node)
    {
        walkTo(worldPoint);
        return interact(objectName, action, node);
    }

    public TileObjectBuilder visit(WorldPoint worldPoint, String objectName, String action, String ... dialogueOptions)
    {
        DialogueNode node = DialogueNode.get(dialogueOptions);
        return visit(worldPoint, objectName, action, node);
    }

    public TileObjectBuilder visit(WorldPoint worldPoint, String objectName, String action)
    {
        return visit(worldPoint, objectName, action, (DialogueNode) null);
    }

    public TileObjectBuilder interact(String objectName, String action)
    {
        return interact(objectName, action, (DialogueNode) null);
    }

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

    public TileObjectBuilder interact(String objectName, String action, String... dialogueOptions)
    {
        DialogueNode node = DialogueNode.get(dialogueOptions);
        return interact(objectName, action, node);
    }
}
