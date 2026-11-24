package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.TileObjectAPI;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Trapdoors and manholes that may be shut when reached: opened first when needed, then climbed down.
 */
public final class Trapdoors
{
    private static final String OPEN = "Open";
    private static final String CLIMB_DOWN = "Climb-down";
    private static final int SEARCH_RADIUS = 5;
    private static final int DURATION = 1;
    private static final int ARRIVAL_TIMEOUT = 10;

    private Trapdoors()
    {
    }

    /**
     * Adds the way down through every trapdoor and manhole.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        Requirements members = new Requirements();
        members.addRequirement(new WorldRequirement(true));

        add.accept(trapdoor("Trapdoor", ObjectID.TOL_TRAPDOOR01, ObjectID.TOL_TRAPDOOR_OPEN, new WorldPoint(2648, 3213, 0), new WorldPoint(3038, 4376, 0), members));
        add.accept(trapdoor("Trapdoor", ObjectID.TRAPDOOR, ObjectID.TRAPDOOR_OPEN, new WorldPoint(3405, 3506, 0), new WorldPoint(3405, 9906, 0), members));
        add.accept(trapdoor("Trapdoor", ObjectID.PIPEASTSIDETRAPDOOR, ObjectID.PIPEASTSIDETRAPDOOR_OPEN, new WorldPoint(3423, 3485, 0), new WorldPoint(3440, 9887, 0), members));
        add.accept(trapdoor("Trapdoor", ObjectID.PIPEASTSIDETRAPDOOR, ObjectID.PIPEASTSIDETRAPDOOR_OPEN, new WorldPoint(3422, 3484, 0), new WorldPoint(3440, 9887, 0), members));
        add.accept(trapdoor("Trapdoor", ObjectID.TRAPDOOR, ObjectID.TRAPDOOR_OPEN, new WorldPoint(3096, 3468, 0), new WorldPoint(3096, 9867, 0), null));
        add.accept(trapdoor("Manhole", ObjectID.MANHOLECLOSED, ObjectID.MANHOLEOPEN, new WorldPoint(3236, 3458, 0), new WorldPoint(3237, 9858, 0), null));
        add.accept(trapdoor("Trapdoor", ObjectID.AHOY_TRAPDOOR, ObjectID.AHOY_TRAPDOOR_OPEN, new WorldPoint(3654, 3519, 0), new WorldPoint(3669, 9888, 3), null));
        add.accept(trapdoor("Trapdoor", ObjectID.VAMPIRE_TRAP1, ObjectID.VAMPIRE_TRAP2, new WorldPoint(3083, 3272, 0), new WorldPoint(3083, 9672, 0), null));
        add.accept(trapdoor("Trapdoor", ObjectID.VAMPIRE_TRAP1, ObjectID.VAMPIRE_TRAP2, new WorldPoint(3118, 3243, 0), new WorldPoint(3118, 9644, 0), null));
        add.accept(trapdoor("Trapdoor", ObjectID.VAMPIRE_TRAP1, ObjectID.VAMPIRE_TRAP2, new WorldPoint(3117, 3244, 0), new WorldPoint(3118, 9644, 0), null));
        add.accept(trapdoor("Trapdoor", ObjectID.VAMPIRE_TRAP1, ObjectID.VAMPIRE_TRAP2, new WorldPoint(3119, 3244, 0), new WorldPoint(3118, 9644, 0), null));
        add.accept(trapdoor("Trapdoor", ObjectID.VAMPIRE_TRAP1, ObjectID.VAMPIRE_TRAP2, new WorldPoint(3118, 3245, 0), new WorldPoint(3118, 9644, 0), null));
    }

    private static CodeTransport trapdoor(String name, int closedId, int openId, WorldPoint source, WorldPoint destination, Requirements requirements)
    {
        BooleanSupplier arrived = new Transport(name, openId, true, source, destination, 1, null, DURATION, null)::arrived;
        return new CodeTransport(name, source, destination, DURATION, requirements, () -> climbDown(closedId, openId, source, arrived));
    }

    private static StepHandler climbDown(int closedId, int openId, WorldPoint source, BooleanSupplier arrived)
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    TileObjectEx closed = nearest(closedId, source);
                    if (closed != null)
                    {
                        TileObjectAPI.interact(closed, OPEN);
                        return 1;
                    }
                    TileObjectEx open = nearest(openId, source);
                    if (open != null)
                    {
                        TileObjectAPI.interact(open, CLIMB_DOWN);
                        return 2;
                    }
                    return 0;
                })
                .add(1, () ->
                {
                    TileObjectEx open = nearest(openId, source);
                    if (open != null)
                    {
                        TileObjectAPI.interact(open, CLIMB_DOWN);
                        return 2;
                    }
                    return 0;
                })
                .addDelayUntil(2, ARRIVAL_TIMEOUT, arrived, () ->
                {
                })
                .build();
    }

    private static TileObjectEx nearest(int objectId, WorldPoint source)
    {
        return new TileObjectQuery().withId(objectId).within(source, SEARCH_RADIUS).first();
    }
}
