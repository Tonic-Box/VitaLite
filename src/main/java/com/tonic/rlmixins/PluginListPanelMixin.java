package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Delegate;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Redirect;
import com.tonic.injector.util.StreamerBridges;

import java.util.Collection;
import java.util.List;

/**
 * Has RuneLite's plugin list show its rows as the streamer mirror does while streamer mode is on, so the rows of the two line up.
 */
@Mixin("net/runelite/client/plugins/config/PluginListPanel")
public class PluginListPanelMixin
{
    /**
     * Orders the rows the plugin list shows, in place of its search while streamer mode is on.
     *
     * @param rows the plugin list's rows
     * @param query the search text
     * @return the rows to show, in order
     */
    @Redirect(method = "onSearchBarChanged", at = @At(value = AtTarget.INVOKE, owner = "net/runelite/client/plugins/config/PluginSearch", target = "search"), expectedMatches = 1, gate = StreamerBridges.STREAMER_ACTIVE)
    @Delegate("com/tonic/services/streamer/SidebarOrder.pluginRows")
    public static native List<Object> streamerPluginRows(Collection<?> rows, String query);
}
