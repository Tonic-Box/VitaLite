package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.ClickType;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

import java.util.function.Predicate;

/**
 * TileObject API
 */
public class TileObjectAPI
{
    /**
     * Creates an instance of TileObjectQuery
     * @return TileObjectQuery
     */
    public static TileObjectQuery search()
    {
        return new TileObjectQuery();
    }

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
            ClickManager.click(ClickType.OBJECT);
            tclient.getPacketWriter().objectActionPacket(action, object.getId(), object.getWorldPoint().getX(), object.getWorldPoint().getY(), false);
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
            ClickManager.click(ClickType.OBJECT);
            tclient.getPacketWriter().objectActionPacket(action, object.getId(), object.getWorldLocation().getX(), object.getWorldLocation().getY(), false);
        });
    }

    /**
     * interact with a tile object with first matching action
     * @param object object
     * @param actions actions list
     */
    public static void interact(TileObjectEx object, String... actions)
    {
        Client client = Static.getClient();
        TClient tclient = Static.getClient();
        if(!client.getGameState().equals(GameState.LOGGED_IN) || object == null)
            return;

        for (String action : actions)
        {
            int actionIndex = object.getActionIndex(action);

            if (actionIndex == -1)
                continue;

            final WorldPoint wp = object.getWorldPoint();
            Static.invoke(() ->
            {
                ClickManager.click(ClickType.OBJECT);
                tclient.getPacketWriter().objectActionPacket(actionIndex, object.getId(), wp.getX(), wp.getY(), false);
            });
            return;
        }
    }

    /**
     * interact with a tile object by first matching action
     * @param object object
     * @param actions action list
     */
    public static void interact(TileObject object, String... actions)
    {
        Client client = Static.getClient();
        TClient tclient = Static.getClient();
        if(!client.getGameState().equals(GameState.LOGGED_IN) || object == null)
            return;

        for (String action : actions)
        {
            int actionIndex = getAction(object, action);

            if (actionIndex == -1)
                continue;

            Static.invoke(() ->
            {
                ClickManager.click(ClickType.OBJECT);
                tclient.getPacketWriter().objectActionPacket(actionIndex, object.getId(), object.getWorldLocation().getX(), object.getWorldLocation().getY(), false);
            });
        }
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

            if (composition.getImpostorIds() == null)
            {
                return composition.getActions();
            }

            composition = composition.getImpostor();
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

    /**
     * get a tile object by filter
     * @param filter filter
     * @return tile object
     */
    public static TileObjectEx get(Predicate<TileObjectEx> filter)
    {
        return Static.invoke(() -> search().keepIf(filter).sortNearest().first());
    }

    /**
     * get a tile object by name
     * @param names names
     * @return tile object
     */
    public static TileObjectEx get(String... names)
    {
        return Static.invoke(() -> search().withNames(names).sortNearest().first());
    }

    /**
     * get a tile object by name contains
     * @param names names
     * @return tile object
     */
    public static TileObjectEx getContains(String... names)
    {
        return Static.invoke(() -> search().withNamesContains(names).sortNearest().first());
    }

    /**
     * get a tile object by id
     * @param ids ids
     * @return tile object
     */
    public static TileObjectEx get(int... ids)
    {
        return Static.invoke(() -> search().withId(ids).sortNearest().first());
    }
}
