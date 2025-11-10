package com.tonic.services;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.TPacketBufferNode;
import com.tonic.services.ClickPacket.ClickPacket;
import com.tonic.services.ClickPacket.PacketInteractionType;
import com.tonic.services.mouserecorder.EncodedMousePacket;
import com.tonic.services.mouserecorder.MouseRecorderAPI;
import com.tonic.services.mouserecorder.markov.MarkovMouseGenerator;
import com.tonic.services.mouserecorder.markov.MarkovService;
import lombok.Getter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages mouse click strategies and click packet generation.
 * Supports realistic mouse movement via Markov chain generation.
 */
public class ClickManager
{
    @Getter
    private static final AtomicReference<Point> point = new AtomicReference<>(new Point(-1, -1));
    private static volatile Shape shape = null;
    private static final List<ClickPacket> clickPackets = new ArrayList<>();

    // Movement state tracking
    private static Point lastClickPosition = null;
    private static long lastClickTime = 0;
    private static final Random random = new Random();

    // Configuration
    private static final int MIN_DISTANCE_FOR_MOVEMENT = 15;
    private static final long MIN_TIME_FOR_MOVEMENT_MS = 150;
    private static final double MIN_QUALITY_FOR_MOVEMENT = 0.50;
    private static boolean movementLogged = false;

    // Idle movement configuration
    private static final long IDLE_THRESHOLD_MS = 2000;
    private static final long IDLE_MOVEMENT_INTERVAL_MS = 800;
    private static final long MAX_IDLE_JITTER_DURATION_MS = 1500;
    private static final int IDLE_MOVEMENT_RADIUS = 8;
    private static long lastIdleMovementTime = 0;
    private static long idleJitterStartTime = 0;

    // Cached generator and API instances
    private static MarkovMouseGenerator cachedGenerator = null;
    private static MouseRecorderAPI cachedAPI = null;
    private static long lastGeneratorRefresh = 0;
    private static final long GENERATOR_REFRESH_INTERVAL_MS = 30000;

    // Asynchronous movement packet dispatcher
    private static final Queue<EncodedMousePacket> movementQueue = new ConcurrentLinkedQueue<>();
    private static ScheduledExecutorService movementDispatcher = null;
    private static final long BASE_DISPATCH_INTERVAL_MS = 50;
    private static final long DISPATCH_VARIANCE_MS = 25;
    private static long LAST_MOVE_MS = 0;

    static
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (movementDispatcher != null)
            {
                movementDispatcher.shutdown();
                try
                {
                    movementDispatcher.awaitTermination(1, TimeUnit.SECONDS);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }, "ClickManager-MovementDispatcher-Shutdown"));
    }

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
            double quality = MarkovService.getQualityScore();
            return quality >= MIN_QUALITY_FOR_MOVEMENT;
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
            cachedGenerator = MarkovService.getTrainer().createGenerator();
            cachedAPI = new MouseRecorderAPI(cachedGenerator);
            lastGeneratorRefresh = now;
        }

        return cachedAPI;
    }

    /**
     * Generates and returns realistic mouse movement data from start to target.
     * Returns null if movement generation fails or should be skipped.
     * Uses cached API (refreshed every 30s) and always builds packet immediately.
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
     * @return EncodedMousePacket containing movement data, or null if unavailable
     */
    private static EncodedMousePacket generateMovement(int startX, int startY, int targetX, int targetY)
    {
        try
        {
            long now = System.currentTimeMillis();
            if (lastClickTime > 0)
            {
                long timeSinceLastClick = now - lastClickTime;
                if (timeSinceLastClick < MIN_TIME_FOR_MOVEMENT_MS)
                {
                    return null;
                }
            }

            double dist = distance(new Point(startX, startY), new Point(targetX, targetY));
            if (dist < MIN_DISTANCE_FOR_MOVEMENT || !shouldUseRealisticMovement())
            {
                return null;
            }

            MouseRecorderAPI api = getAPI();
            api.recordMovement(startX, startY, targetX, targetY);
            EncodedMousePacket packet = api.buildPacket();
            return packet;
        }
        catch (Exception e)
        {
            if (!movementLogged)
            {
                Logger.warn("Movement generation failed, using teleport: " + e.getMessage());
                movementLogged = true;
            }
            return null;
        }
    }
    /**
     * Generates a small idle movement (jitter) from current position.
     * Used to simulate natural hand tremor and micro-adjustments when idle.
     */
    private static void generateIdleMovement()
    {
        if (!Static.getVitaConfig().shouldIdleJitter() || lastClickPosition == null)
        {
            return;
        }

        long now = System.currentTimeMillis();

        if (lastClickTime > 0 && (now - lastClickTime) < IDLE_THRESHOLD_MS)
        {
            idleJitterStartTime = 0;
            return;
        }

        if (idleJitterStartTime == 0)
        {
            idleJitterStartTime = now;
        }

        if ((now - idleJitterStartTime) >= MAX_IDLE_JITTER_DURATION_MS)
        {
            return;
        }

        if (lastIdleMovementTime > 0 && (now - lastIdleMovementTime) < IDLE_MOVEMENT_INTERVAL_MS)
        {
            return;
        }

        if (!shouldUseRealisticMovement())
        {
            return;
        }

        try
        {
            int offsetX = random.nextInt(IDLE_MOVEMENT_RADIUS * 2) - IDLE_MOVEMENT_RADIUS;
            int offsetY = random.nextInt(IDLE_MOVEMENT_RADIUS * 2) - IDLE_MOVEMENT_RADIUS;

            int targetX = lastClickPosition.x + offsetX;
            int targetY = lastClickPosition.y + offsetY;

            MouseRecorderAPI api = getAPI();

            api.recordMovement(lastClickPosition.x, lastClickPosition.y, targetX, targetY);

            EncodedMousePacket packet = api.buildPacket();
            if (packet != null)
            {
                movementQueue.offer(packet);
                lastClickPosition = new Point(targetX, targetY);
                lastIdleMovementTime = now;
            }
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Ensures the movement dispatcher is running.
     * Starts a background thread that sends queued movements every 50-75ms.
     * Also generates idle movements when no clicks are happening.
     * This decouples movement packet timing from click timing for more natural behavior.
     */
    private static synchronized void ensureDispatcherRunning()
    {
        if (movementDispatcher == null || movementDispatcher.isShutdown())
        {
            movementDispatcher = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ClickManager-MovementDispatcher");
                t.setDaemon(true);
                return t;
            });

            movementDispatcher.scheduleWithFixedDelay(() -> {
                try
                {
                    EncodedMousePacket packet = movementQueue.poll();
                    if (packet != null)
                    {
                        sendMouseMovementPacketImmediate(packet);
                    }
                    else
                    {
                        generateIdleMovement();
                    }
                }
                catch (Exception e)
                {
                    Logger.warn("Error dispatching movement packet: " + e.getMessage());
                }
            }, BASE_DISPATCH_INTERVAL_MS,
               BASE_DISPATCH_INTERVAL_MS + random.nextInt((int) DISPATCH_VARIANCE_MS),
               TimeUnit.MILLISECONDS);

            Logger.warn("Movement dispatcher started (sends packets every 50-75ms)");
        }
    }

    /**
     * Immediately sends a movement packet to the server.
     * Called by the background dispatcher thread.
     */
    private static void sendMouseMovementPacketImmediate(EncodedMousePacket movementPacket)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            TPacketBufferNode node = movementPacket.getBuffer().toPacketBufferNode(client);
            client.getPacketWriter().addNode(node);
        });
    }

    /**
     * Queues a movement packet for asynchronous dispatch.
     * Packet will be sent by background thread after 50-75ms delay.
     * This breaks the correlation between movement and click timing.
     */
    private static void queueMovementPacket(EncodedMousePacket movementPacket)
    {
        movementQueue.offer(movementPacket);
        ensureDispatcherRunning();
    }

    /**
     * Stops the movement dispatcher and clears the queue.
     * Useful for cleanup or testing purposes.
     */
    public static synchronized void stopMovementDispatcher()
    {
        if (movementDispatcher != null && !movementDispatcher.isShutdown())
        {
            movementDispatcher.shutdown();
            try
            {
                movementDispatcher.awaitTermination(1, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            movementQueue.clear();
            Logger.warn("Movement dispatcher stopped");
        }
    }

    /**
     * Gets the current number of queued movement packets.
     */
    public static int getQueuedMovementCount()
    {
        return movementQueue.size();
    }

    /**
     * Sends a click packet using the current strategy.
     */
    public static void click()
    {
        click(PacketInteractionType.UNBOUND_INTERACT);
    }

    /**
     * Sends a click packet using the current strategy and specified interaction type.
     * For RANDOM and CONTROLLED strategies, generates realistic mouse movement if training quality is sufficient.
     * @param packetInteractionType the type of interaction for the click packet
     */
    public static void click(PacketInteractionType packetInteractionType)
    {
        Static.invoke(() -> {
            TClient client = Static.getClient();
            int px = point.get().x;
            int py = point.get().y;
            ClickStrategy strategy = Static.getVitaConfig().getClickStrategy();
            switch (strategy)
            {
                case STATIC:
                    defaultStaticClickPacket(packetInteractionType, client, px, py);
                    break;
                case RANDOM:
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
                        EncodedMousePacket movementPacket = generateMovement(startX, startY, rx, ry);
                        if (movementPacket != null)
                        {
                            LAST_MOVE_MS = System.currentTimeMillis();
                            queueMovementPacket(movementPacket);
                        }
                    }

                    client.getPacketWriter().clickPacket(0, rx, ry);
                    clickPackets.add(new ClickPacket(packetInteractionType, rx, ry));

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
                        EncodedMousePacket cMovementPacket = generateMovement(cStartX, cStartY, p.x, p.y);
                        if (cMovementPacket != null)
                        {
                            LAST_MOVE_MS = System.currentTimeMillis();
                            queueMovementPacket(cMovementPacket);
                        }
                    }

                    client.getPacketWriter().clickPacket(0, p.x, p.y);
                    clickPackets.add(new ClickPacket(packetInteractionType, p.x, p.y));

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

    private static void defaultStaticClickPacket(PacketInteractionType packetInteractionType, TClient client, int x, int y) {
        client.getPacketWriter().clickPacket(0, x, y);
        clickPackets.add(new ClickPacket(packetInteractionType, x, y));
    }

    /**
     * INTERNAL USE: Call injected intor OSRS MouseRecorder class. Don't remove.
     * @return bool
     */
    public static boolean shouldBlockManualMovement()
    {
        return (System.currentTimeMillis() - LAST_MOVE_MS) < 2500;
    }
}
