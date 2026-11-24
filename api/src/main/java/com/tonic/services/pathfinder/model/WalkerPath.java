package com.tonic.services.pathfinder.model;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.PrayerAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.ItemConstants;
import com.tonic.data.LayoutView;
import com.tonic.data.StrongholdSecurityQuestion;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.InventoryQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.queries.WidgetQuery;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.PlayerMotion;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.abstractions.IStep;
import com.tonic.services.pathfinder.collision.Exclusion;
import com.tonic.services.pathfinder.impl.BoundedReach;
import com.tonic.services.pathfinder.impl.LiveLayer;
import com.tonic.services.pathfinder.impl.Pathfinder;
import com.tonic.services.pathfinder.impl.WalkDistances;
import com.tonic.services.pathfinder.teleports.Teleport;
import com.tonic.services.pathfinder.teleports.Wilderness;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportAvailability;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.util.ClickManagerUtil;
import com.tonic.util.StaticIntFinder;
import com.tonic.util.ThreadPool;
import com.tonic.util.WorldPointUtil;
import com.tonic.util.handler.StepHandler;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A planned walk, advanced one game tick at a time by step(): it fires the teleport the route starts with, walks the route checking each click against the walk the scene allows, opens the doors in the way, takes each transport, and replans when the way is blocked, a transport's requirements stop holding, or a teleport or transport keeps failing. Plans are made off the client thread.
 */
public class WalkerPath
{
    private static final int NEARBY_TILES = 250_000;
    private static final int DETOUR_SLACK = 3;
    private static final int STUCK_TICKS = 3;
    private static final int TELEPORT_TRIES = 3;
    private static final int TRANSPORT_TRIES = 2;
    private static final int CONTINUE_HOLD_STEPS = 10;
    private static final int BLOCKED_STEP_LOOKAHEAD = 3;
    private static final int MAX_CLICK_DISTANCE = 17;
    private static final int TRACE_REPEAT_TICKS = 10;
    private static final int[] STAMINA = {ItemID._1DOSESTAMINA, ItemID._2DOSESTAMINA, ItemID._3DOSESTAMINA, ItemID._4DOSESTAMINA, ItemID._1DOSE2STAMINA, ItemID._2DOSE2STAMINA, ItemID._3DOSE2STAMINA, ItemID._4DOSE2STAMINA};

    private final Client client;
    @Getter
    private final List<IStep> steps;
    @Getter
    private boolean canceled = false;
    @Getter
    private Teleport teleport;
    @Setter
    private int healthHandler = 0;
    @Setter
    private PrayerAPI[] prayers = null;
    private final int prayerDangerZone;
    private final boolean useTeleports;
    private WorldPoint destination;
    private WorldPoint liveDestination;
    private WorldPoint passed;
    private WorldPoint clickedWhileBusy;
    private boolean ranInit = false;
    private int timesDialogueSeen = 0;
    private String lastText = null;
    private String heldPage;
    private int heldSteps;
    private final Set<String> failedTeleports = new HashSet<>();
    private final Map<String, Integer> teleportTries = new HashMap<>();
    private final Set<Transport> failedTransports = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Transport, Integer> transportTries = new IdentityHashMap<>();
    private StepHandler transportHandler;
    private StepHandler teleportHandler;
    private volatile CompletableFuture<Plan> replanning;
    private String lastNote;
    private int sameNotes;

    private static final class Plan
    {
        private final List<? extends IStep> steps;
        private final Teleport teleport;

        private Plan(List<? extends IStep> steps, Teleport teleport)
        {
            this.steps = steps == null ? Collections.emptyList() : steps;
            this.teleport = teleport;
        }
    }

    private static final class Snapshot
    {
        private final LiveLayer live;
        private final WorldPoint start;
        private final boolean instanced;
        private final int baseX;
        private final int baseY;
        private final int size;

        private Snapshot(LiveLayer live, WorldPoint start, boolean instanced, int baseX, int baseY, int size)
        {
            this.live = live;
            this.start = start;
            this.instanced = instanced;
            this.baseX = baseX;
            this.baseY = baseY;
            this.size = size;
        }
    }

    /**
     * Plans a walk to a tile.
     *
     * @param target the tile
     * @return the walk
     */
    public static WalkerPath get(WorldPoint target)
    {
        return get(target, true);
    }

    /**
     * Plans a walk to a tile, optionally without teleports; replans keep the same choice.
     *
     * @param target the tile
     * @param useTeleports whether the route may start with a teleport or take a teleport-like network
     * @return the walk
     */
    public static WalkerPath get(WorldPoint target, boolean useTeleports)
    {
        return new WalkerPath(plan(target, null, useTeleports, Collections.emptySet(), Collections.emptySet(), Pathfinder.MOST_TILES), useTeleports);
    }

    /**
     * Plans a walk to the nearest of several areas.
     *
     * @param targets the areas
     * @return the walk
     */
    public static WalkerPath get(List<WorldArea> targets)
    {
        return new WalkerPath(plan(null, targets, true, Collections.emptySet(), Collections.emptySet(), Pathfinder.MOST_TILES), true);
    }

    /**
     * Plans a walk to the nearest tile a loc can be used from.
     *
     * @param object the loc
     * @return the walk, empty when no such tile can be walked to nearby
     */
    public static WalkerPath get(TileObjectEx object)
    {
        List<WorldArea> areas = new ArrayList<>();
        for (WorldPoint tile : object.interactableFrom())
        {
            areas.add(new WorldArea(tile, 1, 1));
        }
        return nearby(areas);
    }

    /**
     * Plans a walk to the nearest of several areas on the loaded scene, only when a route inside the scene exists, so a target walled off from everywhere is given up on at once rather than after a search of the whole world.
     *
     * @param targets the areas
     * @return the walk, empty when none of them can be walked to nearby
     */
    public static WalkerPath nearby(List<WorldArea> targets)
    {
        if (!routableWithinScene(targets))
        {
            return new WalkerPath(new Plan(Collections.emptyList(), null), true);
        }
        return new WalkerPath(plan(null, targets, true, Collections.emptySet(), Collections.emptySet(), NEARBY_TILES), true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private WalkerPath(Plan plan, boolean useTeleports)
    {
        this.client = Static.getClient();
        this.useTeleports = useTeleports;
        this.steps = new ArrayList<>((List) plan.steps);
        this.teleport = plan.teleport;
        this.prayerDangerZone = Static.invoke(() -> client.getRealSkillLevel(Skill.PRAYER) / 2);
        if (!steps.isEmpty())
        {
            IStep last = steps.get(steps.size() - 1);
            destination = WorldPointUtil.fromCompressed(last.getPackedPosition());
            liveDestination = last.getPosition();
        }
    }

    private static Plan plan(WorldPoint target, List<WorldArea> areas, boolean useTeleports, Set<String> failed, Set<Transport> failedTransports, int mostTiles)
    {
        List<Teleport> teleports = useTeleports ? new ArrayList<>(Teleport.buildTeleportLinks()) : new ArrayList<>();
        boolean intoTheWilderness = target != null ? Wilderness.level(target) > 0 : areas.stream().anyMatch(Wilderness::touches);
        if (!intoTheWilderness)
        {
            teleports.removeIf(candidate -> Wilderness.level(candidate.getDestination()) > 0);
        }
        teleports.removeIf(candidate -> failed.contains(candidate.getName()));
        TransportAvailability availability = TransportAvailability.of(TransportLoader.all(), useTeleports).without(failedTransports);
        Snapshot snapshot = snapshot();
        if (snapshot.start == null)
        {
            return new Plan(Collections.emptyList(), null);
        }
        Pathfinder engine = new Pathfinder(TransportLoader.getTransports(), availability, teleports, Pathfinder.TELEPORT_PENALTY, snapshot.live).limitedTo(mostTiles);
        List<? extends IStep> path = target != null ? engine.find(snapshot.start, snap(snapshot.live, target)) : engine.find(snapshot.start, areas);
        return new Plan(path, engine.getTeleport());
    }

    private static Snapshot snapshot()
    {
        return Static.invoke(() ->
        {
            Client client = Static.getClient();
            Player player = client.getLocalPlayer();
            WorldView view = client.getTopLevelWorldView();
            WorldPoint start = player == null ? null : player.getWorldLocation();
            boolean instanced = view != null && view.isInstance();
            int baseX = view == null ? 0 : view.getBaseX();
            int baseY = view == null ? 0 : view.getBaseY();
            int size = view == null ? 0 : view.getSizeX();
            return new Snapshot(LiveLayer.of(client), start, instanced, baseX, baseY, size);
        });
    }

    private static boolean routableWithinScene(List<WorldArea> targets)
    {
        Snapshot snapshot = snapshot();
        if (snapshot.start == null)
        {
            return false;
        }
        if (snapshot.instanced)
        {
            return true;
        }
        TransportAvailability availability = TransportAvailability.of(TransportLoader.all(), true);
        return new BoundedReach(Walker.getCollisionMap(), TransportLoader.getTransports(), availability, snapshot.baseX, snapshot.baseY, snapshot.size).anyReachable(snapshot.start, targets);
    }

    private static WorldPoint snap(LiveLayer live, WorldPoint target)
    {
        if (target == null)
        {
            return null;
        }
        for (int key : live.liveKeys(WorldPointUtil.compress(target)))
        {
            if (live.walkable(key))
            {
                return target;
            }
        }
        return Walker.getCollisionMap().nearestWalkableEuclidean(target, 5);
    }

    /**
     * Swaps the teleport the route begins with.
     *
     * @param teleport the teleport, or null
     */
    public void setTeleport(Teleport teleport)
    {
        this.teleport = teleport;
        this.teleportHandler = null;
    }

    /**
     * Stops the walk at its next step.
     */
    public void cancel()
    {
        this.canceled = true;
    }

    /**
     * Ends the walk's prayers and clears its drawn route.
     */
    public void shutdown()
    {
        if (prayers != null)
        {
            PrayerAPI.turnOffQuickPrayers();
        }
        GameManager.clearPathPoints();
    }

    /**
     * @return whether every step is done, shutting the walk down when it is
     */
    public boolean isDone()
    {
        boolean value = steps.isEmpty() && replanning == null;
        if (value)
        {
            shutdown();
        }
        return value;
    }

    private void init()
    {
        if (ranInit)
        {
            return;
        }
        GameManager.setPathPoints(IStep.toWorldPoints(steps));
        note("planned " + steps.size() + " steps to " + destination + (teleport == null ? "" : " starting with teleport " + teleport.getName()));
        if (prayers != null)
        {
            PrayerAPI.setQuickPrayer(prayers);
        }
        ranInit = true;
    }

    /**
     * Performs a single tick of the walk.
     *
     * @return true while there is more to do
     */
    public boolean step()
    {
        init();

        if (canceled)
        {
            shutdown();
            return false;
        }

        CompletableFuture<Plan> pending = replanning;
        if (pending != null)
        {
            if (!pending.isDone())
            {
                return true;
            }
            replanning = null;
            apply(pending.join());
        }

        if (steps.isEmpty())
        {
            shutdown();
            return false;
        }

        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return true;
        }

        handlePrayer();
        manageRunEnergyAndHitpoints();

        if (continuePastDialogue())
        {
            return !isDone();
        }

        if (teleport != null && handleTeleport())
        {
            return !isDone();
        }

        if (steps.isEmpty())
        {
            shutdown();
            return false;
        }

        if (handleTransports())
        {
            return !isDone();
        }

        timesDialogueSeen = 0;
        lastText = null;

        if (shouldHandleDialogue())
        {
            handleDialogue();
            return !isDone();
        }

        return handleWalking();
    }

    private boolean handleWalking()
    {
        WorldPoint here = PlayerEx.getLocal().getWorldPoint();
        IStep step = steps.get(0);
        WorldPoint dest = step.getPosition();
        WalkDistances distances = WalkDistances.from(here);
        int routeSteps = routeStepsTo(distances, here, dest);
        if (!onRoute(distances, dest, routeSteps))
        {
            note("next tile " + dest + " off route from " + here + ": live walk " + distances.to(dest) + ", route allows " + (routeSteps + DETOUR_SLACK) + ", moving=" + MovementAPI.isMoving());
            if (skipBlockedSteps(distances, routeSteps))
            {
                return !isDone();
            }
            if (!settled())
            {
                return true;
            }
            if (!handlePassThroughObjects(here, dest))
            {
                repath("the next tile " + dest + " is off route from " + here + " and no door stands on either");
            }
            return !isDone();
        }

        int rand = ThreadLocalRandom.current().nextInt(8, 12);
        if (here.distanceTo2D(dest) > rand && MovementAPI.isMoving())
        {
            return true;
        }

        int anchor = distances.to(dest);
        int s = 0;
        rand = ThreadLocalRandom.current().nextInt(10, 16);
        while (s <= rand && s < steps.size())
        {
            if (steps.get(s).hasTransport() || steps.get(s).getPosition().distanceTo2D(here) > MAX_CLICK_DISTANCE || !onRoute(distances, steps.get(s).getPosition(), anchor + s))
            {
                break;
            }
            s++;
        }
        int clicked = Math.max(s - 1, 0);
        if (clicked > 0)
        {
            passed = steps.get(clicked - 1).getPosition();
            steps.subList(0, clicked).clear();
        }
        step = steps.get(0);
        WorldPoint stepTile = step.getPosition();

        IStep nextStep = steps.size() > 1 ? steps.get(1) : null;
        boolean nextBlocked = nextStep != null && !onRoute(distances, nextStep.getPosition(), anchor + clicked + 1);
        boolean atStepPos = here.equals(stepTile);
        boolean isMoving = MovementAPI.isMoving();
        boolean busy = VarAPI.getVar(VarbitID.BUSY) == 1;
        if (!settled() && flaggedAt(stepTile))
        {
            note("already heading for " + stepTile + " from " + here + ", steps left " + steps.size());
        }
        else if (busy && stepTile.equals(clickedWhileBusy))
        {
            note("holding the click on " + stepTile + " while busy, from " + here);
        }
        else
        {
            MovementAPI.walkToWorldPoint(stepTile);
            clickedWhileBusy = busy ? stepTile : null;
            note("clicked " + stepTile + " (" + clicked + " ahead) from " + here + ", moving=" + isMoving + ", nextBlocked=" + nextBlocked + ", busy=" + busy + ", steps left " + steps.size());
        }
        if (!step.hasTransport() && (isMoving || nextBlocked || atStepPos))
        {
            passed = step.getPosition();
            steps.remove(step);
        }
        return !isDone();
    }

    /**
     * Whether the player has stopped: no walk flag, or one left standing over a player who has not stepped for a few ticks, as the server leaves it when a walk is cut short.
     */
    private boolean settled()
    {
        return !MovementAPI.isMoving() || PlayerMotion.stillTicks() >= STUCK_TICKS;
    }

    private static boolean flaggedAt(WorldPoint tile)
    {
        WorldPoint flag = Static.invoke(MovementAPI::getDestinationWorldPoint);
        return flag != null && flag.getX() == tile.getX() && flag.getY() == tile.getY();
    }

    private int routeStepsTo(WalkDistances distances, WorldPoint from, WorldPoint next)
    {
        int toPassed = passed == null ? WalkDistances.UNREACHABLE : distances.to(passed);
        return toPassed == WalkDistances.UNREACHABLE ? from.distanceTo2D(next) : toPassed + 1;
    }

    private static boolean onRoute(WalkDistances distances, WorldPoint tile, int routeSteps)
    {
        int walk = distances.to(tile);
        return walk != WalkDistances.UNREACHABLE && walk <= routeSteps + DETOUR_SLACK;
    }

    /**
     * Drops route steps blocked on the live scene when one a few steps further on is still reached within the route's slack, as when a door swings open onto the tile past it.
     */
    private boolean skipBlockedSteps(WalkDistances distances, int routeSteps)
    {
        for (int ahead = 1; ahead <= BLOCKED_STEP_LOOKAHEAD && ahead < steps.size(); ahead++)
        {
            if (steps.get(ahead - 1).hasTransport())
            {
                return false;
            }
            if (onRoute(distances, steps.get(ahead).getPosition(), routeSteps + ahead))
            {
                note("skipping " + ahead + " step(s) blocked on the live scene to " + steps.get(ahead).getPosition());
                passed = steps.get(ahead - 1).getPosition();
                steps.subList(0, ahead).clear();
                return true;
            }
        }
        return false;
    }

    /**
     * Gets through the door, gate or curtain standing on the player's tile or the step tile: a Stronghold of Security door by its question handler, any other by its first option; one already open is never clicked.
     *
     * @return whether there was one to open
     */
    private boolean handlePassThroughObjects(WorldPoint here, WorldPoint stepTile)
    {
        TileObjectEx door = new TileObjectQuery()
                .keepIf(object -> Exclusion.opensOnTheWay(object.getName()) && !standsOpen(object) && (stands(object, here) || stands(object, stepTile)))
                .nearest(here);
        if (door == null)
        {
            return false;
        }
        note("opening " + door.getName() + " at " + door.getWorldPoint());
        if (StrongholdSecurityQuestion.process(door))
        {
            return true;
        }
        ClickManagerUtil.queueClickBox(door);
        TileObjectAPI.interact(door, 0);
        return true;
    }

    private static boolean stands(TileObjectEx object, WorldPoint tile)
    {
        WorldArea area = object.getWorldArea();
        return area != null && area.getPlane() == tile.getPlane() && tile.getX() >= area.getX() && tile.getX() < area.getX() + Math.max(1, area.getWidth()) && tile.getY() >= area.getY() && tile.getY() < area.getY() + Math.max(1, area.getHeight());
    }

    private static boolean standsOpen(TileObjectEx object)
    {
        String[] actions = object.getActions();
        List<String> list = actions == null ? Collections.emptyList() : Arrays.asList(actions);
        return list.contains("Close") && !list.contains("Open");
    }

    /**
     * Carries on past a continue page that shows up mid-walk, such as a level-up, holding the walk for that tick; a transport or teleport under way answers its own dialogue, and no choice is made here.
     */
    private boolean continuePastDialogue()
    {
        if (transportHandler != null || teleportHandler != null)
        {
            return false;
        }
        if (!DialogueAPI.dialoguePresent() || !DialogueAPI.getOptions().isEmpty())
        {
            heldPage = null;
            heldSteps = 0;
            return false;
        }
        String page = DialogueAPI.getDialogueText();
        if (!Objects.equals(page, heldPage))
        {
            heldPage = page;
            heldSteps = 0;
        }
        if (++heldSteps > CONTINUE_HOLD_STEPS)
        {
            return false;
        }
        DialogueAPI.continueDialogue();
        note("continuing past: " + page);
        return true;
    }

    /**
     * Fires the teleport the route starts with and holds the walk until it lands; one that did not land is fired again, up to TELEPORT_TRIES tries, then the walk is replanned without it.
     */
    private boolean handleTeleport()
    {
        if (teleportHandler == null)
        {
            teleportHandler = teleport.buildHandler();
            note("firing teleport " + teleport.getName());
        }
        if (teleportHandler.step())
        {
            return true;
        }
        boolean landed = teleport.arrived();
        String name = teleport.getName();
        teleportHandler = null;
        note("teleport " + name + (landed ? " landed" : " did not land"));
        if (!landed && teleportTries.merge(name, 1, Integer::sum) < TELEPORT_TRIES)
        {
            return true;
        }
        teleport = null;
        if (!landed)
        {
            failedTeleports.add(name);
            repath("teleport " + name + " did not land");
            return true;
        }
        return false;
    }

    private boolean handleTransports()
    {
        answerUniverseWarning();

        IStep step = steps.get(0);
        if (!step.hasTransport())
        {
            transportHandler = null;
            return false;
        }
        WorldPoint here = PlayerEx.getLocal().getWorldPoint();
        if (transportHandler == null && !here.equals(step.getPosition()))
        {
            return false;
        }

        if (DialogueAPI.dialoguePresent())
        {
            String text = DialogueAPI.getDialogueText();
            if (lastText == null)
            {
                timesDialogueSeen++;
                lastText = text;
            }
            else if (Objects.equals(text, lastText))
            {
                timesDialogueSeen++;
                if (timesDialogueSeen > 2 && !DialogueAPI.continueDialogue() && !DialogueAPI.selectOption("Yes") && !DialogueAPI.selectOption("Okay") && !DialogueAPI.selectOption("Don't ask again"))
                {
                    DialogueAPI.selectOption(1);
                }
            }
        }
        else
        {
            timesDialogueSeen = 0;
            lastText = null;
        }

        if (transportHandler == null)
        {
            Transport transport = step.getTransport();
            if (transport.hasRequirements() && !Static.invoke(() -> transport.getRequirements().fulfilled()))
            {
                repath("transport " + transport.getName() + " is no longer usable");
                return true;
            }
            transportHandler = transport.buildHandler();
            note("taking transport " + transport.getName() + " at " + step.getPosition() + " to " + transport.getDestination().toWorldPoint());
        }
        boolean value = transportHandler.step();
        if (!value)
        {
            Transport transport = step.getTransport();
            note("transport " + transport.getName() + " done, at " + PlayerEx.getLocal().getWorldPoint());
            transportHandler = null;
            passed = null;
            steps.remove(step);
            if (transport.failed())
            {
                if (transportTries.merge(transport, 1, Integer::sum) >= TRANSPORT_TRIES)
                {
                    failedTransports.add(transport);
                }
                repath("transport " + transport.getName() + " at " + transport.getLocation().toWorldPoint() + " left the player where it started");
                return true;
            }
        }
        return value;
    }

    private void answerUniverseWarning()
    {
        Widget widget = new WidgetQuery(LayoutView.MAINMODEL.getCurrentID()).withTextContains("WARNING").first();
        if (widget == null)
        {
            return;
        }
        Static.invoke(() ->
        {
            Widget universe = widget.getParent();
            int id = universe.getId();
            String name = StaticIntFinder.find(InterfaceID.class, id);
            if (name != null && name.toLowerCase().contains("universe"))
            {
                ClickManagerUtil.queueClickBoxInterface(id);
                DialogueAPI.resumePause(id, 1);
            }
        });
    }

    private boolean shouldHandleDialogue()
    {
        return DialogueAPI.dialoguePresent() && !DialogueAPI.getOptions().isEmpty() && liveDestination != null && !SceneAPI.isReachable(PlayerEx.getLocal().getWorldPoint(), liveDestination);
    }

    private void handleDialogue()
    {
        if (!DialogueAPI.selectOption("Yes") && !DialogueAPI.selectOption("Okay") && !DialogueAPI.selectOption("Don't ask again"))
        {
            DialogueAPI.selectOption(1);
        }
    }

    /**
     * Plans the walk again from where the player stands, off the client thread when called on it; the walk holds until the new plan lands.
     */
    private void repath(String why)
    {
        Logger.info("[Pathfinder] Replanning: " + why);
        lastNote = null;
        steps.clear();
        passed = null;
        teleport = null;
        teleportHandler = null;
        transportHandler = null;
        if (destination == null)
        {
            return;
        }
        WorldPoint target = destination;
        Set<String> failed = new HashSet<>(failedTeleports);
        Set<Transport> skipped = Collections.newSetFromMap(new IdentityHashMap<>());
        skipped.addAll(failedTransports);
        if (((TClient) Static.getClient()).isClientThread())
        {
            CompletableFuture<Plan> future = new CompletableFuture<>();
            replanning = future;
            ThreadPool.submit(() ->
            {
                try
                {
                    future.complete(plan(target, null, useTeleports, failed, skipped, Pathfinder.MOST_TILES));
                }
                catch (RuntimeException e)
                {
                    future.complete(new Plan(Collections.emptyList(), null));
                }
            });
            return;
        }
        apply(plan(target, null, useTeleports, failed, skipped, Pathfinder.MOST_TILES));
    }

    /**
     * Logs what the walk is doing when walker tracing is on: each change once, and again every TRACE_REPEAT_TICKS ticks while it does not change.
     */
    private void note(String what)
    {
        if (!Static.getVitaConfig().shouldTraceWalker())
        {
            return;
        }
        if (what.equals(lastNote))
        {
            if (++sameNotes % TRACE_REPEAT_TICKS == 0)
            {
                Logger.info("[Walker] " + what + " (still, " + sameNotes + " ticks)");
            }
            return;
        }
        lastNote = what;
        sameNotes = 0;
        Logger.info("[Walker] " + what);
    }

    private void apply(Plan plan)
    {
        steps.clear();
        steps.addAll(plan.steps);
        teleport = plan.teleport;
        teleportHandler = null;
        note("planned " + steps.size() + " steps" + (teleport == null ? "" : " starting with teleport " + teleport.getName()));
        if (!steps.isEmpty())
        {
            liveDestination = steps.get(steps.size() - 1).getPosition();
        }
        GameManager.setPathPoints(IStep.toWorldPoints(steps));
    }

    private void handlePrayer()
    {
        if (prayers == null)
        {
            return;
        }
        PrayerAPI.flickQuickPrayer();
        if (client.getBoostedSkillLevel(Skill.PRAYER) < prayerDangerZone)
        {
            ItemEx pot = InventoryAPI.getItem(i -> ArrayUtils.contains(ItemConstants.PRAYER_POTION, i.getId()));
            if (pot != null)
            {
                InventoryAPI.interact(pot, 1);
            }
        }
    }

    private void manageRunEnergyAndHitpoints()
    {
        if (DialogueAPI.dialoguePresent())
        {
            return;
        }

        int energy = client.getEnergy() / 100;

        if (!MovementAPI.isRunEnabled() && energy > Walker.Setting.toggleRunThreshold)
        {
            WidgetAPI.interact(1, InterfaceID.Orbs.RUNBUTTON, -1, -1);
            Walker.Setting.toggleRunThreshold = Walker.Setting.toggleRunRange.randomEnclosed();
        }

        if (energy < Walker.Setting.consumeStaminaThreshold && !MovementAPI.staminaInEffect())
        {
            ItemEx stamina = InventoryAPI.getItem(i -> ArrayUtils.contains(STAMINA, i.getId()));
            if (stamina != null)
            {
                InventoryAPI.interact(stamina, 2);
                Walker.Setting.consumeStaminaThreshold = Walker.Setting.consumeStaminaRange.randomEnclosed();
            }
        }

        if (energy <= Walker.Setting.toggleRunThreshold)
        {
            ItemEx stamina = InventoryAPI.getItem(i -> ArrayUtils.contains(STAMINA, i.getId()));
            if (stamina != null)
            {
                InventoryAPI.interact(stamina, 2);
                Walker.Setting.toggleRunThreshold = Walker.Setting.toggleRunRange.randomEnclosed();
            }
        }

        int threshold = healthHandler;
        int hitpoints = Static.invoke(() -> client.getBoostedSkillLevel(Skill.HITPOINTS));
        if (threshold != 0 && hitpoints <= threshold)
        {
            ItemEx item = InventoryQuery.fromInventoryId(InventoryID.INV).removeIf(i -> i.getName().contains("Banana") || i.getName().contains("Cabbage")).withAction("Eat").first();
            if (item != null)
            {
                InventoryAPI.interact(item, 1);
            }
        }
    }
}
