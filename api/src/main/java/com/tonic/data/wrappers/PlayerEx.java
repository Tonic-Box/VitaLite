package com.tonic.data.wrappers;

import com.tonic.Static;
import com.tonic.api.entities.PlayerAPI;
import com.tonic.util.Location;
import com.tonic.util.TextUtil;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

public class PlayerEx extends ActorEx<Player>
{
    private static PlayerEx localPlayerEx;

    public static PlayerEx getLocal()
    {
        Client client = Static.getClient();
        if (localPlayerEx == null || !client.getLocalPlayer().equals(localPlayerEx.getPlayer()))
        {
            localPlayerEx = new PlayerEx(client.getLocalPlayer());
        }
        return localPlayerEx;
    }

    private PlayerComposition composition;
    public PlayerEx(Player player)
    {
        super(player);
    }

    public Player getPlayer()
    {
        return actor;
    }

    public PlayerComposition getComposition()
    {
        if (composition == null)
        {
            composition = Static.invoke(actor::getPlayerComposition);
        }
        return composition;
    }

    public void transformToNpc(int npcId) {
        Static.invoke(() -> getComposition().setTransformedNpcId(npcId));
    }

    @Override
    public WorldPoint getWorldPoint() {
        return actor.getWorldLocation();
    }

    @Override
    public WorldArea getWorldArea() {
        return actor.getWorldArea();
    }

    @Override
    public LocalPoint getLocalPoint() {
        return actor.getLocalLocation();
    }

    @Override
    public Tile getTile() {
        return Location.toTile(getWorldPoint());
    }

    @Override
    public void interact(String... actions) {
        PlayerAPI.interact(this, actions);
    }

    @Override
    public void interact(int action) {
        PlayerAPI.interact(this, action);
    }

    @Override
    public String[] getActions() {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            String[] actions = client.getPlayerOptions();
            String[] cleaned = new String[actions.length];
            for(int i = 0; i < actions.length; i++) {
                if(actions[i] != null) {
                    cleaned[i] = TextUtil.sanitize(actions[i]);
                } else {
                    cleaned[i] = null;
                }
            }
            return cleaned;
        });
    }

    @Override
    public int getId() {
        return getIndex();
    }
}
