package com.tonic.services.streamer;

import java.awt.Rectangle;

/**
 * Keeps the views apart while the client draws text holding the player's name: the text is drawn for the real view only over the area it may cover, then the replaced text for the mirror only. Runs on the client thread only.
 */
final class MirrorNameText implements StreamerName.Handler
{
    private static final MirrorNameText INSTANCE = new MirrorNameText();

    private final Rectangle area = new Rectangle();

    private MirrorNameText()
    {
    }

    /**
     * Starts handling the client's text draws for the mirror.
     */
    static void install()
    {
        StreamerName.setHandler(INSTANCE);
    }

    @Override
    public int openDraw()
    {
        return MirrorHiding.openDraw();
    }

    @Override
    public void real(int x, int y, int width, int height)
    {
        area.setBounds(x, y, Math.max(0, width), Math.max(0, height));
        MirrorHiding.hideFrame(area);
    }

    @Override
    public void mirror()
    {
        MirrorHiding.mirrorFrame(area);
    }

    @Override
    public void done()
    {
        MirrorHiding.end();
    }
}
