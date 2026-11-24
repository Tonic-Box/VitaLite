package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.handlers.GenericHandlerBuilder;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.requirements.OtherRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.DialogueNode;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The Tree Gnome Stronghold's front gate going in: through the gate once the guards let the player in, else by helping Femi.
 */
public final class GnomeStrongholdGate
{
    private static final WorldPoint INSIDE = new WorldPoint(2461, 3385, 0);
    private static final WorldPoint[] OUTSIDE = {new WorldPoint(2460, 3382, 0), new WorldPoint(2461, 3382, 0), new WorldPoint(2462, 3382, 0)};
    private static final int LET_IN_VARP = 21;
    private static final int LET_IN_BITS = 1 << 19 | 1 << 21 | 1 << 22;
    private static final String GATE = "Gate";
    private static final String FEMI = "Femi";
    private static final String OPEN = "Open";
    private static final String TALK_TO = "Talk-to";
    private static final String HELP_HER = "Okay then";
    private static final int SEARCH_RADIUS = 8;
    private static final int GATE_TICKS = 3;
    private static final int FEMI_TICKS = 40;
    private static final int OPEN_TICKS = 2;
    private static final int DIALOGUE_TIMEOUT = 15;
    private static final int LIFTING_TIMEOUT = 30;
    private static final int CROSSING_TIMEOUT = 25;

    private GnomeStrongholdGate()
    {
    }

    /**
     * Adds both ways in, the gate and Femi's, from each tile outside the gate.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        for (WorldPoint outside : OUTSIDE)
        {
            BooleanSupplier arrived = new Transport(GATE, ObjectID.GNOME_AREAGATE, true, outside, INSIDE, 1, null, GATE_TICKS, null)::arrived;
            add.accept(new CodeTransport(GATE, outside, INSIDE, GATE_TICKS, requirements(new OtherRequirement(GnomeStrongholdGate::letIn)), () -> throughTheGate(outside, arrived)));
            add.accept(new CodeTransport(FEMI, outside, INSIDE, FEMI_TICKS, requirements(new OtherRequirement(() -> !letIn())), () -> withFemi(outside, arrived)));
        }
    }

    private static boolean letIn()
    {
        return (VarAPI.getVarp(LET_IN_VARP) & LET_IN_BITS) == LET_IN_BITS;
    }

    private static Requirements requirements(OtherRequirement when)
    {
        Requirements requirements = new Requirements();
        requirements.addRequirement(new WorldRequirement(true));
        requirements.addRequirement(when);
        return requirements;
    }

    private static StepHandler throughTheGate(WorldPoint outside, BooleanSupplier arrived)
    {
        return GenericHandlerBuilder.get()
                .add(() ->
                {
                    TileObjectEx gate = new TileObjectQuery().withId(ObjectID.GNOME_AREAGATE).within(outside, SEARCH_RADIUS).nearest();
                    if (gate != null)
                    {
                        TileObjectAPI.interact(gate, OPEN);
                    }
                })
                .addDelay(OPEN_TICKS)
                .add(() -> MovementAPI.walkToWorldPoint(INSIDE))
                .addDelayUntil(CROSSING_TIMEOUT, arrived, () ->
                {
                })
                .build();
    }

    private static StepHandler withFemi(WorldPoint outside, BooleanSupplier arrived)
    {
        DialogueNode helpHer = DialogueNode.get(new String[]{HELP_HER});
        return GenericHandlerBuilder.get()
                .add(() -> talkToFemi(outside))
                .addDelayUntil(DIALOGUE_TIMEOUT, DialogueAPI::dialoguePresent, () ->
                {
                })
                .addDelayUntil(() -> !helpHer.processStep())
                .addDelayUntil(LIFTING_TIMEOUT, DialogueAPI::dialoguePresent, () ->
                {
                })
                .addDelayUntil(() -> !DialogueAPI.continueDialogue() && VarAPI.getVar(VarbitID.BUSY) == 0)
                .add(() -> talkToFemi(outside))
                .addDelayUntil(CROSSING_TIMEOUT, arrived, () ->
                {
                })
                .build();
    }

    private static void talkToFemi(WorldPoint outside)
    {
        NpcEx femi = new NpcQuery().withIds(NpcID.GRANDTREE_FEMI).within(outside, SEARCH_RADIUS).nearest();
        if (femi != null)
        {
            NpcAPI.interact(femi, TALK_TO);
        }
    }
}
