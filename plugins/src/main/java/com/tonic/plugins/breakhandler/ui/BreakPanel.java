package com.tonic.plugins.breakhandler.ui;

import com.tonic.plugins.breakhandler.Break;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public class BreakPanel extends JPanel
{
    private Break scheduledBreak;
    private final JLabel pluginLabel;
    private final JLabel countdownLabel;
    private final Timer updateTimer;

    public BreakPanel(Break scheduledBreak)
    {
        this.scheduledBreak = scheduledBreak;

        setLayout(new BorderLayout(5, 0));
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        setBackground(new Color(40, 40, 40));
        setPreferredSize(new Dimension(200, 28));

        pluginLabel = new JLabel(scheduledBreak.getPluginName());
        pluginLabel.setForeground(Color.WHITE);
        pluginLabel.setFont(pluginLabel.getFont().deriveFont(Font.BOLD, 12f));

        countdownLabel = new JLabel();
        countdownLabel.setForeground(Color.LIGHT_GRAY);
        countdownLabel.setFont(countdownLabel.getFont().deriveFont(Font.PLAIN, 11f));
        countdownLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        add(pluginLabel, BorderLayout.WEST);
        add(countdownLabel, BorderLayout.EAST);

        updateTimer = new Timer(1000, e -> updateCountdown());
        updateTimer.setInitialDelay(0);
        updateTimer.start();
    }

    private void updateCountdown()
    {
        if (scheduledBreak == null)
            return;

        final Instant now = Instant.now();
        final Instant startTime = scheduledBreak.getStartTime();

        if (scheduledBreak.isStarted())
        {
            Instant endTime = startTime.plus(scheduledBreak.getDuration());
            long remaining = Duration.between(now, endTime).getSeconds();

            if (remaining <= 0)
            {
                countdownLabel.setText("Break over");
                setBackground(new Color(40, 40, 40));
            }
            else
            {
                countdownLabel.setText("On break (" + formatDuration(Duration.ofSeconds(remaining)) + ")");
                setBackground(new Color(70, 50, 20));
            }
            return;
        }

        long secondsUntil = Duration.between(now, startTime).getSeconds();
        if (secondsUntil <= 0)
        {
            countdownLabel.setText("Waiting for plugin");
            setBackground(new Color(50, 45, 60));
        }
        else
        {
            countdownLabel.setText("in " + formatDuration(Duration.ofSeconds(secondsUntil)));
            setBackground(new Color(40, 40, 40));
        }
    }

    private String formatDuration(Duration d)
    {
        long total = Math.max(0, d.getSeconds());
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%d:%02d", m, s);
    }

    @Override
    public void removeNotify()
    {
        super.removeNotify();
        updateTimer.stop();
    }
}
