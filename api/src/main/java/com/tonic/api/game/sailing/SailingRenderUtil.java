package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.services.pathfinder.sailing.BoatCollisionAPI;
import com.tonic.services.pathfinder.sailing.Sailing;
import com.tonic.services.pathfinder.sailing.sea.BoatHull;
import com.tonic.services.pathfinder.sailing.sea.BoatKinematics;
import com.tonic.services.pathfinder.sailing.sea.BoatPose;
import com.tonic.services.pathfinder.sailing.sea.HullRule;
import com.tonic.services.pathfinder.sailing.sea.SeaGraph;
import com.tonic.services.pathfinder.sailing.sea.SeaMap;
import com.tonic.services.pathfinder.sailing.sea.SeaRoute;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayUtil;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Draws the player's boat and its voyage in overlays: hull, deck, route, predicted track, sea cells and the nav graph
 */
public class SailingRenderUtil
{
    private static final Color HULL_COLOR = new Color(255, 165, 0);
    private static final Color HULL_FILL = new Color(255, 165, 0, 40);
    private static final Color INSET_COLOR = new Color(255, 220, 120);
    private static final Color OFFENDING_FILL = new Color(255, 40, 40, 150);
    private static final Color DECK_COLOR = new Color(0, 255, 0);
    private static final Color DECK_FILL = new Color(0, 255, 0, 30);
    private static final Color ROUTE_COLOR = new Color(0, 220, 255);
    private static final Color GOAL_FILL = new Color(0, 220, 255, 60);
    private static final Color TRACK_COLOR = new Color(255, 255, 255, 110);
    private static final Color LAND_FILL = new Color(150, 110, 60, 110);
    private static final Color WALK_BLOCKED_FILL = new Color(160, 60, 200, 60);
    private static final Color[] CLEARANCE_COLORS = {new Color(255, 40, 40, 140), new Color(255, 120, 0, 120), new Color(255, 220, 0, 100)};
    private static final Color GRAPH_COLOR = new Color(120, 255, 160);
    private static final Stroke STROKE = new BasicStroke(2.0f);
    private static final Stroke THIN = new BasicStroke(1.0f);
    private static final Stroke DASHED = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f);
    private static final int FINE_SHIFT = 7;
    private static final int CELL = 32;
    private static final int NODE_SIZE = 3;
    private static final float NEAR = 50f;
    private static final int MINIMAP_RADIUS = 76;
    private static final int MINIMAP_REACH = 64 << FINE_SHIFT;
    private static final float ANGLE_TO_RADIANS = (float) (2 * Math.PI / 2048);

    /**
     * Draws the hull at the boat's real angle, its calibrated inset dashed, and any cells it overlaps land in red
     * @param graphics the graphics to draw on
     */
    public static void renderPlayerBoatHull(Graphics2D graphics)
    {
        renderPlayerBoatHull(graphics, HULL_COLOR, HULL_FILL, STROKE);
    }

    /**
     * Draws the hull at the boat's real angle in the given colours, its calibrated inset dashed and any land it overlaps in red
     * @param graphics the graphics to draw on
     * @param outlineColor the outline colour
     * @param fillColor the fill colour
     * @param stroke the outline stroke
     */
    public static void renderPlayerBoatHull(Graphics2D graphics, Color outlineColor, Color fillColor, Stroke stroke)
    {
        Sailing sailing = Sailing.get();
        BoatPose pose = sailing.pose();
        BoatHull hull = sailing.hull();
        if (pose == null || hull == null)
        {
            return;
        }
        drawHull(graphics, pose, hull.halfAcross(), hull.halfAlong(), outlineColor, fillColor, stroke);
        int inset = HullRule.CALIBRATED.inset();
        drawHull(graphics, pose, hull.halfAcross() - inset, hull.halfAlong() - inset, INSET_COLOR, null, DASHED);
        for (BoatHull.Cell cell : hull.offendingCells(sailing.sea(), pose, HullRule.CALIBRATED))
        {
            int x = cell.cellX() * CELL;
            int y = cell.cellY() * CELL;
            fillQuad(graphics, x, y, x + CELL, y, x + CELL, y + CELL, x, y + CELL, OFFENDING_FILL);
        }
    }

    /**
     * Draws the deck's walkable tiles where they lie on the boat
     * @param graphics the graphics to draw on
     */
    public static void renderPlayerBoatDeck(Graphics2D graphics)
    {
        renderPlayerBoatDeck(graphics, DECK_COLOR, DECK_FILL, STROKE);
    }

    /**
     * Draws the deck's walkable tiles where they lie on the boat, in the given colours
     * @param graphics the graphics to draw on
     * @param outlineColor the outline colour
     * @param fillColor the fill colour
     * @param stroke the outline stroke
     */
    public static void renderPlayerBoatDeck(Graphics2D graphics, Color outlineColor, Color fillColor, Stroke stroke)
    {
        Client client = Static.getClient();
        for (LocalPoint tile : BoatCollisionAPI.getPlayerBoatDeckTiles())
        {
            Polygon polygon = Perspective.getCanvasTilePoly(client, tile);
            if (polygon != null)
            {
                OverlayUtil.renderPolygon(graphics, polygon, outlineColor, fillColor, stroke);
            }
        }
    }

    /**
     * Draws the route being sailed, pose by pose, and its goal tile
     * @param graphics the graphics to draw on
     */
    public static void renderRoute(Graphics2D graphics)
    {
        SeaRoute route = Sailing.get().currentRoute();
        if (route == null || route.isEmpty())
        {
            return;
        }
        graphics.setColor(ROUTE_COLOR);
        graphics.setStroke(STROKE);
        BoatPose last = null;
        for (BoatPose pose : route.poses())
        {
            if (last != null)
            {
                drawSceneLine(graphics, last.fineX(), last.fineY(), pose.fineX(), pose.fineY());
            }
            last = pose;
        }
        WorldPoint goal = route.goal();
        if (goal != null)
        {
            int x = goal.getX() << FINE_SHIFT;
            int y = goal.getY() << FINE_SHIFT;
            fillQuad(graphics, x, y, x + 128, y, x + 128, y + 128, x, y + 128, GOAL_FILL);
        }
    }

    /**
     * Draws the route being sailed on the minimap as a solid line
     * @param graphics the graphics to draw on
     */
    public static void renderRouteOnMinimap(Graphics2D graphics)
    {
        SeaRoute route = Sailing.get().currentRoute();
        Client client = Static.getClient();
        Player player = client.getLocalPlayer();
        if (route == null || route.isEmpty() || player == null)
        {
            return;
        }
        Point centre = Perspective.localToMinimap(client, player.getLocalLocation());
        if (centre == null)
        {
            return;
        }

        WorldView top = client.getTopLevelWorldView();
        Shape clip = graphics.getClip();
        Stroke stroke = graphics.getStroke();
        graphics.clip(new Ellipse2D.Double(centre.getX() - MINIMAP_RADIUS, centre.getY() - MINIMAP_RADIUS, MINIMAP_RADIUS * 2, MINIMAP_RADIUS * 2));
        graphics.setColor(ROUTE_COLOR);
        graphics.setStroke(STROKE);
        Point last = null;
        for (BoatPose pose : route.poses())
        {
            LocalPoint local = new LocalPoint(pose.fineX() - (top.getBaseX() << FINE_SHIFT), pose.fineY() - (top.getBaseY() << FINE_SHIFT), top);
            Point point = Perspective.localToMinimap(client, local, MINIMAP_REACH);
            if (point != null && last != null)
            {
                graphics.drawLine(last.getX(), last.getY(), point.getX(), point.getY());
            }
            last = point;
        }
        graphics.setClip(clip);
        graphics.setStroke(stroke);
    }

    /**
     * Draws the hull over the next ticks if the boat holds its heading and throttle
     * @param graphics the graphics to draw on
     * @param ticks how many ticks ahead to draw
     */
    public static void renderPredictedTrack(Graphics2D graphics, int ticks)
    {
        Sailing sailing = Sailing.get();
        BoatPose pose = sailing.pose();
        BoatHull hull = sailing.hull();
        if (pose == null || hull == null)
        {
            return;
        }
        BoatKinematics.State state = new BoatKinematics.State(pose, sailing.speed());
        for (int tick = 0; tick < ticks; tick++)
        {
            state = BoatKinematics.step(state, pose.heading(), sailing.throttle(), sailing.profile());
            drawHull(graphics, state.pose(), hull.halfAcross(), hull.halfAlong(), TRACK_COLOR, null, THIN);
        }
    }

    /**
     * Draws the sea map round the boat: land in coast tiles, walk-blocked water and water close to land
     * @param graphics the graphics to draw on
     * @param radius how many tiles round the boat to draw
     */
    public static void renderSeaCells(Graphics2D graphics, int radius)
    {
        Sailing sailing = Sailing.get();
        BoatPose pose = sailing.pose();
        if (pose == null)
        {
            return;
        }
        SeaMap sea = sailing.sea();
        for (int x = pose.tileX() - radius; x <= pose.tileX() + radius; x++)
        {
            for (int y = pose.tileY() - radius; y <= pose.tileY() + radius; y++)
            {
                if (!sea.isWater(x, y))
                {
                    continue;
                }
                int cells = sea.cells(x, y);
                if (cells != SeaMap.ALL_CELLS)
                {
                    fillLandCells(graphics, x, y, cells);
                }
                if (sea.walkBlocked(x, y))
                {
                    fillTile(graphics, x, y, WALK_BLOCKED_FILL);
                }
                int clearance = sea.clearance(x, y);
                if (clearance < CLEARANCE_COLORS.length)
                {
                    fillTile(graphics, x, y, CLEARANCE_COLORS[clearance]);
                }
            }
        }
    }

    /**
     * Draws the nav graph's nodes and edges across the loaded scene, edges running off it cut at its edge
     * @param graphics the graphics to draw on
     */
    public static void renderSeaGraph(Graphics2D graphics)
    {
        SeaGraph graph = Sailing.get().graph();
        Client client = Static.getClient();
        WorldView top = client.getTopLevelWorldView();
        int plane = top.getPlane();
        int minX = top.getBaseX() << FINE_SHIFT;
        int minY = top.getBaseY() << FINE_SHIFT;
        int maxX = ((top.getBaseX() + top.getSizeX()) << FINE_SHIFT) - 1;
        int maxY = ((top.getBaseY() + top.getSizeY()) << FINE_SHIFT) - 1;

        graphics.setColor(GRAPH_COLOR);
        graphics.setStroke(THIN);
        graph.forEachEdge((from, to) ->
        {
            if (SeaGraph.plane(from) == plane && SeaGraph.plane(to) == plane)
            {
                drawClipped(graphics, centre(SeaGraph.x(from)), centre(SeaGraph.y(from)), centre(SeaGraph.x(to)), centre(SeaGraph.y(to)), minX, minY, maxX, maxY);
            }
        });
        graph.forEachNode(node ->
        {
            Point point = SeaGraph.plane(node) == plane ? tileCentre(SeaGraph.x(node), SeaGraph.y(node)) : null;
            if (point != null)
            {
                graphics.fillRect(point.getX() - NODE_SIZE, point.getY() - NODE_SIZE, NODE_SIZE * 2, NODE_SIZE * 2);
            }
        });
    }

    private static void drawHull(Graphics2D graphics, BoatPose pose, int halfAcross, int halfAlong, Color outline, Color fill, Stroke stroke)
    {
        double theta = pose.angle() * (2 * Math.PI / 2048);
        double alongX = -Math.sin(theta);
        double alongY = -Math.cos(theta);
        double acrossX = Math.cos(theta);
        double acrossY = -Math.sin(theta);
        int[] xs = new int[4];
        int[] ys = new int[4];
        int[][] signs = {{1, 1}, {1, -1}, {-1, -1}, {-1, 1}};
        for (int corner = 0; corner < 4; corner++)
        {
            double fineX = pose.fineX() + signs[corner][0] * halfAlong * alongX + signs[corner][1] * halfAcross * acrossX;
            double fineY = pose.fineY() + signs[corner][0] * halfAlong * alongY + signs[corner][1] * halfAcross * acrossY;
            Point point = canvas((int) Math.round(fineX), (int) Math.round(fineY));
            if (point == null)
            {
                return;
            }
            xs[corner] = point.getX();
            ys[corner] = point.getY();
        }
        Polygon polygon = new Polygon(xs, ys, 4);
        if (fill != null)
        {
            graphics.setColor(fill);
            graphics.fill(polygon);
        }
        graphics.setColor(outline);
        graphics.setStroke(stroke);
        graphics.draw(polygon);
    }

    private static void fillLandCells(Graphics2D graphics, int tileX, int tileY, int cells)
    {
        for (int row = 0; row < SeaMap.CELLS; row++)
        {
            for (int column = 0; column < SeaMap.CELLS; column++)
            {
                if ((cells & 1 << (row * SeaMap.CELLS + column)) == 0)
                {
                    int x = (tileX << FINE_SHIFT) + column * CELL;
                    int y = (tileY << FINE_SHIFT) + row * CELL;
                    fillQuad(graphics, x, y, x + CELL, y, x + CELL, y + CELL, x, y + CELL, LAND_FILL);
                }
            }
        }
    }

    private static void fillTile(Graphics2D graphics, int tileX, int tileY, Color fill)
    {
        int x = tileX << FINE_SHIFT;
        int y = tileY << FINE_SHIFT;
        fillQuad(graphics, x, y, x + 128, y, x + 128, y + 128, x, y + 128, fill);
    }

    private static void fillQuad(Graphics2D graphics, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4, Color fill)
    {
        Point a = canvas(x1, y1);
        Point b = canvas(x2, y2);
        Point c = canvas(x3, y3);
        Point d = canvas(x4, y4);
        if (a == null || b == null || c == null || d == null)
        {
            return;
        }
        graphics.setColor(fill);
        graphics.fill(new Polygon(new int[]{a.getX(), b.getX(), c.getX(), d.getX()}, new int[]{a.getY(), b.getY(), c.getY(), d.getY()}, 4));
    }

    private static int centre(int tile)
    {
        return (tile << FINE_SHIFT) + 64;
    }

    private static void drawClipped(Graphics2D graphics, double x0, double y0, double x1, double y1, int minX, int minY, int maxX, int maxY)
    {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x0 - minX, maxX - x0, y0 - minY, maxY - y0};
        double enter = 0;
        double exit = 1;
        for (int side = 0; side < 4; side++)
        {
            if (p[side] == 0)
            {
                if (q[side] < 0)
                {
                    return;
                }
            }
            else
            {
                double t = q[side] / p[side];
                if (p[side] < 0)
                {
                    enter = Math.max(enter, t);
                }
                else
                {
                    exit = Math.min(exit, t);
                }
            }
        }
        if (enter > exit)
        {
            return;
        }

        drawSceneLine(graphics, (int) Math.round(x0 + enter * dx), (int) Math.round(y0 + enter * dy), (int) Math.round(x0 + exit * dx), (int) Math.round(y0 + exit * dy));
    }

    private static void drawSceneLine(Graphics2D graphics, int fineX0, int fineY0, int fineX1, int fineY1)
    {
        Client client = Static.getClient();
        float[] from = toCamera(client, fineX0, fineY0);
        float[] to = toCamera(client, fineX1, fineY1);
        if (from == null || to == null || (from[2] < NEAR && to[2] < NEAR))
        {
            return;
        }
        if (from[2] < NEAR)
        {
            from = toNear(from, to);
        }
        else if (to[2] < NEAR)
        {
            to = toNear(to, from);
        }

        float centreX = client.getViewportWidth() / 2f + client.getViewportXOffset();
        float centreY = client.getViewportHeight() / 2f + client.getViewportYOffset();
        int scale = client.getScale();
        graphics.drawLine(Math.round(centreX + from[0] * scale / from[2]), Math.round(centreY + from[1] * scale / from[2]), Math.round(centreX + to[0] * scale / to[2]), Math.round(centreY + to[1] * scale / to[2]));
    }

    private static float[] toCamera(Client client, int fineX, int fineY)
    {
        WorldView top = client.getTopLevelWorldView();
        LocalPoint local = new LocalPoint(fineX - (top.getBaseX() << FINE_SHIFT), fineY - (top.getBaseY() << FINE_SHIFT), top);
        if (!local.isInScene())
        {
            return null;
        }

        boolean gpu = client.isGpu();
        float x = local.getX() - (gpu ? client.getCameraFpX() : client.getCameraX());
        float y = local.getY() - (gpu ? client.getCameraFpY() : client.getCameraY());
        float z = Perspective.getTileHeight(client, local, top.getPlane()) - (gpu ? client.getCameraFpZ() : client.getCameraZ());
        float pitch = gpu ? client.getCameraFpPitch() : client.getCameraPitch() * ANGLE_TO_RADIANS;
        float yaw = gpu ? client.getCameraFpYaw() : client.getCameraYaw() * ANGLE_TO_RADIANS;
        float pitchSin = (float) Math.sin(pitch);
        float pitchCos = (float) Math.cos(pitch);
        float yawSin = (float) Math.sin(yaw);
        float yawCos = (float) Math.cos(yaw);

        float x1 = x * yawCos + y * yawSin;
        float y1 = y * yawCos - x * yawSin;
        return new float[]{x1, z * pitchCos - y1 * pitchSin, y1 * pitchCos + z * pitchSin};
    }

    private static float[] toNear(float[] behind, float[] ahead)
    {
        float t = (NEAR - behind[2]) / (ahead[2] - behind[2]);
        return new float[]{behind[0] + t * (ahead[0] - behind[0]), behind[1] + t * (ahead[1] - behind[1]), NEAR};
    }

    private static Point tileCentre(int tileX, int tileY)
    {
        return canvas((tileX << FINE_SHIFT) + 64, (tileY << FINE_SHIFT) + 64);
    }

    private static Point canvas(int fineX, int fineY)
    {
        Client client = Static.getClient();
        WorldView top = client.getTopLevelWorldView();
        LocalPoint local = new LocalPoint(fineX - (top.getBaseX() << FINE_SHIFT), fineY - (top.getBaseY() << FINE_SHIFT), top);
        if (!local.isInScene())
        {
            return null;
        }
        return Perspective.localToCanvas(client, local, top.getPlane());
    }
}
