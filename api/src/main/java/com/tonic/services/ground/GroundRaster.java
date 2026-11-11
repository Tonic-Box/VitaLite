package com.tonic.services.ground;

import com.tonic.api.TClient;
import net.runelite.api.BufferProvider;
import net.runelite.api.Client;
import net.runelite.api.Projection;
import net.runelite.api.WorldView;

import java.util.List;

/**
 * Draws ground triangles into the software renderer's frame: projected as the scene was, tested against the scene's depth without writing it, and blended once per pixel.
 */
final class GroundRaster
{
    private static final float NEAR = 80f;
    private static final int MAX_CORNERS = 4;

    private final float[] viewX = new float[MAX_CORNERS];
    private final float[] viewY = new float[MAX_CORNERS];
    private final float[] viewZ = new float[MAX_CORNERS];
    private final float[] screenX = new float[MAX_CORNERS];
    private final float[] screenY = new float[MAX_CORNERS];
    private final float[] depths = new float[MAX_CORNERS];
    private final float[] clippedX = new float[MAX_CORNERS];
    private final float[] clippedY = new float[MAX_CORNERS];
    private final float[] clippedZ = new float[MAX_CORNERS];
    private final float[] view = new float[3];

    private int[] pixels;
    private float[] depth;
    private int width;
    private int clipLeft;
    private int clipTop;
    private int clipRight;
    private int clipBottom;
    private float centreX;
    private float centreY;
    private float scale;
    private TClient tclient;

    void draw(Client client, List<GroundLayer> layers)
    {
        BufferProvider buffer = client.getBufferProvider();
        float[] sceneDepth = ((TClient) client).getRasterDepth();
        if (buffer == null || sceneDepth == null)
        {
            return;
        }

        pixels = buffer.getPixels();
        depth = sceneDepth;
        width = buffer.getWidth();
        int height = buffer.getHeight();
        if (pixels == null || depth.length < width * height || pixels.length < width * height)
        {
            return;
        }

        clipLeft = Math.max(0, client.getViewportXOffset());
        clipTop = Math.max(0, client.getViewportYOffset());
        clipRight = Math.min(width, client.getViewportXOffset() + client.getViewportWidth());
        clipBottom = Math.min(height, client.getViewportYOffset() + client.getViewportHeight());
        centreX = client.getViewportXOffset() + client.getViewportWidth() / 2f;
        centreY = client.getViewportYOffset() + client.getViewportHeight() / 2f;
        scale = client.getScale();
        tclient = (TClient) client;

        for (GroundLayer layer : layers)
        {
            for (GroundTriangles triangles : layer.views())
            {
                if (triangles.count() == 0)
                {
                    continue;
                }
                WorldView worldView = client.getWorldView(triangles.worldViewId());
                Projection projection = worldView == null ? null : worldView.getCanvasProjection();
                if (projection != null)
                {
                    drawTriangles(triangles, projection);
                }
            }
        }
        pixels = null;
        depth = null;
        tclient = null;
    }

    private void drawTriangles(GroundTriangles triangles, Projection projection)
    {
        for (int block = 0; block < triangles.blockCount(); block++)
        {
            if (offscreen(triangles, block, projection))
            {
                continue;
            }
            int end = Math.min(triangles.count(), (block + 1) * GroundTriangles.BLOCK);
            for (int triangle = block * GroundTriangles.BLOCK; triangle < end; triangle++)
            {
                drawTriangle(triangles, triangle, projection);
            }
        }
    }

    private boolean offscreen(GroundTriangles triangles, int block, Projection projection)
    {
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        int behind = 0;
        for (int corner = 0; corner < 8; corner++)
        {
            float x = triangles.bound(block, (corner & 1) == 0 ? GroundTriangles.MIN_X : GroundTriangles.MAX_X);
            float height = triangles.bound(block, (corner & 2) == 0 ? GroundTriangles.MIN_HEIGHT : GroundTriangles.MAX_HEIGHT);
            float y = triangles.bound(block, (corner & 4) == 0 ? GroundTriangles.MIN_Y : GroundTriangles.MAX_Y);
            projection.project(x, height, y, view);
            if (view[2] < NEAR)
            {
                behind++;
                continue;
            }
            float screenX = centreX + view[0] * scale / view[2];
            float screenY = centreY + view[1] * scale / view[2];
            minX = Math.min(minX, screenX);
            maxX = Math.max(maxX, screenX);
            minY = Math.min(minY, screenY);
            maxY = Math.max(maxY, screenY);
        }
        if (behind == 8)
        {
            return true;
        }
        if (behind > 0)
        {
            return false;
        }
        return maxX < clipLeft || minX >= clipRight || maxY < clipTop || minY >= clipBottom;
    }

    private void drawTriangle(GroundTriangles triangles, int triangle, Projection projection)
    {
        int argb = triangles.colour(triangle);
        if ((argb >>> 24) == 0)
        {
            return;
        }

        for (int corner = 0; corner < 3; corner++)
        {
            projection.project(triangles.x(triangle, corner), triangles.height(triangle, corner), triangles.y(triangle, corner), view);
            viewX[corner] = view[0];
            viewY[corner] = view[1];
            viewZ[corner] = view[2];
        }

        int count = clipNear();
        if (count < 3)
        {
            return;
        }
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int corner = 0; corner < count; corner++)
        {
            screenX[corner] = centreX + clippedX[corner] * scale / clippedZ[corner];
            screenY[corner] = centreY + clippedY[corner] * scale / clippedZ[corner];
            minX = Math.min(minX, screenX[corner]);
            maxX = Math.max(maxX, screenX[corner]);
            minY = Math.min(minY, screenY[corner]);
            maxY = Math.max(maxY, screenY[corner]);
        }
        if (maxX < clipLeft || minX >= clipRight || maxY < clipTop || minY >= clipBottom)
        {
            return;
        }
        for (int corner = 0; corner < count; corner++)
        {
            depths[corner] = tclient.depthOf(clippedZ[corner]);
        }
        for (int corner = 1; corner + 1 < count; corner++)
        {
            fill(0, corner, corner + 1, argb);
        }
    }

    private int clipNear()
    {
        int kept = 0;
        for (int corner = 0; corner < 3; corner++)
        {
            int next = (corner + 1) % 3;
            boolean inside = viewZ[corner] >= NEAR;
            boolean nextInside = viewZ[next] >= NEAR;
            if (inside)
            {
                clippedX[kept] = viewX[corner];
                clippedY[kept] = viewY[corner];
                clippedZ[kept++] = viewZ[corner];
            }
            if (inside != nextInside)
            {
                float t = (NEAR - viewZ[corner]) / (viewZ[next] - viewZ[corner]);
                clippedX[kept] = viewX[corner] + (viewX[next] - viewX[corner]) * t;
                clippedY[kept] = viewY[corner] + (viewY[next] - viewY[corner]) * t;
                clippedZ[kept++] = NEAR;
            }
        }
        return kept;
    }

    private void fill(int a, int b, int c, int argb)
    {
        float ax = screenX[a];
        float ay = screenY[a];
        float bx = screenX[b];
        float by = screenY[b];
        float cx = screenX[c];
        float cy = screenY[c];
        float area = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        if (area == 0 || Float.isNaN(area))
        {
            return;
        }
        if (area < 0)
        {
            float swapX = bx;
            float swapY = by;
            bx = cx;
            by = cy;
            cx = swapX;
            cy = swapY;
            int swap = b;
            b = c;
            c = swap;
            area = -area;
        }

        int minX = Math.max(clipLeft, (int) Math.floor(Math.min(ax, Math.min(bx, cx))));
        int maxX = Math.min(clipRight - 1, (int) Math.ceil(Math.max(ax, Math.max(bx, cx))));
        int minY = Math.max(clipTop, (int) Math.floor(Math.min(ay, Math.min(by, cy))));
        int maxY = Math.min(clipBottom - 1, (int) Math.ceil(Math.max(ay, Math.max(by, cy))));
        if (minX > maxX || minY > maxY)
        {
            return;
        }

        float da = depths[a];
        float db = depths[b];
        float dc = depths[c];
        int alpha = argb >>> 24;
        int inverse = 255 - alpha;
        int red = ((argb >> 16) & 0xFF) * alpha;
        int green = ((argb >> 8) & 0xFF) * alpha;
        int blue = (argb & 0xFF) * alpha;

        boolean ownsAB = owns(ax, ay, bx, by);
        boolean ownsBC = owns(bx, by, cx, cy);
        boolean ownsCA = owns(cx, cy, ax, ay);

        for (int y = minY; y <= maxY; y++)
        {
            float py = y + 0.5f;
            int row = y * width;
            for (int x = minX; x <= maxX; x++)
            {
                float px = x + 0.5f;
                float w0 = edge(bx, by, cx, cy, px, py);
                float w1 = edge(cx, cy, ax, ay, px, py);
                float w2 = edge(ax, ay, bx, by, px, py);
                if (!covers(w0, ownsBC) || !covers(w1, ownsCA) || !covers(w2, ownsAB))
                {
                    continue;
                }

                float z = (w0 * da + w1 * db + w2 * dc) / area;
                int index = row + x;
                if (depth[index] > z)
                {
                    continue;
                }

                int under = pixels[index];
                int r = (red + ((under >> 16) & 0xFF) * inverse) / 255;
                int g = (green + ((under >> 8) & 0xFF) * inverse) / 255;
                int bl = (blue + (under & 0xFF) * inverse) / 255;
                pixels[index] = (r << 16) | (g << 8) | bl;
            }
        }
    }

    private static float edge(float ax, float ay, float bx, float by, float px, float py)
    {
        return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
    }

    private static boolean covers(float weight, boolean owned)
    {
        return weight > 0 || (weight == 0 && owned);
    }

    private static boolean owns(float ax, float ay, float bx, float by)
    {
        float dx = bx - ax;
        float dy = by - ay;
        return dy < 0 || (dy == 0 && dx > 0);
    }
}
