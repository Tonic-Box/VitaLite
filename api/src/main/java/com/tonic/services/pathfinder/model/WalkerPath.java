package com.tonic.services.pathfinder.model;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.PrayerAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.*;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.InventoryQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.queries.WidgetQuery;
import static com.tonic.services.pathfinder.Walker.*;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.abstractions.IPathfinder;
import com.tonic.services.pathfinder.abstractions.IStep;
import com.tonic.services.pathfinder.teleports.Teleport;
import com.tonic.util.ClickManagerUtil;
import com.tonic.util.Location;
import com.tonic.util.StaticIntFinder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import org.apache.commons.lang3.ArrayUtils;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WalkerPath
{
    private static final int[] STAMINA = {net.runelite.api.gameval.ItemID._1DOSESTAMINA, net.runelite.api.gameval.ItemID._2DOSESTAMINA, net.runelite.api.gameval.ItemID._3DOSESTAMINA, ItemID._4DOSESTAMINA};

    private final Client client;
    @Getter
    private final List<IStep> steps;
    @Getter
    private boolean canceled = false;
    @Setter
    private Teleport teleport;
    @Setter
    private int healthHandler = 0;
    @Setter
    private PrayerAPI[] prayers = null;
    private final int prayerDangerZone;
    private WorldPoint destination;
    private boolean ranInit = false;
    private int timesDialogueSeen = 0;
    private String lastText = null;

    /**
     * Get a WalkerPath to a single target
     * @param target The target WorldPoint
     * @return The WalkerPath
     */
    public static WalkerPath get(WorldPoint target)
    {
        target = Walker.getCollisionMap().nearestWalkableEuclidean(target, 5);
        final IPathfinder engine = Static.getVitaConfig().getPathfinderImpl().newInstance();
        List<? extends IStep> path = engine.find(target);
        return new WalkerPath(path, engine.getTeleport());
    }

    /**
     * Get a WalkerPath to the closest of multiple targets
     * @param targets The target WorldAreas
     * @return The WalkerPath
     */
    public static WalkerPath get(List<WorldArea> targets)
    {
        final IPathfinder engine = Static.getVitaConfig().getPathfinderImpl().newInstance();
        List<? extends IStep> path = engine.find(targets);
        return new WalkerPath(path, engine.getTeleport());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    WalkerPath(List<? extends IStep> steps, Teleport teleport) {
        this.client = Static.getClient();
        this.steps = (List) steps;
        this.teleport = teleport;
        prayerDangerZone = client.getRealSkillLevel(Skill.PRAYER) / 2;
        if(!steps.isEmpty()) {
            destination = steps.get(steps.size() - 1).getPosition();
        }
    }

    public void cancel()
    {
        this.canceled = true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void repath()
    {
        steps.clear();
        final IPathfinder engine = Static.getVitaConfig().getPathfinderImpl().newInstance();
        ((List) steps).addAll(engine.find(destination));
    }

    /**
     * Initialize the WalkerPath
     */
    private void init()
    {
        if(ranInit)
        {
            return;
        }

        GameManager.setPathPoints(IStep.toWorldPoints(steps));

        if(prayers != null)
        {
            PrayerAPI.setQuickPrayer(prayers);
        }
        ranInit = true;
    }

    /**
     * Shutdown the WalkerPath (handles prayer cleanup)
     */
    public void shutdown()
    {
        if(prayers != null)
        {
            PrayerAPI.turnOffQuickPrayers();
        }
        GameManager.clearPathPoints();
    }

    public boolean isDone()
    {
        boolean value = steps.isEmpty();
        if(value)
        {
            shutdown();
        }
        return value;
    }

    /**
     * Perform a single step of the WalkerPath
     * @return true if there are more steps to perform, false otherwise
     */
    public boolean step()
    {
        init();

        if(canceled)
        {
            shutdown();
            return false;
        }

        if(steps == null || steps.isEmpty())
        {
            shutdown();
            return false;
        }

        if(!client.getGameState().equals(GameState.LOGGED_IN))
        {
            return !isDone();
        }

        if(handleTeleport())
        {
            return !isDone();
        }

        if(handleTransports())
        {
            return !isDone();
        }

        timesDialogueSeen = 0;
        lastText = null;

        if (shouldHandleDialogue(steps)) {
            handleDialogue();
            return !isDone();
        }

        if (steps.isEmpty()) {
            shutdown();
            return false;
        }

        handlePrayer();
        manageRunEnergyAndHitpoints();

        Player local = client.getLocalPlayer();
        WorldPoint last = local.getWorldLocation();
        IStep step = steps.get(0);
        return handleWalking(local, last, step, step.getPosition());
    }

    private boolean handleWalking(Player local, WorldPoint last, IStep step, WorldPoint dest) {
        if(!SceneAPI.isReachable(local.getWorldLocation(), step.getPosition())) {
            if (MovementAPI.isMoving()) {
                return true;
            }
            if (handlePassThroughObjects(local, steps, step) || !PlayerEx.getLocal().isIdle()) {
                return !isDone();
            }
            repath();
            return true;
        }

        int rand = ThreadLocalRandom.current().nextInt(8, 12);
        if (last.distanceTo2D(dest) > rand && !PlayerEx.getLocal().isIdle())
        {
            return true;
        }

        int s = 0;
        rand = ThreadLocalRandom.current().nextInt(10, 16);
        while(s <= rand && s < steps.size() && !steps.get(s).hasTransport())
        {
            if(!SceneAPI.isReachable(local.getWorldLocation(), steps.get(s).getPosition()))
            {
                break;
            }
            s++;
        }
        if(s > 0)
        {
            s--;
            steps.subList(0, s).clear();
        }
        step = steps.get(0);

        // Never remove transport steps here - they must be handled by handleTransports()
        if(step.hasTransport()) {
            return !isDone();
        }

        IStep nextStep = steps.size() > 1 ? steps.get(1) : null;
        // Don't consider next step "blocked" if it has a transport (transport handles the transition)
        boolean nextBlocked = nextStep != null && !nextStep.hasTransport() && !SceneAPI.isReachable(step.getPosition(), nextStep.getPosition());

        boolean atStepPos = local.getWorldLocation().equals(step.getPosition());
        boolean isMoving = MovementAPI.isMoving();

        MovementAPI.walkToWorldPoint(step.getPosition());
        if(isMoving || nextBlocked || atStepPos)
            steps.remove(step);
        return !isDone();
    }

    private boolean handlePassThroughObjects(Player local, List<? extends IStep> steps, IStep step)
    {
        TileObjectEx object = new TileObjectQuery()
                .withNamesContains("door", "gate", "curtain")
                .keepIf(o -> (o.getWorldPoint().equals(local.getWorldLocation()) || o.getWorldPoint().equals(step.getPosition())))
                .sortNearest()
                .first();

        if(StrongholdSecurityQuestion.process(object))
        {
            return true;
        }

        if(object != null)
        {
            ClickManagerUtil.queueClickBox(object);
            TileObjectAPI.interact(object, 0);
            Logger.info("[Pathfinder] Interacting with '" + object.getName() + "'");
            return true;
        }
        else {
            if (!PlayerEx.getLocal().isIdle())
            {
                return true;
            }
            Logger.info("[Pathfinder] Failed to find Passthrough, atempting to circumvent");
            repath();
            return true;
        }
    }

    private boolean handleTransports() {
        if(!PlayerEx.getLocal().isIdle() && !MovementAPI.isMoving())
        {
            return true;
        }

        //HACK to handle warnings dynamically
        Widget widget = new WidgetQuery(LayoutView.MAINMODEL.getCurrentID())
                .withTextContains("WARNING")
                .first();
        if(widget != null)
        {
            Static.invoke(() -> {
                Widget universe = widget.getParent();
                int id = universe.getId();
                String name = StaticIntFinder.find(InterfaceID.class, id);
                if(name != null && name.toLowerCase().contains("universe"))
                {
                    ClickManagerUtil.queueClickBoxInterface(id);
                    DialogueAPI.resumePause(id, 1);
                }
            });
        }

        IStep step = steps.get(0);
        if(!step.hasTransport())
        {
            return false;
        }

        //hack to handle unhandled dialogues
        if(DialogueAPI.dialoguePresent())
        {
            if(lastText == null)
            {
                timesDialogueSeen++;
                lastText = DialogueAPI.getDialogueText();
            }
            else if (DialogueAPI.getDialogueText().equals(lastText)) {
                timesDialogueSeen++;
                if(timesDialogueSeen > 2)
                {
                    if(!DialogueAPI.continueDialogue() && !DialogueAPI.selectOption("Yes") && !DialogueAPI.selectOption("Okay") && !DialogueAPI.selectOption("Don't ask again"))
                    {
                        DialogueAPI.selectOption(1);
                    }
                }
            }
        }
        else
        {
            timesDialogueSeen = 0;
            lastText = null;
        }

        boolean value = step.getTransport().getHandler().step();
        if(!value)
        {
            System.out.println("[Pathfinder] Transport complete, removing step");
            steps.remove(step);
        }
        else
        {
            System.out.println("[Pathfinder] Transport in progress...");
        }
        return value;
    }

    private boolean handleTeleport() {
        if (teleport != null) {
            boolean state = teleport.getHandlers().step();
            if(!state) {
                teleport = null;
            }
            return state;
        }
        return false;
    }

    private boolean shouldHandleDialogue(List<? extends IStep> steps) {
        return DialogueAPI.dialoguePresent() && !steps.isEmpty() && !SceneAPI.isReachable(PlayerEx.getLocal().getWorldPoint(), steps.get(steps.size() - 1).getPosition());
    }

    private void handleDialogue() {
        if(!DialogueAPI.continueDialogue())
        {
            if(!DialogueAPI.selectOption("Yes") && !DialogueAPI.selectOption("Okay") && !DialogueAPI.selectOption("Don't ask again"))
            {
                DialogueAPI.selectOption(1);
            }
        }
    }

    private void handlePrayer()
    {
        if(prayers != null)
        {
            PrayerAPI.flickQuickPrayer();
            if(client.getBoostedSkillLevel(Skill.PRAYER) < prayerDangerZone)
            {
                ItemEx pot = InventoryAPI.getItem(i -> ArrayUtils.contains(ItemConstants.PRAYER_POTION, i.getId()));
                if(pot != null)
                {
                    InventoryAPI.interact(pot, 1);
                }
            }
        }
    }

    private void manageRunEnergyAndHitpoints()
    {
        if(DialogueAPI.dialoguePresent())
        {
            return;
        }

        int energy = client.getEnergy() / 100;

        if(!MovementAPI.isRunEnabled() && energy > Setting.toggleRunThreshold)
        {
            WidgetAPI.interact(1, WidgetInfo.MINIMAP_TOGGLE_RUN_ORB, -1, -1);
            Setting.toggleRunThreshold = Setting.toggleRunRange.randomEnclosed();
        }

        if(energy < Setting.consumeStaminaThreshold && !MovementAPI.staminaInEffect())
        {
            ItemEx stam = InventoryAPI.getItem(i -> ArrayUtils.contains(STAMINA, i.getId()));

            if(stam != null)
            {
                InventoryAPI.interact(stam, 2);
                Setting.consumeStaminaThreshold = Setting.consumeStaminaRange.randomEnclosed();
            }
        }

        if(energy <= Setting.toggleRunThreshold)
        {
            ItemEx stam = InventoryAPI.getItem(i -> ArrayUtils.contains(STAMINA, i.getId()));

            if(stam != null)
            {
                InventoryAPI.interact(stam, 2);
                Setting.toggleRunThreshold = Setting.toggleRunRange.randomEnclosed();
            }
        }

        int threshold = healthHandler;
        int hitpoints = Static.invoke(() -> client.getBoostedSkillLevel(Skill.HITPOINTS));

        if(threshold != 0 && hitpoints <= threshold)
        {
            ItemEx item = InventoryQuery.fromInventoryId(InventoryID.INV)
                    .removeIf(i -> i.getName().contains("Banana") || i.getName().contains("Cabbage"))
                    .withAction("Eat")
                    .first();
            if(item != null)
                InventoryAPI.interact(item, 1);
        }
    }
}
