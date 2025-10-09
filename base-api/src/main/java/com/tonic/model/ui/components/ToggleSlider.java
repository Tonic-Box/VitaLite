package com.tonic.model.ui.components;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ToggleSlider extends JPanel {
    private static final Color TOGGLE_OFF_BG = new Color(65, 65, 70);
    private static final Color TOGGLE_ON_BG = new Color(64, 169, 211);
    private static final Color TOGGLE_KNOB = new Color(245, 245, 250);
    private static final int TOGGLE_WIDTH = 44;
    private static final int TOGGLE_HEIGHT = 24;
    private float animationProgress = 0f;
    private Timer animator;
    @Getter
    private boolean selected = false;
    private List<ActionListener> actionListeners = new ArrayList<>();

    public ToggleSlider() {
        setPreferredSize(new Dimension(TOGGLE_WIDTH, TOGGLE_HEIGHT));
        setMinimumSize(new Dimension(TOGGLE_WIDTH, TOGGLE_HEIGHT));
        setMaximumSize(new Dimension(TOGGLE_WIDTH, TOGGLE_HEIGHT));
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setSelected(!selected);
                fireActionPerformed();
            }
        });
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        animateToggle();
    }

    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }

    private void fireActionPerformed() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "toggle");
        for (ActionListener listener : actionListeners) {
            listener.actionPerformed(event);
        }
    }

    private void animateToggle() {
        if (animator != null && animator.isRunning()) {
            animator.stop();
        }

        final float start = animationProgress;
        final float end = isSelected() ? 1f : 0f;

        final long startTime = System.currentTimeMillis();
        final int duration = 200;

        animator = new Timer(10, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1f, (float)elapsed / duration);

            animationProgress = start + (end - start) * progress;

            if (progress >= 1f) {
                animationProgress = end;
                ((Timer)e.getSource()).stop();
            }
            repaint();
        });

        animator.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color trackColor = mixColors(animationProgress);
        g2d.setColor(trackColor);
        g2d.fillRoundRect(0, 0, TOGGLE_WIDTH, TOGGLE_HEIGHT, TOGGLE_HEIGHT, TOGGLE_HEIGHT);
        int knobSize = TOGGLE_HEIGHT - 6;
        int knobX = (int)(3 + (TOGGLE_WIDTH - knobSize - 6) * animationProgress);
        int knobY = 3;
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(knobX + 1, knobY + 1, knobSize, knobSize);
        g2d.setColor(TOGGLE_KNOB);
        g2d.fillOval(knobX, knobY, knobSize, knobSize);
    }

    private Color mixColors(float ratio) {
        int r = (int)(ToggleSlider.TOGGLE_OFF_BG.getRed() * (1 - ratio) + ToggleSlider.TOGGLE_ON_BG.getRed() * ratio);
        int g = (int)(ToggleSlider.TOGGLE_OFF_BG.getGreen() * (1 - ratio) + ToggleSlider.TOGGLE_ON_BG.getGreen() * ratio);
        int b = (int)(ToggleSlider.TOGGLE_OFF_BG.getBlue() * (1 - ratio) + ToggleSlider.TOGGLE_ON_BG.getBlue() * ratio);
        return new Color(r, g, b);
    }
}
