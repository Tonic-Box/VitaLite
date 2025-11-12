package com.tonic.services.mouserecorder.alt;

import com.tonic.services.mouserecorder.IMouseMovementGenerator;
import com.tonic.services.mouserecorder.MouseDataPoint;
import com.tonic.services.mouserecorder.MouseMovementSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * IMouseMovementGenerator implementation that uses a Bezier-path based
 * MousePathGenerator to produce human-like movement sequences.
 */
public class BezierMouseGenerator implements IMouseMovementGenerator
{
    private final MousePathGenerator generator;

    public BezierMouseGenerator()
    {
        this(new MousePathGenerator(MousePathGenerator.MovementProfile.MODERATE));
    }

    public BezierMouseGenerator(MousePathGenerator generator)
    {
        this.generator = generator;
    }

    @Override
    public MouseMovementSequence generate(int startX, int startY, int endX, int endY)
    {
        long startTime = System.currentTimeMillis();
        return generate(startX, startY, endX, endY, startTime);
    }

    @Override
    public MouseMovementSequence generate(int startX, int startY, int endX, int endY, long startTimeMs)
    {
        List<MousePathGenerator.PathPoint> path = generator.generatePath(startX, startY, endX, endY);

        long t = startTimeMs;
        List<MouseDataPoint> points = new ArrayList<>(path.size());
        for (MousePathGenerator.PathPoint pp : path)
        {
            t += Math.max(1, pp.getDelayMs());
            points.add(new MouseDataPoint(pp.getX(), pp.getY(), t));
        }

        if (points.isEmpty())
        {
            points.add(MouseDataPoint.now(endX, endY));
        }

        return new MouseMovementSequence(points);
    }

    @Override
    public String getName()
    {
        return "BezierMouseGenerator";
    }

    @Override
    public String getDescription()
    {
        return "Generates Bezier-curve based human-like movement with jitter and overshoot.";
    }
}

