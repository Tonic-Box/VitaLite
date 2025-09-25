package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.data.TileObjectEx;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.PacketInteractionType;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

import java.util.function.Predicate;

/**
 * TileObject API
 */
public class TileObjectAPI
{
    /**
     * interact with a tile object
     * @param object object
     * @param action action
     */
    public static void interact(TileObjectEx object, int action)
    {
        Client client = Static.getClient();
        TClient tclient = Static.getClient();
        if(!client.getGameState().equals(GameState.LOGGED_IN) || object == null)
            return;

        Static.invoke(() ->
        {
            ClickManager.click(PacketInteractionType.TILEOBJECT_INTERACT);
            tclient.getPacketWriter().objectActionPacket(action, object.getId(), object.getWorldLocation().getX(), object.getWorldLocation().getY(), false);
        });
    }

    /**
     * interact with a tile object
     * @param object object
     * @param action action
     */
    public static void interact(TileObject object, int action)
    {
        Client client = Static.getClient();
        TClient tclient = Static.getClient();
        if(!client.getGameState().equals(GameState.LOGGED_IN) || object == null)
            return;

        Static.invoke(() ->
        {
            ClickManager.click(PacketInteractionType.TILEOBJECT_INTERACT);
            tclient.getPacketWriter().objectActionPacket(action, object.getId(), object.getWorldLocation().getX(), object.getWorldLocation().getY(), false);
        });
    }

    /**
     * interact with a tile object
     * @param object object
     * @param action action
     */
    public static void interact(TileObjectEx object, String action)
    {
        Client client = Static.getClient();
        TClient tclient = Static.getClient();
        if(!client.getGameState().equals(GameState.LOGGED_IN) || object == null)
            return;

        int actionIndex = object.getActionIndex(action);

        final WorldPoint wp = object.getWorldLocation();
        Static.invoke(() ->
        {
            ClickManager.click(PacketInteractionType.TILEOBJECT_INTERACT);
            tclient.getPacketWriter().objectActionPacket(actionIndex, object.getId(), wp.getX(), wp.getY(), false);
        });
    }

    /**
     * interact with a tile object
     * @param object object
     * @param action action
     */
    public static void interact(TileObject object, String action)
    {
        Client client = Static.getClient();
        TClient tclient = Static.getClient();
        if(!client.getGameState().equals(GameState.LOGGED_IN) || object == null)
            return;

        int actionIndex = getAction(object, action);

        Static.invoke(() ->
        {
            ClickManager.click(PacketInteractionType.TILEOBJECT_INTERACT);
            tclient.getPacketWriter().objectActionPacket(actionIndex, object.getId(), object.getWorldLocation().getX(), object.getWorldLocation().getY(), false);
        });
    }

    /**
     * get the actions of a tile object
     * @param tileObject tile object
     * @return actions
     */
    public static String[] getActions(TileObject tileObject) {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            ObjectComposition composition = client.getObjectDefinition(tileObject.getId());
            if(composition == null)
                return new String[]{};
            return composition.getActions();
        });
    }

    private static int getAction(TileObject object, String action) {
        String[] actions = getActions(object);
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

    public static TileObjectEx get(Predicate<TileObjectEx> filter)
    {
        return Static.invoke(() -> new TileObjectQuery<>().keepIf(filter).sortNearest().first());
    }

    public static TileObjectEx get(String... names)
    {
        return Static.invoke(() -> new TileObjectQuery<>().withNames(names).sortNearest().first());
    }

    public static TileObjectEx getContains(String... names)
    {
        return Static.invoke(() -> new TileObjectQuery<>().withNamesContains(names).sortNearest().first());
    }

    public static TileObjectEx get(int... ids)
    {
        return Static.invoke(() -> new TileObjectQuery<>().withId(ids).sortNearest().first());
    }
}
