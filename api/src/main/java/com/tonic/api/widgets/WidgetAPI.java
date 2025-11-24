package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.data.WidgetInfoExtended;
import com.tonic.queries.WidgetQuery;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.ClickType;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

/**
 * Widget API
 */
public class WidgetAPI
{
    /**
     * Creates an instance of WidgetQuery
     * @return WidgetQuery
     */
    public static WidgetQuery search()
    {
        return new WidgetQuery();
    }

    /**
     * invoke a widget action by first matching action name
     * @param widget widget
     * @param actions action list
     */
    public static void interact(Widget widget, String... actions)
    {
        if (widget == null || widget.getActions() == null)
            return;
        for (String action : actions)
        {
            for(int i = 0; i < widget.getActions().length; i++)
            {
                String option = widget.getActions()[i];
                if(option != null && option.equalsIgnoreCase(action))
                {
                    interact(widget, i + 1);
                    return;
                }
            }
        }
    }

    /**
     * invoke a widget action by action index
     * @param widget widget
     * @param action action index
     */
    public static void interact(Widget widget, int action)
    {
        if (widget == null)
            return;
        WidgetAPI.interact(action, widget.getId(), widget.getIndex(), widget.getItemId());
    }

    /**
     * invoke a widget packet
     * @param action action type
     * @param widgetId packed widget ID
     * @param childId child ID
     * @param itemId item ID
     */
    public static void interact(int action, int widgetId, int childId, int itemId)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click(ClickType.WIDGET);
            client.getPacketWriter().widgetActionPacket(action, widgetId, childId, itemId);
        });
    }

    /**
     * invoke a widget packet
     * @param action action type
     * @param widgetId packed widget ID
     * @param childId child ID
     * @param itemId item ID
     */
    public static void interact(int action, int subOp, int widgetId, int childId, int itemId)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click(ClickType.WIDGET);
            client.getPacketWriter().widgetActionSubOpPacket(action, subOp, widgetId, childId, itemId);
        });
    }

    /**
     * invoke a widget sub op packet
     * @param widget runelite Widget - should come from Inventory
     * @param menu right click menu i.e "Rub"
     * @param action action insdie right click menu i.e "Grand Exchange"
     */
    public static void interact(Widget widget, String menu, String action)
    {
        Client client = Static.getClient();

        ItemComposition composition = client.getItemDefinition(widget.getItemId());

        String[] actions = widget.getActions();
        String[][] subOps = composition.getSubops();

        if (subOps == null)
        {
            return;
        }

        int menuIndex = -1;
        int actionIndex = -1;

        for (int i = 0; i < actions.length; i++)
        {
            String a = actions[i];

            if (a != null && a.equalsIgnoreCase(menu))
            {
                menuIndex = i + 1;
                break;
            }
        }

        for (String[] subOp : subOps)
        {
            if (actionIndex != -1)
                break;

            if (subOp == null)
                continue;

            for (int i = 0; i < subOp.length; i++)
            {
                String op = subOp[i];

                if (op != null && op.equalsIgnoreCase(action))
                {
                    actionIndex = i;
                    break;
                }
            }
        }

        if (menuIndex == -1 || actionIndex == -1)
        {
            return;
        }

        interact(menuIndex, actionIndex, widget.getId(), widget.getIndex(), widget.getItemId());
    }

    /**
     * invoke a widget action
     * @param action action type
     * @param widgetInfo widget info
     * @param childId child ID
     * @param itemId item ID
     */
    @SuppressWarnings("deprecation")
    public static void interact(int action, WidgetInfo widgetInfo, int childId, int itemId)
    {
        interact(action, widgetInfo.getId(), childId, itemId);
    }

    /**
     * invoke a widget action
     * @param action action type
     * @param widgetInfo widget info
     * @param childId child ID
     * @param itemId item ID
     */
    public static void interact(int action, WidgetInfoExtended widgetInfo, int childId, int itemId)
    {
        interact(action, widgetInfo.getId(), childId, itemId);
    }

    /**
     * invoke a widget action
     * @param action action type
     * @param widgetInfo widget info
     * @param childId child ID
     */
    public static void interact(int action, WidgetInfoExtended widgetInfo, int childId)
    {
        interact(action, widgetInfo.getId(), childId);
    }

    /**
     * invoke a widget action
     * @param action action type
     * @param widgetInfo widget info
     * @param childId child ID
     */
    @SuppressWarnings("deprecation")
    public static void interact(int action, WidgetInfo widgetInfo, int childId)
    {
        interact(action, widgetInfo.getId(), childId, -1);
    }

    /**
     * invoke a widget action
     * @param action action type
     * @param widgetId widget iid
     * @param childId child ID
     */
    public static void interact(int action, int widgetId, int childId)
    {
        interact(action, widgetId, childId, -1);
    }

    public static void dragWidget(Widget source, Widget dest)
    {
        if (source == null || dest == null) {
            return;
        }
        dragWidget(source.getId(), source.getItemId(), source.getIndex(), dest.getId(), dest.getItemId(), dest.getIndex());
    }

    public static void dragWidget(int widgetId, int itemId, int slot, int widgetId2, int itemId2, int slot2)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click(ClickType.WIDGET);
            client.getPacketWriter().widgetDragPacket(widgetId, itemId, slot, widgetId2, itemId2, slot2);
        });
    }

    /**
     * Use a widget on a game object
     *
     * @param selectedWidgetId selected widget ID
     * @param itemId item ID
     * @param slot slot
     * @param objectID object ID
     * @param worldX worldX
     * @param worldY worldY
     * @param ctrl if ctrl is held
     */
    public static void onTileObject(int selectedWidgetId, int itemId, int slot, int objectID, int worldX, int worldY, boolean ctrl)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click(ClickType.WIDGET);
            client.getPacketWriter().widgetTargetOnGameObjectPacket(selectedWidgetId, itemId, slot, objectID, worldX, worldY, ctrl);
        });
    }

    /**
     * Use a widget on a game object
     *
     * @param source widget
     * @param dest tile object
     * @param ctrl if ctrl is held
     * */
    public static void onTileObject(Widget source, TileObject dest, boolean ctrl)
    {
        if (source == null || dest == null) {
            return;
        }
        onTileObject(source.getId(), source.getItemId(), source.getIndex(), dest.getId(), dest.getWorldLocation().getX(), dest.getWorldLocation().getY(), ctrl);
    }

    /**
     * Use a widget on a game object without holding ctrl
     *
     * @param source widget
     * @param dest tile object
     * */
    public static void onTileObject(Widget source, TileObject dest)
    {
        onTileObject(source, dest, false);
    }

    /**
     * Use a widget on a ground item
     *
     * @param selectedWidgetId selected widget ID
     * @param itemId item ID
     * @param slot slot
     * @param groundItemID ground item ID
     * @param worldX worldX
     * @param worldY worldY
     * @param ctrl if ctrl is held
     */
    public static void onGroundItem(int selectedWidgetId, int itemId, int slot, int groundItemID, int worldX, int worldY, boolean ctrl)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click(ClickType.WIDGET);
            client.getPacketWriter().widgetOnGroundItemPacket(selectedWidgetId, itemId, slot, groundItemID, worldX, worldY, ctrl);
        });
    }

    /**
     * Use a widget on an NPC
     *
     * @param selectedWidgetId selected widget ID
     * @param itemId item ID
     * @param slot slot
     * @param npcIndex npc index
     * @param ctrl if ctrl is held
     */
    public static void onNpc(int selectedWidgetId, int itemId, int slot, int npcIndex, boolean ctrl)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click(ClickType.WIDGET);
            client.getPacketWriter().widgetTargetOnNpcPacket(npcIndex, selectedWidgetId, itemId, slot, ctrl);
        });
    }

    /**
     * Use a widget on an NPC
     *
     * @param source widget
     * @param dest npc
     * @param ctrl if ctrl is held
     */
    public static void onNpc(Widget source, NPC dest, boolean ctrl)
    {
        if (source == null || dest == null) {
            return;
        }
        onNpc(source.getId(), source.getItemId(), source.getIndex(), dest.getIndex(), ctrl);
    }

    /**
     * Use a widget on an NPC without holding ctrl
     *
     * @param source widget
     * @param dest npc
     */
    public static void onNpc(Widget source, NPC dest)
    {
        onNpc(source, dest, false);
    }

    /**
     * Use a widget on a player
     *
     * @param selectedWidgetId selected widget ID
     * @param itemId item ID
     * @param slot slot
     * @param playerIndex player index
     * @param ctrl if ctrl is held
     */
    public static void onPlayer(int selectedWidgetId, int itemId, int slot, int playerIndex, boolean ctrl)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click(ClickType.WIDGET);
            client.getPacketWriter().widgetTargetOnPlayerPacket(playerIndex, selectedWidgetId, itemId, slot, ctrl);
        });
    }

    /**
     * Use a widget on a player
     *
     * @param source widget
     * @param dest player
     * @param ctrl if ctrl is held
     */
    public static void onPlayer(Widget source, Player dest, boolean ctrl)
    {
        if (source == null || dest == null) {
            return;
        }
        onPlayer(source.getId(), source.getItemId(), source.getIndex(), dest.getId(), ctrl);
    }

    /**
     * Use a widget on a player without holding ctrl
     *
     * @param source widget
     * @param dest player
     */
    public static void onPlayer(Widget source, Player dest)
    {
        onPlayer(source, dest, false);
    }

    /**
     * Use a widget on another widget
     *
     * @param selectedWidgetId selected widget ID
     * @param itemId item ID
     * @param slot slot
     * @param targetWidgetId target widget ID
     * @param itemId2 target item ID
     * @param slot2 target slot
     */
    public static void onWidget(int selectedWidgetId, int itemId, int slot, int targetWidgetId, int itemId2, int slot2)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click(ClickType.WIDGET);
            client.getPacketWriter().widgetOnWidgetPacket(selectedWidgetId, itemId, slot, targetWidgetId, itemId2, slot2);
        });
    }

    /**
     * Uses a widget on another widget
     * @param source source widget
     * @param dest destination widget
     */
    public static void onWidget(Widget source, Widget dest)
    {
        if (source == null || dest == null) {
            return;
        }
        onWidget(source.getId(), source.getItemId(), source.getIndex(), dest.getId(), dest.getItemId(), dest.getIndex());
    }

    /**
     * Get the text of a widget
     * @param widgetInfo widget info
     * @return text
     */
    @SuppressWarnings("deprecation")
    public static String getText(WidgetInfo widgetInfo)
    {
        return getText(widgetInfo.getGroupId(), widgetInfo.getChildId());
    }

    /**
     * Get the text of a widget
     * @param groupId groupId
     * @param childId childId
     * @return text
     */
    public static String getText(int groupId, int childId)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            Widget widget = client.getWidget(groupId, childId);
            if(widget == null || widget.getText() == null)
                return "";

            return widget.getText();
        });
    }

    /**
     * Get the text of a widget
     * @return text
     */
    public static String getText(int widgetId)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            Widget widget = client.getWidget(widgetId);
            if(widget == null || widget.getText() == null)
                return "";

            return widget.getText();
        });
    }

    /**
     * Get the text of a widget
     * @return text
     */
    public static String getText(Widget widget)
    {
        return Static.invoke(() -> {
            if(widget == null || widget.getText() == null)
                return "";

            return widget.getText();
        });
    }

    /**
     * Get a widget by WidgetInfo
     * @param info widget info
     * @return widget
     */
    @SuppressWarnings("deprecation")
    public static Widget get(WidgetInfo info)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getWidget(info));
    }

    /**
     * Get a widget by WidgetInfoExtended
     * @param info widget info
     * @return widget
     */
    @SuppressWarnings("deprecation")
    public static Widget get(WidgetInfoExtended info)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getWidget(info.getGroupId(), info.getChildId()));
    }

    /**
     * Get a widget by groupId and childId
     * @param groupId groupId
     * @param childId childId
     * @return widget
     */
    public static Widget get(int groupId, int childId)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getWidget(groupId, childId));
    }

    /**
     * Get a widget by interfaceId
     * @param interfaceId interfaceId
     * @return widget
     */
    public static Widget get(int interfaceId)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getWidget(interfaceId));
    }

    /**
     * Get a child widget by groupId, childId and child index
     * @param groupId groupId
     * @param childId childId
     * @param child child index
     * @return widget
     */
    public static Widget get(int groupId, int childId, int child)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            Widget parent = client.getWidget(groupId, childId);
            if(parent == null || parent.getChildren() == null)
                return null;
            return parent.getChild(child);
        });
    }

    /**
     * Check if a widget is visible
     * @param widget widget
     * @return true if visible
     */
    public static boolean isVisible(Widget widget)
    {
        if(Static.isHeadless())
        {
            return Static.invoke(() -> widget != null && !widget.isSelfHidden());
        }
        return Static.invoke(() -> widget != null && !widget.isHidden() && !widget.isSelfHidden());
    }

    /**
     * Check if a widget is visible
     * @param groupId groupId
     * @param childId childId
     * @return true if visible
     */
    public static boolean isVisible(int groupId, int childId)
    {
        return isVisible(get(groupId, childId));
    }

    /**
     * Check if a widget is visible
     * @param interfaceId interfaceId
     * @return true if visible
     */
    public static boolean isVisible(int interfaceId)
    {
        return isVisible(get(interfaceId));
    }
}
