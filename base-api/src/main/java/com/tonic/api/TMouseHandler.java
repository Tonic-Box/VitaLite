package com.tonic.api;

public interface TMouseHandler {
    /**
     * The last time a mouse button was pressed, in milliseconds.
     * @return The last time a mouse button was pressed, in milliseconds.
     */
    long getMouseLastPressedMillis();

    /**
     * Sets the last time a mouse button was pressed, in milliseconds.
     * @param millis The last time a mouse button was pressed, in milliseconds.
     */
    void setMouseLastPressedMillis(long millis);
}
