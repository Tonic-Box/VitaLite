package com.tonic.services.mouserecorder.markov.ui;

import com.tonic.services.mouserecorder.markov.MarkovChainData;
import com.tonic.services.mouserecorder.markov.MarkovMouseTrainer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Visualization of the Markov chain state space.
 * Shows distribution of movement states (delta-X vs delta-Y).
 */
public class StateSpacePanel extends JPanel
{
    private static final Color BG_COLOR = new Color(30, 31, 34);
    private static final Color GRID_COLOR = new Color(60, 62, 66);
    private static final Color AXIS_COLOR = new Color(100, 100, 100);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);
    private static final Color STATE_COLOR_LOW = new Color(50, 150, 255, 180);
    private static final Color STATE_COLOR_HIGH = new Color(255, 80, 80, 255);

    private final MarkovMouseTrainer trainer;
    private Map<Point, Long> stateFrequencies;
    private long maxFrequency;
    private long lastUpdateCount = 0;

    public StateSpacePanel(MarkovMouseTrainer trainer)
    {
        this.trainer = trainer;
        this.stateFrequencies = new HashMap<>();
        setBackground(BG_COLOR);
        setPreferredSize(new Dimension(600, 400));
        updateStateData();
    }

    /**
     * Updates state data if the chain has changed.
     */
    public void updateIfNeeded()
    {
        MarkovChainData chainData = trainer.getChainData();
        long currentCount = chainData.getTotalTransitions();

        if (currentCount != lastUpdateCount || (stateFrequencies.isEmpty() && currentCount > 0))
        {
            updateStateData();
            lastUpdateCount = currentCount;
            repaint();
        }
    }

    private void updateStateData()
    {
        MarkovChainData chainData = trainer.getChainData();

        stateFrequencies = chainData.getStateFrequenciesByPosition();

        maxFrequency = stateFrequencies.values().stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2d.drawString("State Space Distribution (Delta-X vs Delta-Y)", 20, 30);

        if (stateFrequencies.isEmpty())
        {
            drawNoDataMessage(g2d, width, height);
            return;
        }

        int padding = 60;
        int plotWidth = width - 2 * padding;
        int plotHeight = height - 2 * padding;

        int minX = stateFrequencies.keySet().stream().mapToInt(p -> p.x).min().orElse(-10);
        int maxX = stateFrequencies.keySet().stream().mapToInt(p -> p.x).max().orElse(10);
        int minY = stateFrequencies.keySet().stream().mapToInt(p -> p.y).min().orElse(-10);
        int maxY = stateFrequencies.keySet().stream().mapToInt(p -> p.y).max().orElse(10);

        int rangeX = maxX - minX;
        int rangeY = maxY - minY;

        if (rangeX == 0) rangeX = 1;
        if (rangeY == 0) rangeY = 1;

        drawAxes(g2d, padding, width, height, plotWidth, plotHeight, minX, maxX, minY, maxY, rangeX, rangeY);

        for (Map.Entry<Point, Long> entry : stateFrequencies.entrySet())
        {
            Point state = entry.getKey();
            long frequency = entry.getValue();

            int screenX = padding + (int) (((state.x - minX) / (float) rangeX) * plotWidth);
            int screenY = height - padding - (int) (((state.y - minY) / (float) rangeY) * plotHeight);

            Color color = interpolateColor(frequency);

            int size = (int) (6 + Math.log(frequency + 1) * 4);
            size = Math.min(size, 25);

            g2d.setColor(color);
            Ellipse2D.Double circle = new Ellipse2D.Double(screenX - size / 2.0, screenY - size / 2.0, size, size);
            g2d.fill(circle);

            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.setStroke(new BasicStroke(1));
            g2d.draw(circle);
        }

        drawLegend(g2d, width, height);
        drawStats(g2d);
    }

    private void drawAxes(Graphics2D g2d, int padding, int width, int height,
                          int plotWidth, int plotHeight,
                          int minX, int maxX, int minY, int maxY,
                          int rangeX, int rangeY)
    {
        int zeroX = padding + (int) (((-minX) / (float) rangeX) * plotWidth);
        int zeroY = height - padding - (int) (((-minY) / (float) rangeY) * plotHeight);

        g2d.setColor(AXIS_COLOR);
        g2d.setStroke(new BasicStroke(2));

        if (zeroY >= padding && zeroY <= height - padding)
        {
            g2d.drawLine(padding, zeroY, width - padding, zeroY);
        }

        if (zeroX >= padding && zeroX <= width - padding)
        {
            g2d.drawLine(zeroX, padding, zeroX, height - padding);
        }

        g2d.setColor(GRID_COLOR);
        g2d.drawRect(padding, padding, plotWidth, plotHeight);

        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        String xLabel = "Delta-X (bins)";
        g2d.drawString(xLabel, width / 2 - 30, height - 10);

        String yLabel = "Delta-Y (bins)";
        g2d.rotate(-Math.PI / 2);
        g2d.drawString(yLabel, -height / 2 - 30, 20);
        g2d.rotate(Math.PI / 2);

        g2d.drawString(String.valueOf(minX), padding, height - padding + 15);
        g2d.drawString(String.valueOf(maxX), width - padding - 20, height - padding + 15);
        g2d.drawString(String.valueOf(minY), padding - 25, height - padding);
        g2d.drawString(String.valueOf(maxY), padding - 25, padding + 5);
    }

    private void drawLegend(Graphics2D g2d, int width, int height)
    {
        int padding = 60;
        int legendWidth = 20;
        int legendHeight = 120;

        int legendX = width - padding + 15;
        int legendY = (height - legendHeight) / 2;

        for (int i = 0; i < legendHeight; i++)
        {
            float ratio = i / (float) legendHeight;
            long freq = (long) (maxFrequency * (1.0 - ratio));
            Color color = interpolateColor(freq);
            g2d.setColor(color);
            g2d.drawLine(legendX, legendY + i, legendX + legendWidth, legendY + i);
        }

        g2d.setColor(GRID_COLOR);
        g2d.drawRect(legendX, legendY, legendWidth, legendHeight);

        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2d.drawString("High", legendX + legendWidth + 5, legendY + 12);
        g2d.drawString("Freq", legendX + legendWidth + 5, legendY + 24);
        g2d.drawString("Low", legendX + legendWidth + 5, legendY + legendHeight - 5);
    }

    private void drawStats(Graphics2D g2d)
    {
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        String stats = String.format("Unique states: %d | Max frequency: %d",
            stateFrequencies.size(), maxFrequency);
        g2d.drawString(stats, 20, getHeight() - 20);
    }

    private Color interpolateColor(long frequency)
    {
        if (maxFrequency == 0) return STATE_COLOR_LOW;

        float ratio = (float) frequency / maxFrequency;
        ratio = (float) Math.pow(ratio, 0.5);

        int r = (int) (STATE_COLOR_LOW.getRed() + ratio * (STATE_COLOR_HIGH.getRed() - STATE_COLOR_LOW.getRed()));
        int g = (int) (STATE_COLOR_LOW.getGreen() + ratio * (STATE_COLOR_HIGH.getGreen() - STATE_COLOR_LOW.getGreen()));
        int b = (int) (STATE_COLOR_LOW.getBlue() + ratio * (STATE_COLOR_HIGH.getBlue() - STATE_COLOR_LOW.getBlue()));
        int a = (int) (STATE_COLOR_LOW.getAlpha() + ratio * (STATE_COLOR_HIGH.getAlpha() - STATE_COLOR_LOW.getAlpha()));

        return new Color(r, g, b, a);
    }

    private void drawNoDataMessage(Graphics2D g2d, int width, int height)
    {
        g2d.setColor(new Color(150, 150, 150));
        g2d.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        String message = "No state data available - Record some movements to see distribution";
        FontMetrics fm = g2d.getFontMetrics();
        int msgWidth = fm.stringWidth(message);
        g2d.drawString(message, (width - msgWidth) / 2, height / 2);
    }
}
