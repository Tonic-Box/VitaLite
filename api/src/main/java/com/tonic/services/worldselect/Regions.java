package com.tonic.services.worldselect;

import net.runelite.client.plugins.worldhopper.WorldHopperPlugin;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.worlds.WorldRegion;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * The names and flags of the regions worlds are hosted in; the flags are RuneLite's world hopper ones.
 */
final class Regions
{
    private static final Map<WorldRegion, BufferedImage> flags = new EnumMap<>(WorldRegion.class);

    private Regions()
    {
    }

    /**
     * @param region a region
     * @return its name, as a player would write it
     */
    static String name(WorldRegion region)
    {
        switch (region)
        {
            case UNITED_STATES_OF_AMERICA:
                return "United States";
            case UNITED_KINGDOM:
                return "United Kingdom";
            case AUSTRALIA:
                return "Australia";
            case GERMANY:
                return "Germany";
            case BRAZIL:
                return "Brazil";
            case JAPAN:
                return "Japan";
            case SINGAPORE:
                return "Singapore";
            case SOUTH_AFRICA:
                return "South Africa";
            default:
                return region.name();
        }
    }

    /**
     * @param region a region
     * @return its flag, loaded once, or null when RuneLite has none for it
     */
    static synchronized BufferedImage flag(WorldRegion region)
    {
        if (!flags.containsKey(region))
        {
            String file = flagFile(region);
            flags.put(region, file == null ? null : ImageUtil.loadImageResource(WorldHopperPlugin.class, file));
        }
        return flags.get(region);
    }

    private static String flagFile(WorldRegion region)
    {
        switch (region)
        {
            case UNITED_STATES_OF_AMERICA:
                return "flag_us.png";
            case UNITED_KINGDOM:
                return "flag_uk.png";
            case AUSTRALIA:
                return "flag_aus.png";
            case GERMANY:
                return "flag_ger.png";
            case BRAZIL:
                return "flag_br.png";
            case JAPAN:
                return "flag_jp.png";
            case SINGAPORE:
                return "flag_sg.png";
            case SOUTH_AFRICA:
                return "flag_za.png";
            default:
                return null;
        }
    }
}
