package com.tonic.services.mouserecorder.alt;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates human-like mouse movement paths using Bezier curves and natural variance.
 * This class creates realistic cursor trajectories that mimic human behavior patterns
 * for security research and detection system testing.
 */
public class MousePathGenerator {

    private final Random random;

    @Getter
    @Setter
    private MovementProfile profile;

    public enum MovementProfile {
        CONSERVATIVE(0.7, 15, 30, 3.0, 0.15, 100, 250),
        MODERATE(1.0, 10, 20, 2.0, 0.10, 50, 150),
        AGGRESSIVE(1.5, 5, 15, 1.5, 0.08, 20, 80),
        CAUTIOUS(0.5, 20, 40, 4.0, 0.20, 150, 350);

        final double speedMultiplier;
        final int minPoints;
        final int maxPoints;
        final double jitterVariance;
        final double overshootProbability;
        final int minDelayMs;
        final int maxDelayMs;

        MovementProfile(double speedMultiplier, int minPoints, int maxPoints,
                        double jitterVariance, double overshootProbability,
                        int minDelayMs, int maxDelayMs) {
            this.speedMultiplier = speedMultiplier;
            this.minPoints = minPoints;
            this.maxPoints = maxPoints;
            this.jitterVariance = jitterVariance;
            this.overshootProbability = overshootProbability;
            this.minDelayMs = minDelayMs;
            this.maxDelayMs = maxDelayMs;
        }
    }

    @Getter
    public static class PathPoint {
        private final Point point;
        private final long delayMs;
        private final boolean isOvershoot;

        public PathPoint(Point point, long delayMs, boolean isOvershoot) {
            this.point = point;
            this.delayMs = delayMs;
            this.isOvershoot = isOvershoot;
        }

        public int getX() { return point.x; }
        public int getY() { return point.y; }
    }

    public MousePathGenerator() { this(MovementProfile.MODERATE); }

    public MousePathGenerator(MovementProfile profile) {
        this.random = new Random();
        this.profile = profile;
    }

    public MousePathGenerator(MovementProfile profile, long seed) {
        this.random = new Random(seed);
        this.profile = profile;
    }

    public List<PathPoint> generatePath(int startX, int startY, int endX, int endY) {
        Point start = new Point(startX, startY);
        Point end = new Point(endX, endY);
        double distance = start.distance(end);

        List<Point> basePath = generateBezierPath(start, end, distance);
        basePath = addJitter(basePath);

        boolean shouldOvershoot = random.nextDouble() < profile.overshootProbability;
        List<PathPoint> pathPoints = addTimingInformation(basePath, distance, false);
        if (shouldOvershoot && distance > 50) {
            List<PathPoint> overshootPath = generateOvershootCorrection(end, distance);
            pathPoints.addAll(overshootPath);
        }
        return pathPoints;
    }

    private List<Point> generateBezierPath(Point start, Point end, double distance) {
        List<Point> points = new ArrayList<>();
        int numPoints = calculatePointCount(distance);
        boolean useCubic = distance > 100 || random.nextBoolean();

        if (useCubic) {
            Point cp1 = generateControlPoint(start, end, 0.25, distance * 0.3);
            Point cp2 = generateControlPoint(start, end, 0.75, distance * 0.3);
            for (int i = 0; i <= numPoints; i++) {
                double t = (double) i / numPoints;
                Point p = cubicBezier(start, cp1, cp2, end, t);
                points.add(p);
            }
        } else {
            Point cp = generateControlPoint(start, end, 0.5, distance * 0.25);
            for (int i = 0; i <= numPoints; i++) {
                double t = (double) i / numPoints;
                Point p = quadraticBezier(start, cp, end, t);
                points.add(p);
            }
        }
        return points;
    }

    private Point generateControlPoint(Point start, Point end, double t, double variance) {
        double angle = Math.atan2(end.y - start.y, end.x - start.x);
        double distance = start.distance(end);
        double baseX = start.x + (end.x - start.x) * t;
        double baseY = start.y + (end.y - start.y) * t;
        double offsetAngle = angle + (random.nextDouble() - 0.5) * Math.PI / 3;
        double offsetDist = random.nextDouble() * variance;
        int x = (int) (baseX + Math.cos(offsetAngle) * offsetDist);
        int y = (int) (baseY + Math.sin(offsetAngle) * offsetDist);
        return new Point(x, y);
    }

    private Point quadraticBezier(Point p0, Point p1, Point p2, double t) {
        double u = 1 - t;
        int x = (int) (u * u * p0.x + 2 * u * t * p1.x + t * t * p2.x);
        int y = (int) (u * u * p0.y + 2 * u * t * p1.y + t * t * p2.y);
        return new Point(x, y);
    }

    private Point cubicBezier(Point p0, Point p1, Point p2, Point p3, double t) {
        double u = 1 - t;
        double uu = u * u;
        double uuu = uu * u;
        double tt = t * t;
        double ttt = tt * t;

        double x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x;
        double y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y;
        return new Point((int) x, (int) y);
    }

    private List<Point> addJitter(List<Point> points) {
        List<Point> jittered = new ArrayList<>();
        Point prev = null;
        for (Point p : points) {
            int jitterX = (int) (randomGaussian(0, profile.jitterVariance));
            int jitterY = (int) (randomGaussian(0, profile.jitterVariance));
            Point jp = new Point(p.x + jitterX, p.y + jitterY);
            if (prev != null && prev.equals(jp)) {
                jp = new Point(jp.x + 1, jp.y);
            }
            jittered.add(jp);
            prev = jp;
        }
        return jittered;
    }

    private List<PathPoint> addTimingInformation(List<Point> path, double totalDistance, boolean isOvershoot) {
        List<PathPoint> pathPoints = new ArrayList<>();
        double baseDelay = calculateBaseDelay(totalDistance);
        for (int i = 0; i < path.size(); i++) {
            Point p = path.get(i);
            double varianceFactor = 0.15 + random.nextDouble() * 0.15;
            long delay = (long) Math.max(1, randomGaussian(baseDelay, baseDelay * varianceFactor));
            delay = Math.max(profile.minDelayMs, Math.min(profile.maxDelayMs, delay));
            pathPoints.add(new PathPoint(p, delay, isOvershoot));
        }
        return pathPoints;
    }

    private List<PathPoint> generateOvershootCorrection(Point target, double originalDistance) {
        double overshootPercent = 0.05 + random.nextDouble() * 0.10;
        double overshootDistance = Math.min(originalDistance * overshootPercent, 30);
        double angle = (random.nextDouble() - 0.3) * Math.PI / 3;
        int overshootX = target.x + (int) (overshootDistance * Math.cos(angle));
        int overshootY = target.y + (int) (overshootDistance * Math.sin(angle));
        Point overshootPoint = new Point(overshootX, overshootY);

        int correctionPoints = 3 + random.nextInt(4);
        List<Point> correctionPath = new ArrayList<>();
        for (int i = 1; i <= correctionPoints; i++) {
            double t = (double) i / correctionPoints;
            int x = (int) (overshootPoint.x + (target.x - overshootPoint.x) * t);
            int y = (int) (overshootPoint.y + (target.y - overshootPoint.y) * t);
            correctionPath.add(new Point(x, y));
        }
        correctionPath = addJitter(correctionPath);
        return addTimingInformation(correctionPath, overshootDistance, true);
    }

    private int calculatePointCount(double distance) {
        int basePoints = (int) (distance / 15);
        basePoints = Math.max(profile.minPoints, Math.min(profile.maxPoints, basePoints));
        int variance = profile.maxPoints - profile.minPoints;
        basePoints += random.nextInt(Math.max(1, variance / 4)) - variance / 8;
        return Math.max(profile.minPoints, basePoints);
    }

    private double calculateBaseDelay(double distance) {
        double a = profile.minDelayMs;
        double b = (profile.maxDelayMs - profile.minDelayMs) * profile.speedMultiplier;
        double fittsDelay = a + b * (Math.log(distance / 20.0 + 1) / Math.log(2));
        return fittsDelay / calculatePointCount(distance);
    }

    private double randomGaussian(double mean, double stdDev) {
        return mean + random.nextGaussian() * stdDev;
    }

    public static double calculatePathLength(List<PathPoint> path) {
        if (path.size() < 2) return 0;
        double totalDistance = 0;
        for (int i = 1; i < path.size(); i++) {
            Point p1 = path.get(i - 1).getPoint();
            Point p2 = path.get(i).getPoint();
            totalDistance += p1.distance(p2);
        }
        return totalDistance;
    }

    public static long calculatePathDuration(List<PathPoint> path) {
        return path.stream().mapToLong(PathPoint::getDelayMs).sum();
    }
}

