package com.tonic.services.mouserecorder.trajectory;

import com.tonic.services.mouserecorder.MouseDataPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Noise generation for trajectory variation.
 * Provides white noise and spatially correlated noise options.
 */
public class NoiseGenerator
{
    private final Random random;

    public NoiseGenerator(Random random)
    {
        this.random = random;
    }

    public List<MouseDataPoint> applyWhiteNoise(List<MouseDataPoint> points, double sigma)
    {
        if (sigma <= 0) return new ArrayList<>(points);

        List<MouseDataPoint> noisy = new ArrayList<>();

        for (MouseDataPoint point : points)
        {
            double noiseX = random.nextGaussian() * sigma;
            double noiseY = random.nextGaussian() * sigma;

            int newX = (int) Math.round(point.getX() + noiseX);
            int newY = (int) Math.round(point.getY() + noiseY);

            noisy.add(new MouseDataPoint(newX, newY, point.getTimestampMillis()));
        }

        return noisy;
    }

    public List<MouseDataPoint> applyCorrelatedNoise(List<MouseDataPoint> points, double sigma, double correlation)
    {
        if (sigma <= 0) return new ArrayList<>(points);

        List<MouseDataPoint> noisy = new ArrayList<>();
        double prevNoiseX = 0;
        double prevNoiseY = 0;

        for (int i = 0; i < points.size(); i++)
        {
            MouseDataPoint point = points.get(i);

            double noiseX = prevNoiseX * correlation + random.nextGaussian() * sigma * Math.sqrt(1 - correlation * correlation);
            double noiseY = prevNoiseY * correlation + random.nextGaussian() * sigma * Math.sqrt(1 - correlation * correlation);

            prevNoiseX = noiseX;
            prevNoiseY = noiseY;

            int newX = (int) Math.round(point.getX() + noiseX);
            int newY = (int) Math.round(point.getY() + noiseY);

            noisy.add(new MouseDataPoint(newX, newY, point.getTimestampMillis()));
        }

        return noisy;
    }
}
