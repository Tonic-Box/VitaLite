package com.tonic.ui;

import javax.swing.*;
import java.awt.*;

/**
 * A panel that always takes the scroll pane's width, so its contents wrap rather than scroll sideways.
 */
public final class ScrollColumn extends JPanel implements Scrollable {
    public ScrollColumn() {
        setBackground(VitaTheme.BACKGROUND);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(16, visible.height - 16);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
