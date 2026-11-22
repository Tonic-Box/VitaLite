package com.tonic.api;

/**
 * One chat type's line buffer, which the streamer mirror narrows for a moment while it builds its own chatbox.
 */
public interface TChatLineBuffer
{
    /**
     * Sets how many of the buffer's lines are in use.
     *
     * @param count the number of lines, newest first
     */
    void setLineCount(int count);
}
