package com.tonic.services.pathfinder;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.PrayerAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.ItemConstants;
import com.tonic.data.ItemEx;
import com.tonic.data.TileObjectEx;
import com.tonic.queries.InventoryQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.model.Step;
import com.tonic.services.pathfinder.teleports.Teleport;
import com.tonic.util.Coroutine;
import com.tonic.util.Location;
import com.tonic.util.WorldPointUtil;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.WidgetInfo;
import org.apache.commons.lang3.ArrayUtils;
import java.util.ArrayList;
import java.util.List;

public class Walker
{
    private final static Walker instance = new Walker();
    private final Client client;
    private boolean running = false;
    private final int[] stamina = {ItemID._1DOSESTAMINA,ItemID._2DOSESTAMINA,ItemID._3DOSESTAMINA,ItemID._4DOSESTAMINA};
    private int cooldown = 0;
    private int timeout = 0;
    private int recalcs = 0;
    @Setter
    private int healthHandler = 0;
    @Setter
    private PrayerAPI[] prayers = null;
    private final int prayerDangerZone;
    private int timer;
    private Teleport teleport;
    private final List<Runnable> multiSteps = new ArrayList<>();
    private int multiStepPointer = 0;

    private Walker()
    {
        this.client = Static.getClient();
        prayerDangerZone = client.getRealSkillLevel(Skill.PRAYER) / 2;
    }

    public static void cancelWalk()
    {
        instance.cancel();
    }

    public static boolean isWalking()
    {
        return instance.running;
    }

    public static void walkTo(WorldPoint target, int eatAtHp)
    {
        instance.setHealthHandler(eatAtHp);
        walkTo(target, true);
    }

    public static void walkTo(WorldPoint target, PrayerAPI... prayers)
    {
        instance.setPrayers(prayers);
        walkTo(target, true);
    }

    public static void walkTo(WorldPoint target, int eatAtHp, PrayerAPI... prayers)
    {
        instance.setHealthHandler(eatAtHp);
        instance.setPrayers(prayers);
        walkTo(target, true);
    }

    public static void walkTo(WorldPoint target)
    {
        walkTo(target, true);
    }

    public static void walkTo(WorldPoint target, boolean useTeleports)
    {
        instance.walk(target, useTeleports);
    }

    public static void walkTo(List<Step> steps, Teleport teleport)
    {
        instance.walk(steps, teleport);
    }

    public void walk(WorldPoint target, boolean useTeleports)
    {
        if(running)
        {
            return;
        }
        running = true;

        reset();
        final Pathfinder engine = new Pathfinder(target);

        List<Step> steps = engine.find();
        if(useTeleports && engine.getTeleport() != null)
            teleport = engine.getTeleport();
        walkTo(steps);
        running = false;
    }

    public void walk(List<Step> steps, Teleport teleport)
    {
        if(running)
        {
            return;
        }
        running = true;

        reset();
        if(teleport != null)
            this.teleport = teleport;
        walkTo(steps);
        running = false;
    }

    private void reset()
    {
        teleport = null;
        cooldown = 0;
        timeout = 0;
        recalcs = 0;
        timer = 0;
        multiSteps.clear();
        multiStepPointer = 0;

        if(healthHandler == 0)
        {
            healthHandler = (int) (Static.invoke(() -> client.getRealSkillLevel(Skill.HITPOINTS))* 0.8);
        }
    }

    /**
     * Traverses the supplied path.
     * NOTE: This CANNOT be ran on the client thread.
     * @param path path
     */
    private void walkTo(List<Step> path)
    {
        try
        {
            timer = client.getTickCount();
            Delays.tick();
            if(prayers != null)
            {
                PrayerAPI.setQuickPrayer(prayers);
            }
            handlePrayer();
            if(path == null || path.isEmpty())
            {
                return;
            }
            WorldPoint end = path.get(path.size() - 1).getPosition();
            while(traverse(path))
            {
                if(!running)
                {
                    prayers = null;
                    healthHandler = 0;
                    running = false;
                    timer = 0;
                    return;
                }
                Delays.tick();
            }
            int timeout = 50;
            WorldPoint worldPoint = Static.invoke(() -> client.getLocalPlayer().getWorldLocation());
            while(!worldPoint.equals(end) && timeout > 0)
            {
                handlePrayer();
                Delays.tick();
                if(PlayerAPI.isIdle(client.getLocalPlayer()))
                {
                    timeout--;
                    if(!Location.isReachable(client.getLocalPlayer().getWorldLocation(), end))
                    {
                        Pathfinder engine = new Pathfinder(end);
                        List<Step> path2 =  engine.find();
                        walkTo(path2);
                        return;
                    }
                    MovementAPI.walkToWorldPoint(end);
                    handlePrayer();
                    Delays.tick();
                }
                worldPoint = Static.invoke(() -> client.getLocalPlayer().getWorldLocation());
                if(!running)
                {
                    prayers = null;
                    healthHandler = 0;
                    running = false;
                    timer = 0;
                    return;
                }
            }
        }
        finally {
            prayers = null;
            healthHandler = 0;
            running = false;
            timer = 0;
            System.out.println("Pathfinder took: " + (client.getTickCount() - timer) + " ticks");
        }
    }

    private void cancel()
    {
        running = false;
    }

    /**
     * Traverse a step map from the pathfinder. Call this method from the
     * GameTick event until it returns false.
     * @param steps step map
     * @return true if there's more to process, false if it's done. Note,
     * it will call done after its sent the final walk, not once its
     * actually at the final tile.
     */
    public boolean traverse(List<Step> steps) {
        if(steps == null)
        {
            Logger.error("[Pathfinder] Steps are null");
            return false;
        }
        if (isCancelledOrTimedOut(steps)) {
            return false;
        }

        if(!client.getGameState().equals(GameState.LOGGED_IN))
        {
            return !steps.isEmpty();
        }

        if(handleTeleport())
        {
            return !steps.isEmpty();
        }

        if(handleMultiSteps())
        {
            return !steps.isEmpty();
        }

        if(applyCooldown())
        {
            return !steps.isEmpty();
        }

        if (shouldHandleDialogue(steps)) {
            handleDialogue();
            return true;
        }

        if (steps.isEmpty()) {
            return false;
        }

        Player local = client.getLocalPlayer();
        WorldPoint last = local.getWorldLocation();
        Tile tile = Location.toTile(local.getWorldLocation());
        Step step = steps.get(0);

        if (step.hasTransport()) {
            return handleTransport(steps, local, tile, step);
        }

        handlePrayer();

        return handleWalking(steps, local, last, step, step.getPosition());
    }

    private boolean isCancelledOrTimedOut(List<Step> steps) {
        if (Coroutine._isCancelled()) {
            steps.clear();
            return true;
        }
        if (timeout > 5) {
            if (recalcs < 6) {
                recalcs++;
                timeout = 0;
                if(steps.isEmpty())
                    return true;
                WorldPoint wp = steps.get(steps.size() - 1).getPosition();
                steps.clear();
                Pathfinder engine = new Pathfinder(wp);
                steps.addAll(engine.find());
                return !steps.isEmpty();
            }
            recalcs = 0;
            timeout = 0;
            Logger.consoleErrorOutput("Pathfinder", "Unable to pass tile");
            steps.clear();
            return true;
        }
        return false;
    }

    private boolean handleTeleport() {
        if (teleport != null) {
            multiSteps.addAll(teleport.getHandlers());
            multiStepPointer = 0;
            cooldown = 0;
            teleport = null;
            return true;
        }
        return false;
    }

    private boolean handleMultiSteps() {
        if (!multiSteps.isEmpty()) {
            if (!PlayerAPI.isIdle(client.getLocalPlayer()))
                return true;

            if (multiStepPointer >= multiSteps.size()) {
                multiSteps.clear();
                multiStepPointer = 0;
            } else {
                multiSteps.get(multiStepPointer).run();
                multiStepPointer++;
                return true;
            }
        }
        return false;
    }

    private boolean applyCooldown() {
        if (cooldown > 0) {
            cooldown--;
            return true;
        }
        return false;
    }

    private boolean shouldHandleDialogue(List<Step> steps) {
        return DialogueAPI.dialoguePresent() && !steps.isEmpty() && !Location.isReachable(client.getLocalPlayer().getWorldLocation(), steps.get(steps.size() - 1).getPosition());
    }

    private void handleDialogue() {
        if(!DialogueAPI.continueDialogue())
        {
            if(!DialogueAPI.selectOption("Yes") && !DialogueAPI.selectOption("Okay") && !DialogueAPI.selectOption("okay"))
            {
                DialogueAPI.selectOption(1);
            }
        }
    }

    private boolean handleTransport(List<Step> steps, Player local, Tile tile, Step step) {
        if (step.hasTransport()) {
            if (!PlayerAPI.isIdle(local)) {
                return true;
            }

            if (tile != null && Location.getDistance(tile, WorldPointUtil.fromCompressed(step.transport.getSource())) < 10) {
                Logger.info("[Pathfinder] Interacting with transport");

                step.transport.getHandler().get(0).run();
                if (step.transport.getHandler().size() > 1) {
                    multiSteps.addAll(step.transport.getHandler());
                    multiStepPointer = 1;
                }
                cooldown = Math.max(step.transport.getDuration(), 0);
                timeout = 0;
                steps.remove(step);
                return true;
            }

            if (cooldown > 0) {
                return true;
            }

            timeout++;
            return true;
        }
        return false;
    }

    private boolean handleWalking(List<Step> steps, Player local, WorldPoint last, Step step, WorldPoint dest) {
        manageRunEnergyAndHitpoints();

        if(!Location.isReachable(local.getWorldLocation(), step.getPosition()))
        {
            if(MovementAPI.isMoving())
            {
                return true;
            }
            if(handlePassThroughObjects(local, steps, step) || !PlayerAPI.isIdle(local))
            {
                return !steps.isEmpty();
            }
            WorldPoint wp = steps.get(steps.size() - 1).getPosition();
            steps.clear();
            Pathfinder engine = new Pathfinder(wp);
            steps.addAll(engine.find());
            return true;
        }

        if (last.distanceTo2D(dest) > 6 && !PlayerAPI.isIdle(local))
        {
            return true;
        }

        int s = 0;
        while(s <= 12 && s < steps.size() && !steps.get(s).hasTransport())
        {
            if(!Location.isReachable(local.getWorldLocation(), steps.get(s).getPosition()))
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
        if(steps.size() > 1 && steps.get(1).hasTransport())
        {
            steps.remove(0);
        }
        step = steps.get(0);
        MovementAPI.walkTowards(step.getPosition());
        if(!step.hasTransport())
            steps.remove(step);
        timeout = 0;
        return !steps.isEmpty();
    }

    private boolean handlePassThroughObjects(Player local, List<Step> steps, Step step)
    {
        TileObjectEx object = new TileObjectQuery<>()
                .withNamesContains("door", "gate")
                .keepIf(o -> (o.getWorldLocation().equals(local.getWorldLocation()) || o.getWorldLocation().equals(step.getPosition())))
                .sortNearest()
                .first();

        if(object != null)
        {
            TileObjectAPI.interact(object, 0);
            Logger.info("[Pathfinder] Interacting with '" + object.getName() + "'");
            return true;
        }
        else {
            Logger.info("[Pathfinder] Failed to find Passthrough, atempting to circumvent");
            if (steps.size() <= 6) {
                timeout++;
                return false;
            }
            Step next;
            for (int i = 1; i <= 6; i++) {
                next = steps.get(i);
                if (!Location.isReachable(client.getLocalPlayer().getWorldLocation(), next.getPosition())) {
                    MovementAPI.walkTowards(next.getPosition());
                    steps.subList(0, i).clear();
                    return true;
                }
            }
        }
        timeout++;
        return false;
    }

    private void manageRunEnergyAndHitpoints()
    {
        if(DialogueAPI.dialoguePresent())
        {
            return;
        }
        int energy = client.getEnergy();

        if(!MovementAPI.isRunEnabled() && energy > 30)
        {
            WidgetAPI.interact(0, WidgetInfo.MINIMAP_TOGGLE_RUN_ORB, -1, -1);
        }

        if(energy < 60 && !MovementAPI.staminaInEffect())
        {
            ItemEx stam = InventoryAPI.getItem(i -> ArrayUtils.contains(stamina, i.getId()));

            if(stam != null)
            {
                InventoryAPI.interact(stam, 1);
            }
        }

        if(energy <= 30)
        {
            ItemEx stam = InventoryAPI.getItem(i -> ArrayUtils.contains(stamina, i.getId()));

            if(stam != null)
            {
                InventoryAPI.interact(stam, 1);
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
}
