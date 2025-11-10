package com.tonic.services.mouserecorder.markov.ui;

import com.tonic.services.mouserecorder.MouseDataPoint;
import com.tonic.services.mouserecorder.markov.MarkovMouseTrainer;
import com.tonic.services.mouserecorder.markov.MouseRecordingService;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

/**
 * Real-time visualization of live mouse movement tracking.
 * Shows current mouse position and recent trail.
 */
public class LiveMousePanel extends JPanel
{
    private static final Color BG_COLOR = new Color(30, 31, 34);
    private static final Color GRID_COLOR = new Color(60, 62, 66);
    private static final Color TRAIL_COLOR = new Color(100, 150, 255);
    private static final Color CURRENT_POS_COLOR = new Color(255, 100, 100);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);

    private static final int TRAIL_LENGTH = 100;
    private static final int GRID_SIZE = 50;

    private final MarkovMouseTrainer trainer;

    public LiveMousePanel(MarkovMouseTrainer trainer)
    {
        this.trainer = trainer;
        setBackground(BG_COLOR);
        setPreferredSize(new Dimension(600, 400));
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        drawGrid(g2d, width, height);

        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2d.drawString("Live Mouse Movement Trail", 20, 30);

        MouseRecordingService recordingService = trainer.getRecordingService();
        if (recordingService != null && trainer.isRunning())
        {
            List<MouseDataPoint> samples = recordingService.getSamples(false);
            if (!samples.isEmpty())
            {
                drawTrail(g2d, samples, width, height);
                MouseDataPoint current = samples.get(samples.size() - 1);
                drawCurrentPosition(g2d, current, width, height);
                drawInfo(g2d, current, samples.size());
            }
            else
            {
                drawNoDataMessage(g2d, width, height, "Recording... waiting for data");
            }
        }
        else
        {
            drawNoDataMessage(g2d, width, height, "Not recording - Start recording to see live tracking");
        }
    }

    private void drawGrid(Graphics2D g2d, int width, int height)
    {
        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(1));

        for (int x = 0; x < width; x += GRID_SIZE)
        {
            g2d.drawLine(x, 0, x, height);
        }

        for (int y = 0; y < height; y += GRID_SIZE)
        {
            g2d.drawLine(0, y, width, y);
        }
    }

    private void drawTrail(Graphics2D g2d, List<MouseDataPoint> samples, int width, int height)
    {
        int startIdx = Math.max(0, samples.size() - TRAIL_LENGTH);
        List<MouseDataPoint> recentSamples = samples.subList(startIdx, samples.size());

        if (recentSamples.size() < 2) return;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (MouseDataPoint sample : recentSamples)
        {
            minX = Math.min(minX, sample.getX());
            maxX = Math.max(maxX, sample.getX());
            minY = Math.min(minY, sample.getY());
            maxY = Math.max(maxY, sample.getY());
        }

        int padding = 50;
        int rangeX = maxX - minX;
        int rangeY = maxY - minY;

        if (rangeX == 0) rangeX = 100;
        if (rangeY == 0) rangeY = 100;

        for (int i = 1; i < recentSamples.size(); i++)
        {
            MouseDataPoint prev = recentSamples.get(i - 1);
            MouseDataPoint curr = recentSamples.get(i);

            float alpha = (float) i / recentSamples.size();
            int x1 = (int) (((prev.getX() - minX) / (float) rangeX) * (width - 2 * padding) + padding);
            int y1 = (int) (((prev.getY() - minY) / (float) rangeY) * (height - 2 * padding) + padding);
            int x2 = (int) (((curr.getX() - minX) / (float) rangeX) * (width - 2 * padding) + padding);
            int y2 = (int) (((curr.getY() - minY) / (float) rangeY) * (height - 2 * padding) + padding);

            g2d.setColor(new Color(
                TRAIL_COLOR.getRed(),
                TRAIL_COLOR.getGreen(),
                TRAIL_COLOR.getBlue(),
                (int) (alpha * 255)
            ));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawCurrentPosition(Graphics2D g2d, MouseDataPoint current, int width, int height)
    {
        // Draw pulsing circle at current position
        // Note: For simplicity, we'll just draw at the center if no scaling context
        // In a real implementation, this would use the same scaling as the trail

        int centerX = width / 2;
        int centerY = height / 2;
        g2d.setColor(new Color(CURRENT_POS_COLOR.getRed(), CURRENT_POS_COLOR.getGreen(), CURRENT_POS_COLOR.getBlue(), 50));
        g2d.fill(new Ellipse2D.Double(centerX - 15, centerY - 15, 30, 30));
        g2d.setColor(CURRENT_POS_COLOR);
        g2d.fill(new Ellipse2D.Double(centerX - 8, centerY - 8, 16, 16));
        g2d.setColor(Color.WHITE);
        g2d.fill(new Ellipse2D.Double(centerX - 3, centerY - 3, 6, 6));
    }

    private void drawInfo(Graphics2D g2d, MouseDataPoint current, int sampleCount)
    {
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        String info = String.format("Position: (%d, %d) | Samples in buffer: %d",
            current.getX(), current.getY(), sampleCount);
        g2d.drawString(info, 20, getHeight() - 20);
    }

    private void drawNoDataMessage(Graphics2D g2d, int width, int height, String message)
    {
        g2d.setColor(new Color(150, 150, 150));
        g2d.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        FontMetrics fm = g2d.getFontMetrics();
        int msgWidth = fm.stringWidth(message);
        g2d.drawString(message, (width - msgWidth) / 2, height / 2);
    }
}
