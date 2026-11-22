package com.tonic.services.streamer;

import java.awt.Graphics2D;
import java.util.function.BiConsumer;

/**
 * Graphics that draw the streamer name in place of the player's in the text they draw. Graphics drawing for both views draw such text for the real view only and the replaced text for the mirror only; graphics already drawing for the mirror only just replace it. Runs on the client thread only.
 */
final class NameGraphics extends DelegatingGraphics
{
    private final boolean shared;

    private NameGraphics(Graphics2D graphics, boolean shared, boolean owned)
    {
        super(graphics, owned);
        this.shared = shared;
    }

    /**
     * @param graphics graphics drawing for both views
     * @return graphics showing the streamer name in the mirror, or the same graphics while no name of the player's is known
     */
    static Graphics2D shared(Graphics2D graphics)
    {
        return graphics == null || !StreamerName.hasNames() ? graphics : new NameGraphics(graphics, true, false);
    }

    /**
     * @param graphics graphics drawing for the mirror only
     * @return graphics drawing the streamer name in place of the player's, or the same graphics while no name of the player's is known
     */
    static Graphics2D mirrorOnly(Graphics2D graphics)
    {
        return graphics == null || !StreamerName.hasNames() ? graphics : new NameGraphics(graphics, false, false);
    }

    @Override
    protected Graphics2D wrap(Graphics2D child)
    {
        return new NameGraphics(child, shared, true);
    }

    @Override
    public void drawString(String text, int x, int y)
    {
        String replaced = StreamerName.replace(text);
        if (replaced == null)
        {
            graphics.drawString(text, x, y);
        }
        else
        {
            drawName(text, replaced, (onto, shown) -> onto.drawString(shown, x, y));
        }
    }

    @Override
    public void drawString(String text, float x, float y)
    {
        String replaced = StreamerName.replace(text);
        if (replaced == null)
        {
            graphics.drawString(text, x, y);
        }
        else
        {
            drawName(text, replaced, (onto, shown) -> onto.drawString(shown, x, y));
        }
    }

    private void drawName(String text, String replaced, BiConsumer<Graphics2D, String> draw)
    {
        if (!shared)
        {
            draw.accept(graphics, replaced);
            return;
        }
        MirrorHiding.drawHidden(graphics, hidden -> draw.accept(hidden, text));
        MirrorHiding.drawMirrorOnly(graphics, mirror -> draw.accept(mirror, replaced));
    }
}
