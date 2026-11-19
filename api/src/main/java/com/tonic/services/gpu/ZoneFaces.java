package com.tonic.services.gpu;

import net.runelite.api.DynamicObject;
import net.runelite.api.Model;
import net.runelite.api.Renderable;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;

/**
 * Counts the faces the GPU plugin's scene uploader actually writes for a zone, so each zone buffer is sized to what it holds rather than to every face including the hidden ones it skips.
 */
public final class ZoneFaces
{
    private static final int HIDDEN_TILE = 12345678;
    private static final int HIDDEN_FACE = -2;

    private ZoneFaces()
    {
    }

    /**
     * @param paint the tile's paint
     * @return the faces uploaded for it
     */
    public static int paint(SceneTilePaint paint)
    {
        return paint.getNeColor() == HIDDEN_TILE ? 0 : 2;
    }

    /**
     * @param model the tile's model
     * @return the faces uploaded for it
     */
    public static int tileModel(SceneTileModel model)
    {
        int[] colors = model.getTriangleColorA();
        int faces = 0;
        for (int color : colors)
        {
            if (color != HIDDEN_TILE)
            {
                faces++;
            }
        }
        return faces;
    }

    /**
     * Counts a scenery renderable's uploaded faces.
     *
     * @param renderable the renderable
     * @return the opaque faces in the low 32 bits and the translucent faces in the high 32 bits
     */
    public static long renderable(Renderable renderable)
    {
        Model model = null;
        if (renderable instanceof Model)
        {
            model = (Model) renderable;
        }
        else if (renderable instanceof DynamicObject)
        {
            model = ((DynamicObject) renderable).getModelZbuf();
        }
        if (model == null)
        {
            return 0;
        }

        int faceCount = model.getFaceCount();
        int[] colors = model.getFaceColors3();
        byte[] transparencies = model.getFaceTransparencies();
        long opaque = 0;
        long translucent = 0;
        for (int face = 0; face < faceCount; face++)
        {
            if (colors[face] == HIDDEN_FACE)
            {
                continue;
            }
            if (transparencies != null && transparencies[face] != 0)
            {
                translucent++;
            }
            else
            {
                opaque++;
            }
        }
        return opaque | translucent << 32;
    }
}
