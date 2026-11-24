package com.tonic.services.pathfinder.ui;

import net.runelite.client.config.ConfigManager;

/**
 * The transport editor's and recorder overlay's run-to-run settings in the vitaliteTransports config group; without a config manager they last the run.
 */
public final class TransportEditorSettings
{
    private static final String GROUP = "vitaliteTransports";
    private static final String DIVIDER_KEY = "editor.divider";
    private static final String ON_TOP_KEY = "editor.alwaysOnTop";
    private static final String ORIGIN_KEY = "editor.originFilter";
    private static final String RECORDER_OVERLAY_KEY = "recorderOverlay";

    private final ConfigManager config;
    private volatile int divider = 560;
    private volatile boolean alwaysOnTop;
    private volatile String originFilter = "All";
    private volatile boolean recorderOverlay = true;

    /**
     * @param config where the settings are kept, or null to keep them for the run only
     */
    public TransportEditorSettings(ConfigManager config)
    {
        this.config = config;
        if (config == null)
        {
            return;
        }

        String storedDivider = config.getConfiguration(GROUP, DIVIDER_KEY);
        if (storedDivider != null)
        {
            try
            {
                divider = Integer.parseInt(storedDivider);
            }
            catch (NumberFormatException ignored)
            {
            }
        }
        alwaysOnTop = Boolean.parseBoolean(config.getConfiguration(GROUP, ON_TOP_KEY));
        String origin = config.getConfiguration(GROUP, ORIGIN_KEY);
        if (origin != null && !origin.isEmpty())
        {
            originFilter = origin;
        }
        String overlay = config.getConfiguration(GROUP, RECORDER_OVERLAY_KEY);
        if (overlay != null)
        {
            recorderOverlay = Boolean.parseBoolean(overlay);
        }
    }

    /**
     * @return the editor's split divider position
     */
    public int divider()
    {
        return divider;
    }

    /**
     * @param divider the editor's split divider position
     */
    public void divider(int divider)
    {
        this.divider = divider;
        store(DIVIDER_KEY, Integer.toString(divider));
    }

    /**
     * @return whether the editor stays on top of other windows
     */
    public boolean alwaysOnTop()
    {
        return alwaysOnTop;
    }

    /**
     * @param alwaysOnTop whether the editor stays on top of other windows
     */
    public void alwaysOnTop(boolean alwaysOnTop)
    {
        this.alwaysOnTop = alwaysOnTop;
        store(ON_TOP_KEY, Boolean.toString(alwaysOnTop));
    }

    /**
     * @return which rows the editor lists: All, Live or Staged
     */
    public String originFilter()
    {
        return originFilter;
    }

    /**
     * @param originFilter which rows the editor lists: All, Live or Staged
     */
    public void originFilter(String originFilter)
    {
        this.originFilter = originFilter;
        store(ORIGIN_KEY, originFilter);
    }

    /**
     * @return whether the recorder overlay is drawn while recording
     */
    public boolean recorderOverlay()
    {
        return recorderOverlay;
    }

    /**
     * @param recorderOverlay whether the recorder overlay is drawn while recording
     */
    public void recorderOverlay(boolean recorderOverlay)
    {
        this.recorderOverlay = recorderOverlay;
        store(RECORDER_OVERLAY_KEY, Boolean.toString(recorderOverlay));
    }

    private void store(String key, String value)
    {
        if (config != null)
        {
            config.setConfiguration(GROUP, key, value);
        }
    }
}
