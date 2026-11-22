package com.tonic.services.streamer;

import com.tonic.services.profiler.frames.FrameProfiler;
import net.runelite.api.gameval.InterfaceID;

/**
 * Where the client reports chat messages being added and its chatbox lines being drawn, so the streamer mirror can show a chatbox without the messages that content which is not stock added. The client calls in here; the mirror registers the handler that does the work.
 */
public final class StreamerChat
{
    private static final int CHAT = FrameProfiler.timer("mirror.chat");
    private static volatile Handler handler;
    private static long chatBegan;

    private StreamerChat()
    {
    }

    /**
     * @param chatHandler the handler to report to, or null to stop
     */
    public static void setHandler(Handler chatHandler)
    {
        handler = chatHandler;
    }

    /**
     * Called by the client after it adds a chat message on a caller's behalf.
     *
     * @param node the message just added, the client's MessageNode
     */
    public static void added(Object node)
    {
        Handler current = handler;
        if (current != null && node != null)
        {
            current.added(node);
        }
    }

    /**
     * Called by the client before it draws the children of a component while streamer mode is on.
     *
     * @param widgets the components the client is about to draw
     * @param parentId the component whose children they are
     * @return the components the mirror shows in their place, or null to draw the real ones for both
     */
    public static Object[] mirrorOf(Object[] widgets, int parentId)
    {
        Handler current = handler;
        return parentId != InterfaceID.Chatbox.SCROLLAREA || current == null ? null : current.mirrorOf(widgets);
    }

    /**
     * Called by the client before it draws the real chatbox lines, which the mirror leaves out.
     *
     * @param clipX1 the left of the area drawn
     * @param clipY1 the top of the area drawn
     * @param clipX2 the right of the area drawn
     * @param clipY2 the bottom of the area drawn
     */
    public static void real(int clipX1, int clipY1, int clipX2, int clipY2)
    {
        chatBegan = FrameProfiler.begin();
        Handler current = handler;
        if (current != null)
        {
            current.real(clipX1, clipY1, clipX2, clipY2);
        }
    }

    /**
     * Called by the client after it draws the real chatbox lines, before it draws the mirror's.
     *
     * @return how far down to move the mirror's lines from where the real ones were drawn
     */
    public static int mirror()
    {
        Handler current = handler;
        return current == null ? 0 : current.mirror();
    }

    /**
     * Called by the client once the mirror's chatbox lines are drawn.
     */
    public static void done()
    {
        Handler current = handler;
        if (current != null)
        {
            current.done();
        }
        FrameProfiler.end(CHAT, chatBegan);
    }

    /**
     * What the streamer mirror does with the client's chat.
     */
    public interface Handler
    {
        /**
         * @param node the message just added, the client's MessageNode
         */
        void added(Object node);

        /**
         * @param widgets the chatbox line components the client is about to draw
         * @return the components the mirror shows in their place, or null to draw the real ones for both
         */
        Object[] mirrorOf(Object[] widgets);

        /**
         * Starts the real lines' draw, which the mirror leaves out.
         *
         * @param clipX1 the left of the area drawn
         * @param clipY1 the top of the area drawn
         * @param clipX2 the right of the area drawn
         * @param clipY2 the bottom of the area drawn
         */
        void real(int clipX1, int clipY1, int clipX2, int clipY2);

        /**
         * Starts the mirror's lines' draw.
         *
         * @return how far down to move the mirror's lines from where the real ones were drawn
         */
        int mirror();

        /**
         * Ends the mirror's lines' draw.
         */
        void done();
    }
}
