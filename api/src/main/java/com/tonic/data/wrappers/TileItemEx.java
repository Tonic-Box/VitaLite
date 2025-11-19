package com.tonic.data.wrappers;

import com.tonic.Static;
import com.tonic.api.TItemComposition;
import com.tonic.api.entities.TileItemAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.data.wrappers.abstractions.Entity;
import com.tonic.services.GameManager;
import com.tonic.util.Distance;
import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;

import java.awt.*;

@RequiredArgsConstructor
@Getter
public class TileItemEx implements Entity
{
    private final TileItem item;
    private final WorldPoint worldPoint;
    private final LocalPoint localPoint;
    private String[] actions = null;

    public TileItemEx(TileItem item, WorldPoint worldPoint) {
        this.item = item;
        this.worldPoint = worldPoint;
        Client client = Static.getClient();
        this.localPoint = LocalPoint.fromWorld(client, worldPoint);
    }

    @Override
    public WorldPoint getInteractionPoint()
    {
        if(GameManager.isReachable(WorldPointUtil.compress(worldPoint)))
        {
            return worldPoint;
        }

        TileObjectEx object = TileObjectAPI.search()
                .keepIf(to -> to.getTileObject() instanceof GameObject)
                .keepIf(to -> to.getWorldArea().contains(worldPoint))
                .keepIf(to -> to.getName() != null)
                .first();

        if(object != null)
        {
            return object.getInteractionPoint();
        }

        return worldPoint;
    }

    @Override
    public int getId() {
        return item.getId();
    }

    public boolean isNoted() {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getNote()) == 799;
    }

    public int getCanonicalId() {
        ItemManager itemManager = Static.getInjector().getInstance(ItemManager.class);
        return itemManager.canonicalize(item.getId());
    }

    @Override
    public String getName() {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getName());
    }

    public int getQuantity() {
        return item.getQuantity();
    }

    @Override
    public void interact(String... actions) {
        TileItemAPI.interact(this, actions);
    }

    @Override
    public void interact(int action) {
        TileItemAPI.interact(this, action);
    }

    public String[] getActions()
    {
        if(actions != null)
            return actions;
        if(item == null)
            return new String[0];
        actions = Static.invoke(() -> {
            Client client = Static.getClient();
            TItemComposition itemComp = (TItemComposition) client.getItemDefinition(item.getId());
            return itemComp.getGroundActions();
        });

        return actions;
    }

    public int getShopPrice() {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getPrice());
    }

    public long getGePrice()
    {
        return Static.invoke(() -> {
            ItemManager itemManager = Static.getInjector().getInstance(ItemManager.class);
            int id = itemManager.canonicalize(item.getId());
            if (id == ItemID.COINS)
            {
                return (long) getQuantity();
            }
            else if (id == ItemID.PLATINUM)
            {
                return getQuantity() * 1000L;
            }

            ItemComposition itemDef = itemManager.getItemComposition(id);
            if (itemDef.getPrice() <= 0)
            {
                return 0L;
            }

            return (long) itemManager.getItemPrice(id);
        });
    }

    public int getHighAlchValue()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getHaPrice());
    }

    public int getLowAlchValue()
    {
        return (int) Math.floor(getHighAlchValue() * 0.6);
    }

    @Override
    public WorldView getWorldView() {
        Client client = Static.getClient();
        return Static.invoke(client::getTopLevelWorldView);
    }

    @Override
    public int getWorldViewId()
    {
        WorldView worldView = getWorldView();
        if(worldView == null)
            return -1;
        return worldView.getId();
    }

    @Override
    public WorldArea getWorldArea() {
        return worldPoint.toWorldArea();
    }

    @Override
    public Tile getTile() {
        return SceneAPI.getTile(worldPoint);
    }

    @Override
    public Shape getShape() {
        Client client = Static.getClient();
        return Perspective.getCanvasTilePoly(client, getLocalPoint());
    }
}
