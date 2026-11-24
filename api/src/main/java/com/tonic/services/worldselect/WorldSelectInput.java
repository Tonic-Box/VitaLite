package com.tonic.services.worldselect;

import com.tonic.Static;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.input.MouseWheelListener;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * The world select's mouse and key handling, registered with RuneLite while it is open; it consumes every event, so the login screen under it sees none. Typing goes to the search, Enter switches world and Esc closes.
 */
final class WorldSelectInput implements MouseListener, MouseWheelListener, KeyListener
{
    private static final int MAX_SEARCH = 32;
    private static final int WHEEL_PIXELS = 78;

    private final WorldSelectPainter painter;
    private final WorldSelectModel model;

    private boolean dragging;
    private int dragOffset;

    /**
     * @param painter the painter whose drawn areas the pointer is tested against
     * @param model what typing searches
     */
    WorldSelectInput(WorldSelectPainter painter, WorldSelectModel model)
    {
        this.painter = painter;
        this.model = model;
    }

    /**
     * Starts taking RuneLite's mouse and key events.
     */
    void register()
    {
        dragging = false;
        Static.getInjector().getInstance(MouseManager.class).registerMouseListener(this);
        Static.getInjector().getInstance(MouseManager.class).registerMouseWheelListener(this);
        Static.getInjector().getInstance(KeyManager.class).registerKeyListener(this);
    }

    /**
     * Stops taking RuneLite's mouse and key events.
     */
    void unregister()
    {
        Static.getInjector().getInstance(MouseManager.class).unregisterMouseListener(this);
        Static.getInjector().getInstance(MouseManager.class).unregisterMouseWheelListener(this);
        Static.getInjector().getInstance(KeyManager.class).unregisterKeyListener(this);
    }

    @Override
    public MouseEvent mousePressed(MouseEvent event)
    {
        int x = event.getX();
        int y = event.getY();
        painter.pointer(x, y);
        if (event.getButton() == MouseEvent.BUTTON1)
        {
            if (painter.onThumb(x, y))
            {
                dragging = true;
                dragOffset = y - painter.thumbTop();
            }
            else if (painter.onTrack(x, y))
            {
                dragging = true;
                dragOffset = 0;
                painter.dragThumb(y);
            }
            else
            {
                painter.press(x, y, event.getClickCount());
            }
        }
        event.consume();
        return event;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent event)
    {
        dragging = false;
        event.consume();
        return event;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent event)
    {
        event.consume();
        return event;
    }

    @Override
    public MouseEvent mouseEntered(MouseEvent event)
    {
        event.consume();
        return event;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent event)
    {
        painter.pointer(-1, -1);
        event.consume();
        return event;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent event)
    {
        painter.pointer(event.getX(), event.getY());
        if (dragging)
        {
            painter.dragThumb(event.getY() - dragOffset);
        }
        event.consume();
        return event;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent event)
    {
        painter.pointer(event.getX(), event.getY());
        event.consume();
        return event;
    }

    @Override
    public MouseWheelEvent mouseWheelMoved(MouseWheelEvent event)
    {
        painter.scrollBy(event.getPreciseWheelRotation() * WHEEL_PIXELS);
        event.consume();
        return event;
    }

    @Override
    public boolean isEnabledOnLoginScreen()
    {
        return true;
    }

    @Override
    public void keyPressed(KeyEvent event)
    {
        switch (event.getKeyCode())
        {
            case KeyEvent.VK_ESCAPE:
                WorldSelect.close();
                break;
            case KeyEvent.VK_ENTER:
                WorldSelect.confirm();
                break;
            case KeyEvent.VK_BACK_SPACE:
                String search = model.getSearch();
                if (!search.isEmpty())
                {
                    model.setSearch(event.isControlDown() ? "" : search.substring(0, search.length() - 1));
                }
                break;
            default:
                break;
        }
        event.consume();
    }

    @Override
    public void keyReleased(KeyEvent event)
    {
        event.consume();
    }

    @Override
    public void keyTyped(KeyEvent event)
    {
        char typed = event.getKeyChar();
        if (typed != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(typed) && !event.isControlDown() && !event.isAltDown())
        {
            String search = model.getSearch();
            if (search.length() < MAX_SEARCH)
            {
                model.setSearch(search + typed);
            }
        }
        event.consume();
    }
}
