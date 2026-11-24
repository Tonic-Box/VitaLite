package com.tonic.services.pathfinder.recorder;

import com.tonic.Static;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.collision.CollisionMap;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Turns one manual traversal into a Transport: armed by an object or NPC option, it follows the player each tick until a transport-style move, then records the walk-off tile, the settle tile, the dialogue chosen and the idle ticks spent. Runs on the client thread.
 */
final class RecordingSession
{
    private enum State
    {
        IDLE,
        ARMED,
        SETTLING
    }

    private static final int ABORT_TICKS = 30;
    private static final int SETTLE_MAX = 4;

    private State state = State.IDLE;
    private volatile boolean membersOnly;

    private boolean isObject;
    private int id;
    private int option;
    private String name;

    private int interactionTick;
    private int idleTicks;
    private final List<String> dialogueOptions = new ArrayList<>();

    private WorldPoint wallTile;
    private int wallShape;
    private int wallRotation;

    private WorldPoint previousTile;
    private WorldPoint lastWalkableTile;
    private WorldPoint startTile;
    private WorldPoint settleTile;
    private int settleStartTick;
    private int lastProcessedTick = -1;

    void membersOnly(boolean value)
    {
        membersOnly = value;
    }

    boolean arm(boolean object, int optionIndex, int targetId, String targetName, WorldPoint wall, int shape, int rotation)
    {
        Client client = Static.getClient();
        Player local = client.getLocalPlayer();
        if (optionIndex < 1 || local == null)
        {
            return false;
        }

        WorldPoint playerTile = local.getWorldLocation();
        this.isObject = object;
        this.id = targetId;
        this.option = optionIndex;
        this.name = targetName;
        this.wallTile = wall;
        this.wallShape = shape;
        this.wallRotation = rotation;
        this.interactionTick = client.getTickCount();
        this.idleTicks = 0;
        this.dialogueOptions.clear();
        this.previousTile = playerTile;
        this.lastWalkableTile = WorldPointUtil.fromInstance(playerTile);
        this.startTile = null;
        this.settleTile = null;
        this.lastProcessedTick = -1;
        this.state = State.ARMED;
        return true;
    }

    void onDialogueOption(String chosen)
    {
        if (state == State.IDLE || chosen == null || chosen.isEmpty())
        {
            return;
        }

        dialogueOptions.add(chosen);
        interactionTick = currentTick();
        idleTicks = 0;
    }

    void onDialogueContinue()
    {
        if (state == State.IDLE)
        {
            return;
        }

        interactionTick = currentTick();
        idleTicks = 0;
    }

    Transport onTick()
    {
        if (state == State.IDLE)
        {
            return null;
        }

        Client client = Static.getClient();
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return null;
        }

        WorldPoint current = local.getWorldLocation();
        WorldPoint template = WorldPointUtil.fromInstance(current);
        int now = client.getTickCount();
        if (now == lastProcessedTick)
        {
            return null;
        }
        lastProcessedTick = now;

        if (state == State.SETTLING)
        {
            if (template.equals(settleTile) && !MovementAPI.isMoving())
            {
                return finish();
            }
            settleTile = template;
            if (now - settleStartTick > SETTLE_MAX)
            {
                return finish();
            }
            return null;
        }

        if (previousTile != null && transportStep(previousTile, current, wallTile, wallShape, wallRotation, tile -> isWalkable(WorldPointUtil.fromInstance(tile))))
        {
            startTile = lastWalkableTile != null ? lastWalkableTile : WorldPointUtil.fromInstance(previousTile);
            settleTile = template;
            settleStartTick = now;
            previousTile = current;
            state = State.SETTLING;
            return null;
        }

        if (!MovementAPI.isMoving())
        {
            idleTicks++;
        }

        if (!DialogueAPI.dialoguePresent() && now - interactionTick > ABORT_TICKS)
        {
            reset();
            return null;
        }

        if (isWalkable(template))
        {
            lastWalkableTile = template;
        }
        previousTile = current;
        return null;
    }

    boolean isActive()
    {
        return state != State.IDLE;
    }

    String status()
    {
        if (state == State.IDLE)
        {
            return "idle";
        }

        int ticks = currentTick() - interactionTick;
        return (state == State.SETTLING ? "settling " : "recording ") + name + " opt" + option + " " + ticks + "t" + (dialogueOptions.isEmpty() ? "" : " diag=" + dialogueOptions.size());
    }

    void reset()
    {
        state = State.IDLE;
        dialogueOptions.clear();
        idleTicks = 0;
        previousTile = null;
        lastWalkableTile = null;
        startTile = null;
        settleTile = null;
        wallTile = null;
        lastProcessedTick = -1;
    }

    static boolean transportStep(WorldPoint previous, WorldPoint current, WorldPoint wall, int wallShape, int wallRotation, Predicate<WorldPoint> walkable)
    {
        if (previous.getPlane() != current.getPlane())
        {
            return true;
        }

        int distance = previous.distanceTo(current);
        if (distance > 2)
        {
            return true;
        }
        if (distance == 1)
        {
            return wall != null && WallCrossing.crosses(wall, wallShape, wallRotation, previous, current);
        }
        if (distance == 2)
        {
            int middleX = (previous.getX() + current.getX()) / 2;
            int middleY = (previous.getY() + current.getY()) / 2;
            return walkable != null && !walkable.test(new WorldPoint(middleX, middleY, current.getPlane()));
        }
        return false;
    }

    private Transport finish()
    {
        int duration = Math.max(1, idleTicks);
        Requirements requirements = null;
        if (membersOnly)
        {
            requirements = new Requirements();
            requirements.addRequirement(new WorldRequirement(true));
        }

        Transport record = new Transport(name, id, isObject, startTile, settleTile, option, dialogueOptions.toArray(new String[0]), duration, requirements);
        reset();
        return record;
    }

    private static int currentTick()
    {
        Client client = Static.getClient();
        return client.getTickCount();
    }

    private static boolean isWalkable(WorldPoint tile)
    {
        CollisionMap map = Walker.getCollisionMap();
        return map == null || map.walkable((short) tile.getX(), (short) tile.getY(), (byte) tile.getPlane());
    }
}
