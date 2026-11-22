package com.tonic.services.streamer;

import javax.swing.plaf.ColorUIResource;
import java.awt.Color;

/**
 * A colour VitaLite restyled that shows its stock RuneLite value only while the streamer mirror prints, and VitaLite's value everywhere else.
 */
public class MirrorColor extends Color
{
    private final int stock;

    /**
     * @param real the colour VitaLite shows
     * @param stock the colour stock RuneLite shows
     */
    public MirrorColor(Color real, Color stock)
    {
        super(real.getRGB(), true);
        this.stock = stock.getRGB();
    }

    /**
     * @return the colour stock RuneLite shows
     */
    public Color stock()
    {
        return new Color(stock, true);
    }

    @Override
    public int getRGB()
    {
        return Streamer.mirrorPrinter == Thread.currentThread() ? stock : super.getRGB();
    }

    @Override
    public Color darker()
    {
        return new MirrorColor(new Color(super.getRGB(), true).darker(), stock().darker());
    }

    @Override
    public Color brighter()
    {
        return new MirrorColor(new Color(super.getRGB(), true).brighter(), stock().brighter());
    }

    /**
     * A look-and-feel default VitaLite restyled, showing its stock value only while the streamer mirror prints.
     */
    public static class Resource extends ColorUIResource
    {
        private final int stock;

        /**
         * @param real the colour VitaLite shows
         * @param stock the colour stock RuneLite shows
         */
        public Resource(Color real, Color stock)
        {
            super(real);
            this.stock = stock.getRGB();
        }

        @Override
        public int getRGB()
        {
            return Streamer.mirrorPrinter == Thread.currentThread() ? stock : super.getRGB();
        }

        @Override
        public Color darker()
        {
            return new Resource(new Color(super.getRGB(), true).darker(), new Color(stock, true).darker());
        }

        @Override
        public Color brighter()
        {
            return new Resource(new Color(super.getRGB(), true).brighter(), new Color(stock, true).brighter());
        }
    }
}
