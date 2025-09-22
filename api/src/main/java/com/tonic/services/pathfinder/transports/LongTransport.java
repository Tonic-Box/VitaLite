package com.tonic.services.pathfinder.transports;

import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.data.TileObjectEx;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.math.NumberUtils;
import java.util.ArrayList;
import java.util.List;


public class LongTransport extends Transport
{
    public LongTransport(WorldPoint source, WorldPoint destination, int sourceRadius, int destinationRadius, List<Runnable> handler) {
        super(source, destination, sourceRadius, destinationRadius, null, -1);
        this.handler = handler;
        this.duration = handler.size();
    }

    public LongTransport(WorldPoint source, WorldPoint destination, int sourceRadius, int destinationRadius, List<Runnable> handler, Requirements requirements, int delay) {
        super(WorldPointUtil.compress(source), WorldPointUtil.compress(destination), sourceRadius, destinationRadius, delay, handler, requirements, -1);
        this.source = WorldPointUtil.compress(source);
        this.destination = WorldPointUtil.compress(destination);
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = handler;
    }

    public static LongTransport npcDialogTransport(int delay, Requirements requirements, String npcName, String option, int npcRadious, WorldPoint source, WorldPoint destination, String... dialogueOptions)
    {
        List<Runnable> actions = new ArrayList<>();
        actions.add(() -> {
            NPC npc = new NpcQuery()
                    .withName(npcName)
                    .sortNearest()
                    .first();
            if(NumberUtils.isCreatable(option))
                NpcAPI.interact(npc, Integer.parseInt(option));
            else
                NpcAPI.interact(npc, option);
        });
        for(String opt : dialogueOptions)
        {
            if(opt == null || opt.isBlank())
            {
                actions.add(DialogueAPI::continueDialogue);
            }
            else
            {
                actions.add(() -> DialogueAPI.selectOption(opt));
            }
        }
        actions.add(DialogueAPI::continueDialogue);

        return new LongTransport(source, destination, npcRadious, 2, actions, requirements, delay);
    }

    public static LongTransport addObjectTransport(int delay, Requirements requirements, WorldPoint source, WorldPoint destination, int objectID, String action, String... options)
    {
        List<Runnable> actions = new ArrayList<>();
        actions.add(() -> {
            TileObjectEx obj = new TileObjectQuery<>()
                    .withId(objectID)
                    .sortNearest()
                    .first();

            if(obj == null)
                return;

            Client client = Static.getClient();

            if((client.getLocalPlayer().getWorldLocation().distanceTo(obj.getWorldLocation()) > 2) && objectID != 190)
                return;

            if(NumberUtils.isCreatable(action))
            {
                TileObjectAPI.interact(obj, Integer.parseInt(action));
            }
            else
            {
                TileObjectAPI.interact(obj, action);
            }
        });
        if(options != null)
        {
            for(String option : options)
            {
                if(option == null || option.isBlank())
                {
                    actions.add(DialogueAPI::continueDialogue);
                }
                else
                {
                    actions.add(() -> DialogueAPI.selectOption(option));
                }
            }
        }
        return new LongTransport(source, destination, 2, 2, actions, requirements, delay);
    }
}
