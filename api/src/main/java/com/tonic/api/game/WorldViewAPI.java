package com.tonic.api.game;

import com.tonic.Static;
import com.tonic.api.game.sailing.SailingAPI;
import com.tonic.data.wrappers.ActorEx;
import com.tonic.data.wrappers.PlayerEx;
import net.runelite.api.Client;
import net.runelite.api.Projection;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

public class WorldViewAPI
{
    public static WorldPoint getTolWorldLocation()
    {
        return getTopWorldLocation(PlayerEx.getLocal());
    }
    public static WorldPoint getTopWorldLocation(ActorEx<?> actor) {
        if (actor == null)
            return null;

        WorldPoint rawWp = actor.getWorldPoint();
        if (!SailingAPI.isOnBoat())
            return rawWp;

        WorldView playerWorldView = actor.getWorldView();
        if (playerWorldView.getId() == -1)
            return rawWp;

        LocalPoint playerLocalPoint = actor.getLocalPoint();
        if (playerLocalPoint == null)
            return rawWp;
        Projection mainWorldProj = playerWorldView.getMainWorldProjection();
        if (mainWorldProj == null)
            return rawWp;

        float[] projectedToMainWorld = mainWorldProj.project(playerLocalPoint.getX(), 0, playerLocalPoint.getY());
        Client client = Static.getClient();
        WorldView topLevelWorldView = client.getTopLevelWorldView();

        float xWithDecimals = (projectedToMainWorld[0] / 128f) + topLevelWorldView.getBaseX();
        float yWithDecimals = (projectedToMainWorld[2] / 128f) + topLevelWorldView.getBaseY();
        return new WorldPoint(Math.round(xWithDecimals), Math.round(yWithDecimals), topLevelWorldView.getPlane());
    }

    public static WorldPoint getTopWorldLocation(WorldPoint worldPoint) {
        if (worldPoint == null)
            return null;

        if (!SailingAPI.isOnBoat())
            return worldPoint;

        WorldView wv = PlayerEx.getLocal().getWorldView();
        if (wv.getId() == -1)
            return worldPoint;

        LocalPoint lp = LocalPoint.fromWorld(wv, worldPoint);
        if (lp == null)
            return worldPoint;
        Projection mainWorldProj = wv.getMainWorldProjection();
        if (mainWorldProj == null)
            return worldPoint;

        float[] projectedToMainWorld = mainWorldProj.project(lp.getX(), 0, lp.getY());
        Client client = Static.getClient();
        WorldView topLevelWorldView = client.getTopLevelWorldView();

        float xWithDecimals = (projectedToMainWorld[0] / 128f) + topLevelWorldView.getBaseX();
        float yWithDecimals = (projectedToMainWorld[2] / 128f) + topLevelWorldView.getBaseY();
        return new WorldPoint(Math.round(xWithDecimals), Math.round(yWithDecimals), topLevelWorldView.getPlane());
    }
}
