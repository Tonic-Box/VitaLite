package com.tonic.services.pathfinder.transports;

import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.util.DialogueNode;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.util.handler.StepHandler;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.math.NumberUtils;


public class LongTransport extends Transport
{
    public LongTransport(WorldPoint source, WorldPoint destination, int sourceRadius, int destinationRadius, StepHandler handler) {
        super(source, destination, sourceRadius, destinationRadius, null, -1);
        this.handler = handler;
        this.duration = handler.size();
    }

    public LongTransport(WorldPoint source, WorldPoint destination, int sourceRadius, int destinationRadius, StepHandler handler, Requirements requirements, int delay) {
        super(WorldPointUtil.compress(source), WorldPointUtil.compress(destination), sourceRadius, destinationRadius, delay, handler, requirements, -1);
        this.source = WorldPointUtil.compress(source);
        this.destination = WorldPointUtil.compress(destination);
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = handler;
    }

    public static LongTransport npcDialogTransport(int delay, Requirements requirements, String npcName, String option, int npcRadious, WorldPoint source, WorldPoint destination, String... dialogueOptions)
    {
        DialogueNode node = DialogueNode.get(dialogueOptions);
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    NpcEx npc = new NpcQuery()
                            .withName(npcName)
                            .keepIf(n -> n.getComposition() != null)
                            .sortNearest()
                            .first();
                    if(NumberUtils.isCreatable(option))
                        NpcAPI.interact(npc, Integer.parseInt(option));
                    else
                        NpcAPI.interact(npc, option);
                    return 1;
                })
                .add(1, () -> node.processStep() ? 1 : 2);
        for(int i = 0; i < delay; i++)
        {
            int finalI = i;
            builder.add(2 + i, () -> 3 + finalI);
        }
        return new LongTransport(source, destination, npcRadious, 2, builder.build(), requirements, delay);
    }

    public static LongTransport addObjectTransport(int delay, Requirements requirements, WorldPoint source, WorldPoint destination, int objectID, String action, String... options)
    {
        DialogueNode node = DialogueNode.get(options);
        HandlerBuilder builder = HandlerBuilder.get()
                .add(0, () -> {
                    TileObjectEx obj = new TileObjectQuery()
                            .withId(objectID)
                            .sortNearest()
                            .first();

                    if(obj == null)
                        return 0;

                    Client client = Static.getClient();

                    if((client.getLocalPlayer().getWorldLocation().distanceTo(obj.getWorldPoint()) > 2) && objectID != 190)
                        return 0;

                    if(NumberUtils.isCreatable(action))
                    {
                        TileObjectAPI.interact(obj, Integer.parseInt(action));
                    }
                    else
                    {
                        TileObjectAPI.interact(obj, action);
                    }
                    return 1;
                })
                .add(1, () -> node.processStep() ? 1 : 2);

        for(int i = 0; i < delay; i++)
        {
            int finalI = i;
            builder.add(2 + i, () -> 3 + finalI);
        }

        if(options != null)
        {
            builder.add(1, () -> node.processStep() ? 1 : 2);
        }
        return new LongTransport(source, destination, 2, 2, builder.build(), requirements, delay);
    }
}
