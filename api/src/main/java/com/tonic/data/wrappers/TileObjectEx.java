package com.tonic.data.wrappers;

import com.tonic.Static;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.data.wrappers.abstractions.Entity;
import com.tonic.util.Location;
import com.tonic.util.TextUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;

@RequiredArgsConstructor
@Getter
public class TileObjectEx implements Entity
{
    public static TileObjectEx of(TileObject object)
    {
        if(object == null)
            return null;
        return new TileObjectEx(object);
    }

    private final TileObject tileObject;
    private String[] actions;

    @Override
    public int getId() {
        return tileObject.getId();
    }

    @Override
    public String getName() {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            ObjectComposition composition = client.getObjectDefinition(tileObject.getId());
            if(composition.getImpostorIds() != null)
            {
                composition = composition.getImpostor();
            }
            if(composition == null)
                return null;
            return TextUtil.sanitize(composition.getName());
        });
    }

    public boolean hasAction(String action) {
        return getActionIndex(action) != -1;
    }

    @Override
    public void interact(String... actions) {
        TileObjectAPI.interact(this, actions);
    }

    @Override
    public void interact(int action) {
        TileObjectAPI.interact(this, action);
    }

    @Override
    public String[] getActions() {
        if(actions == null)
        {
            Client client = Static.getClient();
            actions = Static.invoke(() -> {
                ObjectComposition composition = client.getObjectDefinition(tileObject.getId());
                if(composition.getImpostorIds() != null)
                {
                    composition = composition.getImpostor();
                }
                if(composition == null)
                    return new String[]{};
                return composition.getActions();
            });
        }
        return actions;
    }

    public int getActionIndex(String action) {
        String[] actions = getActions();
        for(int i = 0; i < actions.length; i++)
        {
            if(actions[i] == null)
                continue;
            if(!actions[i].toLowerCase().contains(action.toLowerCase()))
                continue;
            return i;
        }
        return -1;
    }

    @Override
    public WorldPoint getWorldPoint() {
        WorldPoint wp = tileObject.getWorldLocation();
        if(tileObject instanceof GameObject)
        {
            final Client client = Static.getClient();
            WorldView wv = client.getTopLevelWorldView();
            GameObject go = (GameObject) tileObject;
            Point p = go.getSceneMinLocation();
            wp = WorldPoint.fromScene(wv, p.getX(), p.getY(), wv.getPlane());
        }
        return wp;
    }

    @Override
    public WorldArea getWorldArea()
    {
        int width = 1;
        int height = 1;
        if(tileObject instanceof GameObject) {
            GameObject go = (GameObject) tileObject;
            Point min = go.getSceneMinLocation();
            Point max = go.getSceneMaxLocation();
            width = max.getX() - min.getX() + 1;
            height = max.getY() - min.getY() + 1;
        }
        return new WorldArea(getWorldPoint(), width, height);
    }

    @Override
    public LocalPoint getLocalPoint() {
        return tileObject.getLocalLocation();
    }

    @Override
    public Tile getTile()
    {
        return Location.toTile(getWorldPoint());
    }

    public Shape getShape()
    {
        if(tileObject instanceof GameObject) {
            GameObject go = (GameObject) tileObject;
            return go.getConvexHull();
        }
        else if(tileObject instanceof WallObject) {
            WallObject wo = (WallObject) tileObject;
            return wo.getConvexHull();
        }
        else if(tileObject instanceof DecorativeObject) {
            DecorativeObject deco = (DecorativeObject) tileObject;
            return deco.getConvexHull();
        }
        else if(tileObject instanceof GroundObject) {
            GroundObject ground = (GroundObject) tileObject;
            return ground.getConvexHull();
        }
        return tileObject.getClickbox();
    }
}
