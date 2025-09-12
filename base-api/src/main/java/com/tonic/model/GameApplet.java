package com.tonic.model;

import com.tonic.Static;
import com.tonic.util.ReflectBuilder;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.VarbitID;
import static com.tonic.model.GameApplet.View.*;
import static net.runelite.api.gameval.InterfaceID.*;
import java.awt.*;

public class GameApplet
{
    GameApplet()
    {
        // hidden
    }

    /**
     * Get the current viewport area
     * @return the viewport area, or null if it could not be determined
     */
    public Rectangle getViewportArea()
    {
        return VIEWPORT.getArea();
    }

    /**
     * Get the current world viewport area
     * @return the world viewport area, or null if it could not be determined
     */
    public Rectangle getWorldViewportArea()
    {
        return WORLD.getArea();
    }

    /**
     * Get the current world map viewport area
     * @return the world map viewport area, or null if it could not be determined
     */
    public Rectangle getWorldMapViewportArea()
    {
        return WORLD_MAP.getArea();
    }

    /**
     * Get the current side menu area
     * @return the side menu area, or null if it could not be determined
     */
    public Rectangle getSideMenuArea()
    {
        return SIDE_MENU.getArea();
    }

    /**
     * Get the current chat box area
     * @return the chat box area, or null if it could not be determined
     */
    public Rectangle getChatBoxArea()
    {
        return CHAT_BOX.getArea();
    }

    @RequiredArgsConstructor
    enum View
    {
        VIEWPORT(Toplevel.GAMEFRAME, ToplevelOsrsStretch.GAMEFRAME, ToplevelPreEoc.GAMEFRAME),
        WORLD(Toplevel.OVERLAY_HUD, ToplevelOsrsStretch.HUD_CONTAINER_FRONT, ToplevelPreEoc.HUD_CONTAINER_FRONT),
        WORLD_MAP(Toplevel.MINIMAP, ToplevelOsrsStretch.MINIMAP, ToplevelPreEoc.MINIMAP),
        SIDE_MENU(Toplevel.SIDE, ToplevelOsrsStretch.SIDE_MENU, ToplevelPreEoc.SIDE_CONTAINER),
        CHAT_BOX(Toplevel.CHAT_CONTAINER, ToplevelOsrsStretch.CHAT_CONTAINER, ToplevelPreEoc.CHAT_CONTAINER),

        ;
        private final int CLASSIC_FIXED;
        private final int CLASSIC_STRETCH;
        private final int MODERN_STRETCH;

        private Rectangle getArea()
        {
            Object applet = Static.getClient();
            if(applet == null) return null;

            return Static.invoke(() -> {
                boolean isResized = ReflectBuilder.of(applet)
                        .method("isResized", null, null)
                        .get();

                int widgetId = isResized ? (readVarbit() == 1 ? MODERN_STRETCH : CLASSIC_STRETCH) : CLASSIC_FIXED;

                Object widget = ReflectBuilder.of(applet)
                        .method("getWidget", new Class[]{int.class}, new Object[]{widgetId})
                        .get();

                if(widget == null) return null;

                return ReflectBuilder.of(widget)
                        .method("getBounds", null, null)
                        .get();
            });
        }

        private int readVarbit()
        {
            Object client = Static.getClient();
            if(client == null) return -1;

            return Static.invoke(() -> ReflectBuilder.of(client)
                    .method(
                            "getVarbitValue",
                            new Class[]{int.class},
                            new Object[]{VarbitID.RESIZABLE_STONE_ARRANGEMENT}
                    ).get());
        }
    }
}
