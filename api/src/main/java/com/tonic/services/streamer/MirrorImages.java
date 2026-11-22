package com.tonic.services.streamer;

import net.runelite.client.util.ImageUtil;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Images VitaLite recoloured, each with the image stock RuneLite shows in its place, which the streamer mirror draws instead.
 */
public final class MirrorImages
{
    private static final Map<Image, Image> twins = Collections.synchronizedMap(new WeakHashMap<>());

    private MirrorImages()
    {
    }

    /**
     * Gives an image recoloured with a restyled colour the image stock RuneLite recolours with its own colour. Called by ImageUtil after each recolour.
     *
     * @param recoloured the recoloured image
     * @param source the image recoloured
     * @param color the colour it was recoloured with
     */
    public static void recoloured(Image recoloured, Image source, Color color)
    {
        if (color instanceof MirrorColor && recoloured != null)
        {
            twins.put(recoloured, ImageUtil.recolorImage(source, ((MirrorColor) color).stock()));
        }
    }

    /**
     * Recolours an image stock RuneLite shows as it is, keeping the original for the mirror.
     *
     * @param source the image
     * @param color the colour to recolour it with
     * @return the recoloured image
     */
    public static BufferedImage recolourKeepingStock(Image source, Color color)
    {
        BufferedImage recoloured = ImageUtil.recolorImage(source, color);
        twins.put(recoloured, source);
        return recoloured;
    }

    /**
     * @param image an image about to be drawn for the mirror
     * @return the image stock RuneLite shows in its place
     */
    static Image forMirror(Image image)
    {
        Image twin = image == null ? null : twins.get(image);
        return twin != null ? twin : image;
    }
}
