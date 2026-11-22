package com.tonic.services.streamer;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * What an overlay drew, call by call, so the same drawing can be made again elsewhere without running the overlay a second time. Graphics the overlay created are numbered, the one it was handed being 0.
 */
final class DrawingLog
{
    private final List<Consumer<Replay>> calls = new ArrayList<>();
    private final Graphics2D start;
    private int graphicsCount = 1;

    /**
     * @param start a copy of the graphics the overlay is handed, taken before it draws, which the log is replayed onto
     */
    DrawingLog(Graphics2D start)
    {
        this.start = start;
    }

    /**
     * Logs a call made on one of the overlay's graphics.
     *
     * @param graphics the number of the graphics it was made on
     * @param call the call
     */
    void add(int graphics, Consumer<Graphics2D> call)
    {
        calls.add(replay -> call.accept(replay.graphics[graphics]));
    }

    /**
     * Logs an absolute transform being set, which the replay keeps moved by its offset.
     *
     * @param graphics the number of the graphics it was set on
     * @param transform the transform
     */
    void setTransform(int graphics, AffineTransform transform)
    {
        AffineTransform copy = new AffineTransform(transform);
        calls.add(replay ->
        {
            AffineTransform moved = new AffineTransform(replay.offset);
            moved.concatenate(copy);
            replay.graphics[graphics].setTransform(moved);
        });
    }

    /**
     * Logs a graphics being created from another.
     *
     * @param parent the number of the graphics it was created from
     * @return the number of the new graphics
     */
    int create(int parent)
    {
        int child = graphicsCount++;
        calls.add(replay -> replay.graphics[child] = (Graphics2D) replay.graphics[parent].create());
        return child;
    }

    /**
     * Logs a created graphics being disposed.
     *
     * @param graphics its number
     */
    void dispose(int graphics)
    {
        calls.add(replay ->
        {
            replay.graphics[graphics].dispose();
            replay.graphics[graphics] = null;
        });
    }

    /**
     * @return the copy of the overlay's starting graphics
     */
    Graphics2D start()
    {
        return start;
    }

    /**
     * Makes the logged drawing again, moved by an offset in device pixels.
     *
     * @param onto graphics in the state the overlay's graphics had when logging began
     * @param dx how far right to move the drawing
     * @param dy how far down to move the drawing
     */
    void replay(Graphics2D onto, int dx, int dy)
    {
        Replay replay = new Replay(graphicsCount, AffineTransform.getTranslateInstance(dx, dy));
        AffineTransform moved = new AffineTransform(replay.offset);
        moved.concatenate(onto.getTransform());
        onto.setTransform(moved);
        replay.graphics[0] = onto;
        try
        {
            for (Consumer<Replay> call : calls)
            {
                call.accept(replay);
            }
        }
        finally
        {
            for (int i = 1; i < replay.graphics.length; i++)
            {
                if (replay.graphics[i] != null)
                {
                    replay.graphics[i].dispose();
                }
            }
        }
    }

    /**
     * Copies a shape the overlay might change after drawing it.
     *
     * @param shape the shape
     * @return a copy, or the shape itself when it cannot be copied
     */
    static Shape copy(Shape shape)
    {
        if (shape instanceof RectangularShape)
        {
            return (Shape) ((RectangularShape) shape).clone();
        }
        if (shape instanceof Path2D)
        {
            return (Shape) ((Path2D) shape).clone();
        }
        if (shape instanceof Polygon)
        {
            Polygon polygon = (Polygon) shape;
            return new Polygon(polygon.xpoints.clone(), polygon.ypoints.clone(), polygon.npoints);
        }
        if (shape instanceof Line2D)
        {
            return (Shape) ((Line2D) shape).clone();
        }
        if (shape instanceof Area)
        {
            return (Shape) ((Area) shape).clone();
        }
        return shape;
    }

    private static final class Replay
    {
        private final Graphics2D[] graphics;
        private final AffineTransform offset;

        private Replay(int graphicsCount, AffineTransform offset)
        {
            this.graphics = new Graphics2D[graphicsCount];
            this.offset = offset;
        }
    }
}
