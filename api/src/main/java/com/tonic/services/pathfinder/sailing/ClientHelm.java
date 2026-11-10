package com.tonic.services.pathfinder.sailing;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.GameAPI;
import com.tonic.api.game.sailing.SailingTab;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.SailingConstants;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;

/**
 * The helm of the live client, each action a menu action or interaction on the client thread
 */
final class ClientHelm implements Helm
{
    private static final String HELM = "Helm";
    private static final String NAVIGATE = "Navigate";
    private static final String STOP_NAVIGATING = "Stop-navigating";
    private static final String TRIM = "Trim";
    private static final String CARGO_HOLD = "cargo hold";
    private static final String DEPOSIT_HELD = "Deposit-held";
    private static final int FIRST_OPTION = 0;
    private static final int CLICK = 1;

    private final ClientSeaWorld world;

    ClientHelm(ClientSeaWorld world)
    {
        this.world = world;
    }

    @Override
    public void setHeading(int heading)
    {
        GameAPI.invokeMenuAction(heading, MenuAction.SET_HEADING.getId(), 0, 0, 0, PlayerEx.getLocal().getWorldViewId());
    }

    @Override
    public void throttle(int child)
    {
        if (!SailingTab.FACILITIES.isOpen())
        {
            SailingTab.FACILITIES.open();
        }
        WidgetAPI.interact(CLICK, InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CLICKLAYER, child);
    }

    @Override
    public boolean takeHelm()
    {
        TileObjectEx helm = TileObjectAPI.search().withName(HELM).withAction(NAVIGATE).nearest();
        if (helm == null)
        {
            return false;
        }
        TileObjectAPI.interact(helm, NAVIGATE);
        return true;
    }

    @Override
    public boolean leaveHelm()
    {
        TileObjectEx helm = TileObjectAPI.search().withName(HELM).withAction(STOP_NAVIGATING).nearest();
        if (helm == null)
        {
            return false;
        }
        TileObjectAPI.interact(helm, STOP_NAVIGATING);
        return true;
    }

    @Override
    public boolean stowCargo()
    {
        TileObjectEx hold = TileObjectAPI.search().withNameContains(CARGO_HOLD).withAction(DEPOSIT_HELD).nearest();
        if (hold == null)
        {
            return false;
        }
        TileObjectAPI.interact(hold, DEPOSIT_HELD);
        return true;
    }

    @Override
    public boolean trimSails()
    {
        TileObjectEx sails = trimmable();
        if (sails == null)
        {
            return false;
        }
        TileObjectAPI.interact(sails, TRIM);
        return true;
    }

    @Override
    public boolean canTrim()
    {
        return trimmable() != null;
    }

    @Override
    public boolean dock(Port port)
    {
        return clickGangplank(port);
    }

    @Override
    public void walkToGangplank(Port port)
    {
        world.walk(port.land());
    }

    @Override
    public boolean board(Port port)
    {
        return clickGangplank(port);
    }

    @Override
    public void walkTo(WorldPoint tile)
    {
        world.walk(tile);
    }

    @Override
    public void stopWalking()
    {
        world.cancelWalk();
    }

    @Override
    public boolean recoverBoat(int shipwright)
    {
        NpcEx npc = NpcAPI.search().withIds(shipwright).nearest();
        if (npc == null)
        {
            return false;
        }
        NpcAPI.interact(npc, Shipwrights.RECOVER_BOAT - 1);
        return true;
    }

    @Override
    public boolean chooseSlot(int slot)
    {
        if (!WidgetAPI.isVisible(InterfaceID.SailingBoatSelection.BOATS_CLICK_LAYER))
        {
            return false;
        }
        DialogueAPI.resumePause(InterfaceID.SailingBoatSelection.BOATS_CLICK_LAYER, slot);
        return true;
    }

    @Override
    public boolean continueDialogue()
    {
        return DialogueAPI.continueDialogue();
    }

    private static TileObjectEx trimmable()
    {
        return TileObjectAPI.search().withId(SailingConstants.SAILS).withActionVisible(TRIM).nearest();
    }

    private static boolean clickGangplank(Port port)
    {
        if (!port.placed())
        {
            return false;
        }
        TileObjectEx plank = TileObjectAPI.search().withId(port.gangplankLocId()).nearest(port.land());
        if (plank == null)
        {
            return false;
        }
        TileObjectAPI.interact(plank, FIRST_OPTION);
        return true;
    }
}
