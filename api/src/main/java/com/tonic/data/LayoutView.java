package com.tonic.data;

import com.tonic.Static;
import com.tonic.api.widgets.WidgetAPI;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

@RequiredArgsConstructor
@Getter
public enum LayoutView {
    GAMEFRAME(InterfaceID.Toplevel.GAMEFRAME, InterfaceID.ToplevelOsrsStretch.GAMEFRAME, InterfaceID.ToplevelPreEoc.GAMEFRAME),
    MAINMODEL(InterfaceID.Toplevel.MAINMODAL, InterfaceID.ToplevelOsrsStretch.MAINMODAL, InterfaceID.ToplevelPreEoc.MAINMODAL),
    VIEWPORT(InterfaceID.Toplevel.GAMEFRAME, InterfaceID.ToplevelOsrsStretch.GAMEFRAME, InterfaceID.ToplevelPreEoc.GAMEFRAME),
    WORLD(InterfaceID.Toplevel.OVERLAY_HUD, InterfaceID.ToplevelOsrsStretch.HUD_CONTAINER_FRONT, InterfaceID.ToplevelPreEoc.HUD_CONTAINER_FRONT),
    WORLD_MAP(InterfaceID.Toplevel.MINIMAP, InterfaceID.ToplevelOsrsStretch.MINIMAP, InterfaceID.ToplevelPreEoc.MINIMAP),
    SIDE_MENU(InterfaceID.Toplevel.SIDE, InterfaceID.ToplevelOsrsStretch.SIDE_MENU, InterfaceID.ToplevelPreEoc.SIDE_CONTAINER),
    CHAT_BOX(InterfaceID.Toplevel.CHAT_CONTAINER, InterfaceID.ToplevelOsrsStretch.CHAT_CONTAINER, InterfaceID.ToplevelPreEoc.CHAT_CONTAINER),
    MULTI_WAY(InterfaceID.Toplevel.MULTIWAY_ICON, InterfaceID.ToplevelOsrsStretch.MULTIWAY_ICON, InterfaceID.ToplevelPreEoc.MULTIWAY_ICON),
    GRAVESTONE(InterfaceID.Toplevel.GRAVESTONE, InterfaceID.ToplevelOsrsStretch.GRAVESTONE, InterfaceID.ToplevelPreEoc.GRAVESTONE),
    ;
    private final int CLASSIC_FIXED;
    private final int CLASSIC_STRETCH;
    private final int MODERN_STRETCH;

    public Widget getWidget() {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            int widgetId = client.isResized() ? (client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1 ? MODERN_STRETCH : CLASSIC_STRETCH) : CLASSIC_FIXED;
            return WidgetAPI.get(widgetId);
        });
    }

    public int getCurrentID() {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            return client.isResized() ? (client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1 ? MODERN_STRETCH : CLASSIC_STRETCH) : CLASSIC_FIXED;
        });
    }

    public boolean isVisible() {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            int widgetId = client.isResized() ? (client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1 ? MODERN_STRETCH : CLASSIC_STRETCH) : CLASSIC_FIXED;
            return WidgetAPI.isVisible(widgetId);
        });
    }
}
