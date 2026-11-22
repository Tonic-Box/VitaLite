package com.tonic.api;

/**
 * The client's right-click menu, as the streamer mirror drives it.
 */
public interface TMenu
{
    /**
     * Draws the menu where it is laid out, as the client does each frame while it is open.
     */
    void drawMenu();

    /**
     * Lays the menu out as the client does when it is opened at a point.
     *
     * @param x where it was opened, across the canvas
     * @param y where it was opened, down the canvas
     */
    void layoutMenu(int x, int y);
}
