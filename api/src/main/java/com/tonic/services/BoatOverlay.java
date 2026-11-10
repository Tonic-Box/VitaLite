package com.tonic.services;

import com.tonic.Static;
import com.tonic.api.game.VarAPI;
import com.tonic.api.game.sailing.BoatStatsAPI;
import com.tonic.api.game.sailing.MoveMode;
import com.tonic.services.pathfinder.sailing.Sailing;
import com.tonic.services.pathfinder.sailing.sea.BoatPose;
import com.tonic.services.pathfinder.sailing.sea.BoatProfile;
import com.tonic.services.pathfinder.sailing.sea.Compass;
import com.tonic.services.pathfinder.sailing.sea.SeaRoute;
import com.tonic.ui.VitaOverlay;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * The boat info panel: where the boat is and how it moves, the voyage under way, the last plan and the boat's resistances
 */
public class BoatOverlay extends VitaOverlay
{
    public BoatOverlay()
    {
        super();
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setWidth(260);
        setHeight(330);
        setHidden(false);
    }

    public void show()
    {
        OverlayManager overlayManager = Static.getInjector().getInstance(OverlayManager.class);
        overlayManager.add(this);
        setHidden(false);
    }

    public void hide()
    {
        OverlayManager overlayManager = Static.getInjector().getInstance(OverlayManager.class);
        overlayManager.remove(this);
        setHidden(true);
    }

    public void update()
    {
        Sailing sailing = Sailing.get();
        BoatPose pose = sailing.pose();
        MoveMode mode = MoveMode.getCurrent();
        clear();
        newLine("# Boat", 14);
        if (pose != null)
        {
            WorldPoint tile = pose.tile();
            newLineEx("Tile: ", tile.getX() + ", " + tile.getY(), 12);
            newLineEx("Angle: ", pose.angle() + " (" + Compass.name(pose.heading()) + ")", 12);
        }
        newLineEx("Speed: ", tiles(sailing.speed()) + " tiles/tick", 12);
        newLineEx("Throttle: ", mode == null ? "-" : mode.name(), 12);
        newLineEx("At helm: ", yesNo(sailing.atHelm()), 12);
        newLineEx("Gust: ", VarAPI.getVar(VarbitID.SAILING_BOAT_SPEEDBOOST_DURATION) + " ticks", 12);
        BoatProfile profile = sailing.profile();
        newLineEx("Speeds: ", "full " + tiles(profile.fullSpeed()) + ", slow " + tiles(profile.slowSpeed()) + ", ramp " + tiles(profile.ramp()), 12);

        newLine("# Voyage", 14);
        newLineEx("State: ", sailing.state().name(), 12);
        WorldPoint goal = sailing.goal();
        newLineEx("Goal: ", goal == null ? "-" : goal.getX() + ", " + goal.getY(), 12);
        SeaRoute route = sailing.currentRoute();
        newLineEx("Route: ", route == null ? "-" : route.size() + " ticks", 12);
        List<Sailing.PlanReport> plans = sailing.recentPlans();
        if (!plans.isEmpty())
        {
            Sailing.PlanReport last = plans.get(plans.size() - 1);
            newLineEx("Last plan: ", last.stats().millis() + " ms, " + last.ticks() + " ticks, cost " + last.stats().cost(), 12);
        }
        String failure = sailing.failure();
        if (failure != null && !failure.isEmpty())
        {
            newLineEx("Failure: ", failure, 12, Color.CYAN, Color.RED);
        }

        newLine("# Resistances", 14);
        resistance("Rapid", BoatStatsAPI.getRapidResistance());
        resistance("Storm", BoatStatsAPI.getStormResistance());
        resistance("Fetid water", BoatStatsAPI.getFetidWaterResistance());
        resistance("Crystal", BoatStatsAPI.getCrystalFleckedResistance());
        resistance("Tangled kelp", BoatStatsAPI.getTangledKelpResistance());
        resistance("Ice", BoatStatsAPI.getIceResistance());
    }

    private static String yesNo(boolean value)
    {
        return value ? "Yes" : "No";
    }

    private static String tiles(int fine)
    {
        return BigDecimal.valueOf(fine, 0).divide(BigDecimal.valueOf(128), 7, RoundingMode.UNNECESSARY).stripTrailingZeros().toPlainString();
    }

    private void resistance(String name, int value)
    {
        Color color = value < 1 ? Color.RED : Color.GREEN;
        newLineEx(name + ": ", value < 1 ? "None" : String.valueOf(value), 12, Color.CYAN, color);
    }
}
