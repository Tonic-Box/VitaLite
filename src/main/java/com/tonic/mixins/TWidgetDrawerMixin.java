package com.tonic.mixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Redirect;
import com.tonic.injector.annotations.Shadow;
import com.tonic.injector.util.StreamerBridges;
import com.tonic.services.streamer.StreamerChat;

/**
 * Draws a component's children twice while streamer mode is on, when StreamerChat gives the mirror its own: the real children for the real view only, then the mirror's, built without the chat messages that content which is not stock added, for the mirror only.
 */
@Mixin("WidgetDrawer")
public abstract class TWidgetDrawerMixin
{
    @Shadow("drawWidgets")
    public static void drawWidgets(Object drawer, Object[] widgets, int second, int third, int parentId, int fifth, int clipX1, int clipY1, int clipX2, int clipY2, int tenth, int y, Object twelfth, int thirteenth)
    {
    }

    /**
     * Draws a component's children, twice when the mirror has children of its own for them. Every draw of children in the widget drawer comes here while streamer mode is on.
     *
     * @param drawer the widget drawer
     * @param widgets the children
     * @param second passed on
     * @param third passed on
     * @param parentId the parent component's id
     * @param fifth passed on
     * @param clipX1 the clip's left
     * @param clipY1 the clip's top
     * @param clipX2 the clip's right
     * @param clipY2 the clip's bottom
     * @param tenth passed on
     * @param y the children's y offset
     * @param twelfth passed on
     * @param thirteenth passed on
     * @param garbage the client's check value, which the shadow call supplies itself
     */
    @Redirect(method = "*", at = @At(value = AtTarget.INVOKE, owner = "WidgetDrawer", target = "drawWidgets"), all = true, gate = StreamerBridges.STREAMER_MIRRORING)
    public static void streamerDrawWidgets(Object drawer, Object[] widgets, int second, int third, int parentId, int fifth, int clipX1, int clipY1, int clipX2, int clipY2, int tenth, int y, Object twelfth, int thirteenth, int garbage)
    {
        Object[] mirrorWidgets = StreamerChat.mirrorOf(widgets, parentId);
        if (mirrorWidgets == null)
        {
            drawWidgets(drawer, widgets, second, third, parentId, fifth, clipX1, clipY1, clipX2, clipY2, tenth, y, twelfth, thirteenth);
            return;
        }

        StreamerChat.real(clipX1, clipY1, clipX2, clipY2);
        try
        {
            drawWidgets(drawer, widgets, second, third, parentId, fifth, clipX1, clipY1, clipX2, clipY2, tenth, y, twelfth, thirteenth);
            int shift = StreamerChat.mirror();
            drawWidgets(drawer, mirrorWidgets, second, third, parentId, fifth, clipX1, clipY1, clipX2, clipY2, tenth, y + shift, twelfth, thirteenth);
        }
        finally
        {
            StreamerChat.done();
        }
    }
}
