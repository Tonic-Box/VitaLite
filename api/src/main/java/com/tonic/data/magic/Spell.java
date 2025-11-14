package com.tonic.data.magic;

import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.*;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;

public interface Spell {
    int getAction();
    int getLevel();
    int getWidget();
    int getAutoCastIndex();
    boolean canCast();
    RuneRequirement[] getRequirements();

    default void cast()
    {
        WidgetAPI.interact(getAction(), getWidget(), -1, -1);
    }

    default void castOn(ItemEx item)
    {
        WidgetAPI.onWidget(getWidget(), -1, -1, InterfaceID.Inventory.ITEMS, item.getId(), item.getSlot());
    }

    default void castOn(NpcEx npc)
    {
        WidgetAPI.onNpc(getWidget(), -1, -1, npc.getIndex(), false);
    }

    default void castOn(PlayerEx player)
    {
        WidgetAPI.onPlayer(getWidget(), -1, -1, player.getId(), false);
    }

    default void castOn(TileObjectEx tileObject)
    {
        WorldPoint loc = tileObject.getWorldPoint();
        WidgetAPI.onTileObject(getWidget(), -1, -1, tileObject.getId(), loc.getX(), loc.getY(), false);
    }

    default void castOn(TileItemEx tileItem)
    {
        WorldPoint loc = tileItem.getWorldPoint();
        WidgetAPI.onGroundItem(getWidget(), -1, -1, tileItem.getId(), loc.getX(), loc.getY(), false);
    }

    default void setAutoCast()
    {
        WidgetAPI.interact(1, InterfaceID.CombatInterface.AUTOCAST_NORMAL, -1, -1);
        WidgetAPI.interact(1, InterfaceID.Autocast.SPELLS, getAutoCastIndex(), -1);
    }

    default void setDefensiveAutoCast()
    {
        WidgetAPI.interact(1, InterfaceID.CombatInterface.AUTOCAST_DEFENSIVE, -1, -1);
        WidgetAPI.interact(1, InterfaceID.Autocast.SPELLS, getAutoCastIndex(), -1);
    }
}