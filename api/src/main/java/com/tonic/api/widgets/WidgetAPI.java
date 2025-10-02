package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.data.WidgetInfoExtended;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.PacketInteractionType;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

/**
 * Widget API
 */
public class WidgetAPI
{
    /**
     * invoke a widget action by action name
     * @param widget widget
     * @param action action
     */
    public static void interact(Widget widget, String action) {
        if (widget == null || widget.getActions() == null)
            return;
        for(int i = 0; i < widget.getActions().length; i++)
        {
            String option = widget.getActions()[i];
            if(option != null && option.toLowerCase().contains(action.toLowerCase()))
            {
                interact(widget, i);
                return;
            }
        }
    }

    /**
     * invoke a widget action by action index
     * @param widget widget
     * @param action action index
     */
    public static void interact(Widget widget, int action) {
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
            ClickManager.click(PacketInteractionType.WIDGET_INTERACT);
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
            ClickManager.click(PacketInteractionType.WIDGET_INTERACT);
            client.getPacketWriter().widgetActionSubOpPacket(action, subOp, widgetId, childId, itemId);
        });
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
            ClickManager.click(PacketInteractionType.WIDGET_INTERACT);
            client.getPacketWriter().widgetTargetOnGameObjectPacket(selectedWidgetId, itemId, slot, objectID, worldX, worldY, ctrl);
        });
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
            ClickManager.click(PacketInteractionType.WIDGET_INTERACT);
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
            ClickManager.click(PacketInteractionType.WIDGET_INTERACT);
            client.getPacketWriter().widgetTargetOnNpcPacket(npcIndex, selectedWidgetId, itemId, slot, ctrl);
        });
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
            ClickManager.click(PacketInteractionType.WIDGET_INTERACT);
            client.getPacketWriter().widgetTargetOnPlayerPacket(playerIndex, selectedWidgetId, itemId, slot, ctrl);
        });
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
            ClickManager.click(PacketInteractionType.WIDGET_INTERACT);
            client.getPacketWriter().widgetOnWidgetPacket(selectedWidgetId, itemId, slot, targetWidgetId, itemId2, slot2);
        });
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
