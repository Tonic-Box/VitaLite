package com.tonic.services.pathfinder.transports;

import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.GameAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.PlayerMotion;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.util.DialogueNode;
import com.tonic.util.WorldPointUtil;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepContext;
import com.tonic.util.handler.StepHandler;
import com.tonic.api.handlers.GenericHandlerBuilder;
import net.runelite.api.Client;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import java.util.function.BooleanSupplier;

/**
 * One edge of the transport graph: using an object or NPC with a 1-based option at the tile it leaves from, answering its dialogue, takes the player to its destination. Instances are shared data holding no execution state; buildHandler makes a fresh handler for each use.
 */
public class Transport
{
    private static final int ARRIVAL_REACH = 2;
    private static final int ARRIVAL_TICKS = 10;
    private static final int FADE_TICKS = 10;
    private static final int CLICK_TRIES = 3;
    private static final int WARNING_TICKS = 3;
    private static final String FADE_WAITED = "FADE_WAITED";
    private static final String CLICKS = "CLICKS";
    private static final String ARRIVAL_WAITED = "ARRIVAL_WAITED";
    private static final String WARNING_ANSWERED = "WARNING_ANSWERED";
    private static final String CLICK_AGAIN = "CLICK_AGAIN";
    private static final String WILDERNESS_WAITED = "WILDERNESS_WAITED";
    private static final String DURATION = "DURATION";
    private static final String DIALOGUE = "DIALOGUE";

    protected String name;
    protected int id;
    protected boolean isObject;
    protected Loc location;
    protected Loc destination;
    protected int option;
    protected String[] dialogueOptions;
    protected int duration;
    protected Requirements requirements;

    /**
     * @param name what it is called, the clicked loc's or NPC's name for a recorded one
     * @param id the loc or NPC id
     * @param isObject whether it is a loc rather than an NPC
     * @param location the tile it leaves from
     * @param destination the tile it lands on
     * @param option the 1-based option used on it
     * @param dialogueOptions the dialogue options chosen in order, or null
     * @param duration how many ticks it takes
     * @param requirements what the player needs to use it, or null
     */
    public Transport(String name, int id, boolean isObject, WorldPoint location, WorldPoint destination, int option, String[] dialogueOptions, int duration, Requirements requirements)
    {
        this.name = name;
        this.id = id;
        this.isObject = isObject;
        this.location = Loc.of(location);
        this.destination = Loc.of(destination);
        this.option = option;
        this.dialogueOptions = dialogueOptions;
        this.duration = duration;
        this.requirements = requirements;
    }

    /**
     * @return the requirements, empty when it has none
     */
    public Requirements getRequirements()
    {
        return requirements == null ? new Requirements() : requirements;
    }

    /**
     * @return whether any requirement is attached
     */
    public boolean hasRequirements()
    {
        return requirements != null && !requirements.getAll().isEmpty();
    }

    /**
     * @param requirements the requirements
     * @return a plain copy of this edge with other requirements
     */
    public Transport withRequirements(Requirements requirements)
    {
        return new Transport(name, id, isObject, location.toWorldPoint(), destination.toWorldPoint(), option, dialogueOptions, duration, requirements);
    }

    /**
     * @param duration how many ticks it takes
     * @return a plain copy of this edge with another duration
     */
    public Transport withDuration(int duration)
    {
        return new Transport(name, id, isObject, location.toWorldPoint(), destination.toWorldPoint(), option, dialogueOptions, duration, requirements);
    }

    /**
     * Whether the walker may take it while teleports are not to be used, as a spirit tree or fairy ring is not.
     *
     * @return true for an ordinary crossing
     */
    public boolean allowedWithoutTeleports()
    {
        return true;
    }

    /**
     * Builds a fresh handler that uses this crossing: waits out a screen fade, clicks it (ending at once when there is no loc or NPC to click), answers its dialogue, waits its duration, then waits for the player to land within two tiles of the destination or to settle elsewhere, clicking again up to three times when the server took no notice; warnings it raises are answered on the way.
     *
     * @return the handler
     */
    public StepHandler buildHandler()
    {
        BooleanSupplier landed = () -> arrived() && PlayerEx.getLocal().isIdle();
        BooleanSupplier idle = () -> !MovementAPI.isMoving();
        BooleanSupplier ignored = () -> PlayerEx.getLocal().isIdle() && PlayerMotion.stillTicks() > ARRIVAL_TICKS;
        return GenericHandlerBuilder.get()
                .addDelayUntil(context -> !waitingOutTheFade(context))
                .add(context ->
                {
                    if (!click())
                    {
                        return HandlerBuilder.END_EXECUTION;
                    }
                    context.put(CLICKS, 1);
                    context.put(DURATION, duration);
                    if (dialogueOptions == null || dialogueOptions.length == 0)
                    {
                        return HandlerBuilder.jump("WAIT", context);
                    }
                    context.put(DIALOGUE, DialogueNode.get(dialogueOptions));
                    return HandlerBuilder.jump("ANSWER", context);
                })
                .addDelayUntil("ANSWER", context -> !answeringWarning(context) && DialogueAPI.dialoguePresent())
                .addDelayUntil(context -> !((DialogueNode) context.get(DIALOGUE)).processStep())
                .addDelayUntil("WAIT", context ->
                {
                    if (answeringWarning(context))
                    {
                        return false;
                    }
                    int left = (int) context.get(DURATION) - 1;
                    if (left <= 0)
                    {
                        context.remove(DURATION);
                        context.remove(DIALOGUE);
                        return true;
                    }
                    context.put(DURATION, left);
                    return false;
                })
                .addDelayUntil(context -> !answeringWarning(context) && arrivedOrSettled(context, landed, idle, ignored))
                .build();
    }

    /**
     * Clicks the crossing: the loc or NPC with its id nearest the player, with its option. An NPC matches by the id it shows now or by its base id, as an NPC that changes form with the player's progress may be recorded by either.
     *
     * @return whether there was one to click
     */
    protected boolean click()
    {
        WorldPoint near = PlayerEx.getLocal().getWorldPoint();
        if (isObject)
        {
            TileObjectEx object = new TileObjectQuery().withId(id).nearest(near);
            if (object == null)
            {
                return false;
            }
            TileObjectAPI.interact(object, option - 1);
            return true;
        }
        NpcEx npc = new NpcQuery().keepIf(candidate -> candidate.getId() == id || baseId(candidate) == id).nearest(near);
        if (npc == null)
        {
            return false;
        }
        NpcAPI.interact(npc, option - 1);
        return true;
    }

    private static int baseId(NpcEx npc)
    {
        return Static.invoke(() ->
        {
            NPCComposition composition = npc.getNpc().getComposition();
            return composition == null ? -1 : composition.getId();
        });
    }

    /**
     * Whether a finished crossing left the player where it started: not at the destination and still within reach of the tile it leaves from, so the click came to nothing.
     *
     * @return whether the crossing failed
     */
    public boolean failed()
    {
        if (arrived())
        {
            return false;
        }
        WorldPoint at = Static.invoke(() ->
        {
            Client client = Static.getClient();
            Player player = client.getLocalPlayer();
            return player == null ? null : WorldPointUtil.fromInstance(player.getWorldLocation());
        });
        WorldPoint from = location.toWorldPoint();
        return at != null && at.getPlane() == from.getPlane() && at.distanceTo2D(from) <= ARRIVAL_REACH;
    }

    private static boolean waitingOutTheFade(StepContext context)
    {
        if (!GameAPI.isScreenFading())
        {
            context.remove(FADE_WAITED);
            return false;
        }
        int waited = (int) context.getOrDefault(FADE_WAITED, 0) + 1;
        context.put(FADE_WAITED, waited);
        return waited <= FADE_TICKS;
    }

    /**
     * Answers a warning the click raised: the Wilderness ditch's with "Enter Wilderness", again every WARNING_TICKS while it stays up; any other by its "don't ask again" toggle, once, the crossing being clicked again on the next tick.
     *
     * @return whether this tick was spent on a warning
     */
    private boolean answeringWarning(StepContext context)
    {
        if (!CrossingWarnings.wildernessWarning())
        {
            context.remove(WILDERNESS_WAITED);
        }
        else
        {
            int waited = context.getOrDefault(WILDERNESS_WAITED, WARNING_TICKS);
            context.put(WILDERNESS_WAITED, waited >= WARNING_TICKS ? 0 : waited + 1);
            context.remove(ARRIVAL_WAITED);
            if (waited >= WARNING_TICKS)
            {
                CrossingWarnings.enterTheWilderness();
            }
            return true;
        }
        if (context.get(CLICK_AGAIN) != null)
        {
            context.remove(CLICK_AGAIN);
            context.remove(ARRIVAL_WAITED);
            click();
            return true;
        }
        if (context.get(WARNING_ANSWERED) == null && CrossingWarnings.dontAskAgain())
        {
            context.put(WARNING_ANSWERED, true);
            context.put(CLICK_AGAIN, true);
            return true;
        }
        return false;
    }

    /**
     * Whether the crossing is over: the player landed and stands idle, or after ARRIVAL_TICKS has settled somewhere else for the walk to plan on from; a player who stood still the whole wait is clicked through again, up to CLICK_TRIES clicks.
     */
    private boolean arrivedOrSettled(StepContext context, BooleanSupplier landed, BooleanSupplier idle, BooleanSupplier ignored)
    {
        if (landed.getAsBoolean())
        {
            return true;
        }
        int waited = (int) context.getOrDefault(ARRIVAL_WAITED, 0) + 1;
        context.put(ARRIVAL_WAITED, waited);
        if (waited <= ARRIVAL_TICKS)
        {
            return false;
        }
        boolean stoodStill = ignored.getAsBoolean();
        int clicks = context.getOrDefault(CLICKS, 1);
        if (stoodStill && clicks < CLICK_TRIES)
        {
            context.put(CLICKS, clicks + 1);
            context.put(ARRIVAL_WAITED, 0);
            click();
            return false;
        }
        return idle.getAsBoolean() || stoodStill;
    }

    /**
     * Whether the player has come through: within two tiles of the destination on its plane, measured from the world tile the player stands on or copies inside an instance, and off the tile it leaves from.
     *
     * @return whether the player has arrived, or there is no player to wait for
     */
    public boolean arrived()
    {
        WorldPoint at = Static.invoke(() ->
        {
            Client client = Static.getClient();
            Player player = client.getLocalPlayer();
            return player == null ? null : WorldPointUtil.fromInstance(player.getWorldLocation());
        });
        if (at == null)
        {
            return true;
        }
        WorldPoint to = destination.toWorldPoint();
        return !at.equals(location.toWorldPoint()) && at.getPlane() == to.getPlane() && at.distanceTo2D(to) <= ARRIVAL_REACH;
    }

    /**
     * Works the tiles' packed keys out again from their coordinates, for an edge read from a file.
     */
    void repack()
    {
        location.repack();
        destination.repack();
    }

    /**
     * @return what it is called
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the loc or NPC id, -1 for a code-defined transport
     */
    public int getId()
    {
        return id;
    }

    /**
     * @return whether it is a loc rather than an NPC
     */
    public boolean isObject()
    {
        return isObject;
    }

    /**
     * @return the tile it leaves from
     */
    public Loc getLocation()
    {
        return location;
    }

    /**
     * @return the tile it lands on
     */
    public Loc getDestination()
    {
        return destination;
    }

    /**
     * @return the 1-based option used on it
     */
    public int getOption()
    {
        return option;
    }

    /**
     * @return the dialogue options chosen in order, or null
     */
    public String[] getDialogueOptions()
    {
        return dialogueOptions;
    }

    /**
     * @return how many ticks it takes
     */
    public int getDuration()
    {
        return duration;
    }

    /**
     * The edge's identity for de-duplication: loc or NPC, id, option, both tiles and the dialogue; duration, name and requirements are left out.
     *
     * @return the key
     */
    public String identityKey()
    {
        return isObject + "|" + id + "|" + option + "|" + location.getPacked() + "|" + destination.getPacked() + "|" + (dialogueOptions == null ? "" : String.join("", dialogueOptions));
    }
}
