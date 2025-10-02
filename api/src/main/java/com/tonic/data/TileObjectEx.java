package com.tonic.data;

import com.tonic.Static;
import com.tonic.util.TextUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@RequiredArgsConstructor
@Getter
public class TileObjectEx
{
    public static TileObjectEx of(TileObject object)
    {
        if(object == null)
            return null;
        return new TileObjectEx(object);
    }

    private final TileObject tileObject;
    private String[] actions;

    public int getId() {
        return tileObject.getId();
    }

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

    public WorldPoint getWorldLocation() {
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

    public LocalPoint getLocalLocation() {
        return tileObject.getLocalLocation();
    }
}
