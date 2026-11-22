package com.tonic.services.streamer;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Prints for the streamer mirror with stock colours, which MirrorColor gives only while the printing thread is marked as the mirror's printer, and lays out what it prints. Only the printing itself is marked, so listeners and layout run in between see VitaLite's colours. Runs on the event dispatch thread only.
 */
final class MirrorPrint
{
    private MirrorPrint()
    {
    }

    /**
     * Prints a component and its children with stock colours.
     *
     * @param component the component
     * @param graphics where to print it
     */
    static void printAll(Component component, Graphics graphics)
    {
        run(() -> component.printAll(graphics));
    }

    /**
     * Runs drawing for the mirror with stock colours.
     *
     * @param drawing the drawing
     */
    static void run(Runnable drawing)
    {
        Thread previous = Streamer.mirrorPrinter;
        Streamer.mirrorPrinter = Thread.currentThread();
        try
        {
            drawing.run();
        }
        finally
        {
            Streamer.mirrorPrinter = previous;
        }
    }

    /**
     * Lays out a container and everything in it, as printing does not lay out what it prints.
     *
     * @param container the container
     */
    static void layOut(Container container)
    {
        container.doLayout();
        for (Component child : container.getComponents())
        {
            if (child instanceof Container)
            {
                layOut((Container) child);
            }
        }
    }

    /**
     * Sizes a panel that is not showing to the area it is printed into, so it is laid out for it.
     *
     * @param wrapped the panel
     * @param area the area it is printed into
     */
    static void fit(JComponent wrapped, Rectangle area)
    {
        if (!wrapped.isShowing() && !wrapped.getSize().equals(area.getSize()))
        {
            wrapped.setSize(area.getSize());
            wrapped.validate();
        }
    }
}
