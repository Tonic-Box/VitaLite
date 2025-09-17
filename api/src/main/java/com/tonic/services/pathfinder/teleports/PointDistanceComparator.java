package com.tonic.services.pathfinder.teleports;

import net.runelite.api.coords.WorldPoint;

import java.util.Comparator;

public class PointDistanceComparator implements Comparator<Teleport> {
    private final WorldPoint referencePoint;

    public PointDistanceComparator(WorldPoint referencePoint) {
        if(referencePoint.getY() > 6400)
        {
            this.referencePoint = new WorldPoint(
                    referencePoint.getX(),
                    referencePoint.getY() - 6400,
                    referencePoint.getPlane()
            );
            return;
        }
        this.referencePoint = referencePoint;
    }

    @Override
    public int compare(Teleport p1, Teleport p2) {
        double dist1 = distance(referencePoint, p1);
        double dist2 = distance(referencePoint, p2);
        return Double.compare(dist1, dist2);
    }

    private double distance(WorldPoint p1, Teleport p2) {
        double dx = p1.getX() - p2.getDestination().getX();
        double dy = p1.getY() - p2.getDestination().getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}