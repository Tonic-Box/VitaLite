package com.tonic.services;

import com.tonic.Static;
import com.tonic.api.game.sailing.BoatStatsAPI;
import com.tonic.api.game.sailing.Heading;
import com.tonic.api.game.sailing.SailingAPI;
import com.tonic.ui.VitaOverlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;

import java.awt.*;

public class BoatOverlay extends VitaOverlay
{
    public BoatOverlay()
    {
        super();
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setWidth(250);
        setHeight(215);
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
        Heading heading = SailingAPI.getHeading();
        Heading targetHeading = SailingAPI.getTargetHeading();
        Heading resolvedHeading = SailingAPI.getResolvedHeading();

        String headingName = heading == null ? "Null" : heading.name();
        String targetHeadingName = targetHeading == null ? "Null" : targetHeading.name();
        String resolvedHeadingName = resolvedHeading == null ? "Null" : resolvedHeading.name();

        clear();
        newLine("# Sailing Boat Info", 14);
        newLineEx("Heading: ", headingName, 12);
        newLineEx("Target Heading: ", targetHeadingName, 12);
        newLineEx("Resolved Heading: ", resolvedHeadingName, 12);
        newLineEx("Sails Need Trimming: ", SailingAPI.sailsNeedTrimming() ? "Yes" : "No", 12);
        newLineEx("Is Navigating: ", SailingAPI.isNavigating() ? "Yes" : "No", 12);
        newLineEx("Movement: ", SailingAPI.isMovingForward() ? "Forward" : (SailingAPI.isMovingBackward() ? "Backward" : "Still"), 12);
        newLineEx("Speed: ", SailingAPI.getSpeed() + "", 12);
        newLine("# Sailing Boat Stats", 14);
        buildStatsLine("Rapid", BoatStatsAPI.getRapidResistance());
        buildStatsLine("Storm", BoatStatsAPI.getStormResistance());
        buildStatsLine("Fetid water", BoatStatsAPI.getFetidWaterResistance());
        buildStatsLine("Crystal", BoatStatsAPI.getCrystalFleckedResistance());
        buildStatsLine("Tangled kelp", BoatStatsAPI.getTangledKelpResistance());
        buildStatsLine("Ice", BoatStatsAPI.getIceResistance());
    }

    private void buildStatsLine(String name, int value)
    {
        Color color = value < 1 ? Color.RED : Color.GREEN;
        String valueStr = value < 1 ? "None" : String.valueOf(value);
        newLineEx(name + " resistance: ", valueStr, 12, Color.CYAN, color);
    }
}
