package com.tonic.services;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.services.ClickPacket.ClickPacket;
import com.tonic.services.ClickPacket.ClickType;
import com.tonic.services.mouserecorder.MouseDataPoint;
import com.tonic.services.mouserecorder.MouseMovementBuffer;
import com.tonic.services.mouserecorder.MouseRecorderAPI;
import com.tonic.services.mouserecorder.MovementVisualization;
import com.tonic.services.mouserecorder.trajectory.TrajectoryGenerator;
import com.tonic.services.mouserecorder.trajectory.TrajectoryService;
import lombok.Getter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages mouse click strategies and click packet generation.
 * Supports realistic mouse movement via trajectory-based generation.
 */
public class ClickManager
{
    @Getter
    private static final AtomicReference<Point> point = new AtomicReference<>(new Point(-1, -1));
    private static volatile Shape shape = null;
    private static final List<ClickPacket> clickPackets = new ArrayList<>();
    public static final Object LOCK = new Object();

    // Movement state tracking
    private static Point lastClickPosition = null;
    private static long lastClickTime = 0;
    private static final Random random = new Random();

    // Configuration
    private static final int MIN_DISTANCE_FOR_MOVEMENT = 15;
    private static final long MIN_TIME_FOR_MOVEMENT_MS = 150;
    private static boolean movementLogged = false;

    // Idle movement configuration
    private static final long IDLE_THRESHOLD_MS = 2000;
    private static final long IDLE_MOVEMENT_INTERVAL_MS = 800;
    private static final long MAX_IDLE_JITTER_DURATION_MS = 1500;
    private static final int IDLE_MOVEMENT_RADIUS = 8;
    private static long lastIdleMovementTime = 0;
    private static long idleJitterStartTime = 0;

    // Cached generator and API instances
    private static TrajectoryGenerator cachedGenerator = null;
    private static MouseRecorderAPI cachedAPI = null;
    private static long lastGeneratorRefresh = 0;
    private static final long GENERATOR_REFRESH_INTERVAL_MS = 30000;

    // Movement buffer (matches client's MouseRecorder behavior)
    private static final MouseMovementBuffer movementBuffer = MouseMovementBuffer.getInstance();

    /**
     * Sets the target point for static clicking.
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public static void setPoint(int x, int y)
    {
        point.set(new Point(x, y));
    }

    /**
     * Queues a shape for controlled clicking.
     * @param shape the shape to click within
     */
    public static void queueClickBox(Shape shape)
    {
        Static.invoke(() -> {
            if(shape == null)
            {
                ClickManager.shape = null;
                return;
            }
            ClickManager.shape = shape;
        });

    }

    /**
     * Clears the currently set click box.
     */
    public static void clearClickBox()
    {
        shape = null;
    }

    /**
     * Calculates Euclidean distance between two points.
     */
    private static double distance(Point p1, Point p2)
    {
        int dx = p2.x - p1.x;
        int dy = p2.y - p1.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Generates a random entry point on the edge of the viewport.
     * Simulates mouse entering the area from outside.
     */
    private static Point generateEntryPoint(Rectangle viewport)
    {
        int edge = random.nextInt(4); // 0=top, 1=right, 2=bottom, 3=left

        switch (edge)
        {
            case 0:
                return new Point(viewport.x + random.nextInt(viewport.width), viewport.y);
            case 1:
                return new Point(viewport.x + viewport.width - 1, viewport.y + random.nextInt(viewport.height));
            case 2:
                return new Point(viewport.x + random.nextInt(viewport.width), viewport.y + viewport.height - 1);
            case 3:
            default:
                return new Point(viewport.x, viewport.y + random.nextInt(viewport.height));
        }
    }

    /**
     * Checks if realistic movement should be used based on training quality.
     */
    private static boolean shouldUseRealisticMovement()
    {
        try
        {
            int trajectoryCount = TrajectoryService.getDatabase().getTrajectoryCount();
            return trajectoryCount >= 50;
        }
        catch (Exception e)
        {
            if (!movementLogged)
            {
                Logger.warn("Movement quality check failed, using teleport: " + e.getMessage());
                movementLogged = true;
            }
            return false;
        }
    }

    /**
     * Gets the cached generator and API, refreshing if necessary.
     * Generator is refreshed every 30 seconds to pick up new training data.
     * API is recreated when generator refreshes.
     *
     * @return Cached or newly created API
     * @throws IllegalStateException if generator cannot be created
     */
    private static MouseRecorderAPI getAPI() throws IllegalStateException
    {
        long now = System.currentTimeMillis();

        if (cachedGenerator == null || cachedAPI == null ||
            (now - lastGeneratorRefresh) > GENERATOR_REFRESH_INTERVAL_MS)
        {
            cachedGenerator = TrajectoryService.createGenerator();
            cachedAPI = new MouseRecorderAPI(cachedGenerator);
            lastGeneratorRefresh = now;
        }

        return cachedAPI;
    }

    /**
     * Generates realistic mouse movement samples and feeds them into the buffer.
     * The buffer will send accumulated samples when forceFlush() is called (before each click).
     *
     * Skips movement generation if:
     * - Distance too short (< 15px)
     * - Time since last click too short (< 150ms) - prevents inhumanly fast movements
     * - Training quality too low (< 50%)
     *
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param targetX Target X coordinate
     * @param targetY Target Y coordinate
     */
    private static void generateMovement(int startX, int startY, int targetX, int targetY)
    {
        try
        {
            long now = System.currentTimeMillis();
            if (lastClickTime > 0)
            {
                long timeSinceLastClick = now - lastClickTime;
                if (timeSinceLastClick < MIN_TIME_FOR_MOVEMENT_MS)
                {
                    return;
                }
            }

            double dist = distance(new Point(startX, startY), new Point(targetX, targetY));
            if (dist < MIN_DISTANCE_FOR_MOVEMENT || !shouldUseRealisticMovement())
            {
                return;
            }

            MouseRecorderAPI api = getAPI();

            // Generate movement path
            List<MouseDataPoint> samples = new ArrayList<>(api.getGenerator().generate(startX, startY, targetX, targetY).getPoints());
            if (!samples.isEmpty())
            {
                // Visualize the generated path immediately
                MovementVisualization.recordMovement(samples, MovementVisualization.MovementSource.TRAJECTORY_GENERATED);

                // Feed path to buffer for playback over time
                // The buffer's sampling thread will follow this path at 50ms intervals
                movementBuffer.playPath(samples);
            }
        }
        catch (Exception e)
        {
            if (!movementLogged)
            {
                Logger.warn("Movement generation failed: " + e.getMessage());
                movementLogged = true;
            }
        }
    }

    /**
     * Starts the movement buffer's background sampling.
     * Should be called when movement spoofing is enabled.
     */
    public static void startMovementSampling()
    {
        movementBuffer.start();
    }

    /**
     * Stops the movement buffer's background sampling.
     */
    public static void stopMovementSampling()
    {
        movementBuffer.stop();
    }

    public static List<ClickPacket> releaseClicks()
    {
        synchronized (LOCK)
        {
            var out = new ArrayList<>(clickPackets);
            clickPackets.clear();
            return out;
        }
    }

    /**
     * Sends a click packet using the current strategy.
     */
    public static void click()
    {
        click(ClickType.GENERIC);
    }

    /**
     * Sends a click packet using the current strategy and specified interaction type.
     * For RANDOM and CONTROLLED strategies, generates realistic mouse movement if training quality is sufficient.
     * @param packetInteractionType the type of interaction for the click packet
     */
    public static void click(ClickType packetInteractionType)
    {
        Static.invoke(() -> {
            TClient client = Static.getClient();
            int px = point.get().x;
            int py = point.get().y;
            ClickStrategy strategy = Static.getVitaConfig().getClickStrategy();
            switch (strategy)
            {
                case STATIC:
                    clearClickBox();
                    // Flush movement buffer before click (matches natural client behavior)
                    movementBuffer.forceFlush();
                    defaultStaticClickPacket(packetInteractionType, client, px, py);
                    break;
                case RANDOM:
                    clearClickBox();
                    Rectangle r = Static.getRuneLite().getGameApplet().getViewportArea();
                    if(r == null)
                    {
                        Logger.warn("Viewport area is null, defaulting to STATIC.");
                        defaultStaticClickPacket(packetInteractionType, client, px, py);
                        break;
                    }
                    int rx = (int) (Math.random() * r.getWidth()) + r.x;
                    int ry = (int) (Math.random() * r.getHeight()) + r.y;

                    int startX, startY;
                    if (lastClickPosition == null)
                    {
                        Point entryPoint = generateEntryPoint(r);
                        startX = entryPoint.x;
                        startY = entryPoint.y;
                    }
                    else
                    {
                        startX = lastClickPosition.x;
                        startY = lastClickPosition.y;
                    }

                    if(Static.getVitaConfig().shouldSpoofMouseMovemnt())
                    {
                        generateMovement(startX, startY, rx, ry);
                    }

                    // Flush movement buffer before click (matches natural client behavior)
                    movementBuffer.forceFlush();
                    client.getPacketWriter().clickPacket(0, rx, ry);
                    synchronized(LOCK)
                    {
                        clickPackets.add(new ClickPacket(packetInteractionType, rx, ry));
                    }

                    lastClickPosition = new Point(rx, ry);
                    lastClickTime = System.currentTimeMillis();
                    idleJitterStartTime = 0; // Reset jitter timer
                    break;

                case CONTROLLED:
                    if(shape == null)
                    {
                        Logger.warn("Click box is null, defaulting to STATIC.");
                        defaultStaticClickPacket(packetInteractionType, client, px, py);
                        break;
                    }

                    Point p = getRandomPointInShape(shape);

                    int cStartX, cStartY;
                    if (lastClickPosition == null)
                    {
                        Rectangle viewport = Static.getRuneLite().getGameApplet().getViewportArea();
                        if (viewport != null)
                        {
                            Point entryPoint = generateEntryPoint(viewport);
                            cStartX = entryPoint.x;
                            cStartY = entryPoint.y;
                        }
                        else
                        {
                            Rectangle shapeBounds = shape.getBounds();
                            Point entryPoint = generateEntryPoint(shapeBounds);
                            cStartX = entryPoint.x;
                            cStartY = entryPoint.y;
                        }
                    }
                    else
                    {
                        cStartX = lastClickPosition.x;
                        cStartY = lastClickPosition.y;
                    }

                    if(Static.getVitaConfig().shouldSpoofMouseMovemnt())
                    {
                        generateMovement(cStartX, cStartY, p.x, p.y);
                    }

                    // Flush movement buffer before click (matches natural client behavior)
                    movementBuffer.forceFlush();
                    client.getPacketWriter().clickPacket(0, p.x, p.y);
                    synchronized(LOCK)
                    {
                        clickPackets.add(new ClickPacket(packetInteractionType, p.x, p.y));
                    }

                    lastClickPosition = new Point(p.x, p.y);
                    lastClickTime = System.currentTimeMillis();
                    idleJitterStartTime = 0;
                    break;
            }
        });
    }

    private static Point getRandomPointInShape(Shape shape) {
        Rectangle bounds = shape.getBounds();

        while (true) {
            int x = (int) (Math.random() * bounds.width) + bounds.x;
            int y = (int) (Math.random() * bounds.height) + bounds.y;

            if (shape.contains(x, y)) {
                return new Point(x, y);
            }
        }
    }

    private static void defaultStaticClickPacket(ClickType packetInteractionType, TClient client, int x, int y) {
        client.getPacketWriter().clickPacket(0, x, y);
        clickPackets.add(new ClickPacket(packetInteractionType, x, y));
    }

    /**
     * INTERNAL USE: Called injected into OSRS MouseRecorder class. Don't remove.
     * Blocks manual movement recording when we have pending synthetic movements in buffer.
     * @return bool
     */
    public static boolean shouldBlockManualMovement()
    {
        return movementBuffer.getBufferSize() > 0;
    }
}
