package com.tonic.util;

import com.tonic.Static;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.TileOverlays;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;

import static net.runelite.api.Perspective.LOCAL_HALF_TILE_SIZE;

public class TileDrawingUtil
{
    public static void renderPolygon(Graphics2D graphics, Shape poly, Color color, Color fillColor, Stroke borderStroke)
    {
        if(poly == null)
            return;
        graphics.setColor(color);
        final Stroke originalStroke = graphics.getStroke();
        graphics.setStroke(borderStroke);
        graphics.draw(poly);
        graphics.setColor(fillColor);
        graphics.fill(poly);
        graphics.setStroke(originalStroke);
    }

    public static void drawNorthLine(Client client, Graphics2D graphics, LocalPoint localLocation, Color color, int strokeWidth) {
        var wv = client.getTopLevelWorldView();
        if (wv == null) return;

        int plane = wv.getPlane();

        // North side runs from NW to NE corner
        // Y increases going north in OSRS
        LocalPoint nw = new LocalPoint(
                localLocation.getX() - LOCAL_HALF_TILE_SIZE,
                localLocation.getY() + LOCAL_HALF_TILE_SIZE,
                localLocation.getWorldView()
        );
        LocalPoint ne = new LocalPoint(
                localLocation.getX() + LOCAL_HALF_TILE_SIZE,
                localLocation.getY() + LOCAL_HALF_TILE_SIZE,
                localLocation.getWorldView()
        );

        Point nwPoint = Perspective.localToCanvas(client, nw, plane);
        Point nePoint = Perspective.localToCanvas(client, ne, plane);

        if (nwPoint != null && nePoint != null) {
            graphics.setColor(color);
            graphics.setStroke(new BasicStroke(strokeWidth));
            graphics.drawLine(nwPoint.getX(), nwPoint.getY(), nePoint.getX(), nePoint.getY());
        }
    }

    public static void drawEastLine(Client client, Graphics2D graphics, LocalPoint localLocation, Color color, int strokeWidth) {
        var wv = client.getWorldView(localLocation.getWorldView());
        if (wv == null) return;

        int plane = wv.getPlane();

        // East side runs from NE to SE corner
        LocalPoint ne = new LocalPoint(
                localLocation.getX() + LOCAL_HALF_TILE_SIZE,
                localLocation.getY() + LOCAL_HALF_TILE_SIZE,
                localLocation.getWorldView()
        );
        LocalPoint se = new LocalPoint(
                localLocation.getX() + LOCAL_HALF_TILE_SIZE,
                localLocation.getY() - LOCAL_HALF_TILE_SIZE,
                localLocation.getWorldView()
        );

        Point nePoint = Perspective.localToCanvas(client, ne, plane);
        Point sePoint = Perspective.localToCanvas(client, se, plane);

        if (nePoint != null && sePoint != null) {
            graphics.setColor(color);
            graphics.setStroke(new BasicStroke(strokeWidth));
            graphics.drawLine(nePoint.getX(), nePoint.getY(), sePoint.getX(), sePoint.getY());
        }
    }

    public static void drawSouthLine(Client client, Graphics2D graphics, LocalPoint localLocation, Color color, int strokeWidth) {
        var wv = client.getWorldView(localLocation.getWorldView());
        if (wv == null) return;

        int plane = wv.getPlane();

        // South side runs from SW to SE corner
        LocalPoint sw = new LocalPoint(
                localLocation.getX() - LOCAL_HALF_TILE_SIZE,
                localLocation.getY() - LOCAL_HALF_TILE_SIZE,
                localLocation.getWorldView()
        );
        LocalPoint se = new LocalPoint(
                localLocation.getX() + LOCAL_HALF_TILE_SIZE,
                localLocation.getY() - LOCAL_HALF_TILE_SIZE,
                localLocation.getWorldView()
        );

        Point swPoint = Perspective.localToCanvas(client, sw, plane);
        Point sePoint = Perspective.localToCanvas(client, se, plane);

        if (swPoint != null && sePoint != null) {
            graphics.setColor(color);
            graphics.setStroke(new BasicStroke(strokeWidth));
            graphics.drawLine(swPoint.getX(), swPoint.getY(), sePoint.getX(), sePoint.getY());
        }
    }

    public static void drawWestLine(Client client, Graphics2D graphics, LocalPoint localLocation, Color color, int strokeWidth) {
        var wv = client.getWorldView(localLocation.getWorldView());
        if (wv == null) return;

        int plane = wv.getPlane();

        // West side runs from SW to NW corner
        LocalPoint sw = new LocalPoint(
                localLocation.getX() - LOCAL_HALF_TILE_SIZE,
                localLocation.getY() - LOCAL_HALF_TILE_SIZE,
                localLocation.getWorldView()
        );
        LocalPoint nw = new LocalPoint(
                localLocation.getX() - LOCAL_HALF_TILE_SIZE,
                localLocation.getY() + LOCAL_HALF_TILE_SIZE,
                localLocation.getWorldView()
        );

        Point swPoint = Perspective.localToCanvas(client, sw, plane);
        Point nwPoint = Perspective.localToCanvas(client, nw, plane);

        if (swPoint != null && nwPoint != null) {
            graphics.setColor(color);
            graphics.setStroke(new BasicStroke(strokeWidth));
            graphics.drawLine(swPoint.getX(), swPoint.getY(), nwPoint.getX(), nwPoint.getY());
        }
    }

    public static void renderWall(Graphics2D g2d, LocalPoint localLocation, Color color, TileOverlays.Wall.Direction direction) {
        Client client = Static.getClient();
        if(direction == TileOverlays.Wall.Direction.NORTH) {
            drawNorthLine(client, g2d, localLocation, color, 2);
        }
        else if(direction == TileOverlays.Wall.Direction.EAST) {
            drawEastLine(client, g2d, localLocation, color, 2);
        }
        else if(direction == TileOverlays.Wall.Direction.SOUTH) {
            drawSouthLine(client, g2d, localLocation, color, 2);
        }
        else if(direction == TileOverlays.Wall.Direction.WEST) {
            drawWestLine(client, g2d, localLocation, color, 2);
        }
    }

    private static Shape finaObjectShape(LocalPoint location) {
        Client client = Static.getClient();
        WorldPoint worldPoint = WorldPoint.fromLocal(client, location);
        TileObjectEx obj = new TileObjectQuery()
                .keepIf(o -> o.getWorldArea().contains(worldPoint))
                .first();

        if(obj == null)
            return null;
        return obj.getShape();
    }
}