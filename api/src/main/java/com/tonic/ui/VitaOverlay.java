package com.tonic.ui;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class VitaOverlay extends Overlay {

    @Getter @Setter
    private boolean hidden = false;

    @Getter @Setter
    private int width = 150;

    @Getter @Setter
    private int height = 100;

    @Getter
    private BufferedImage image;

    @Getter
    private String action = null;

    private Integer y;
    private boolean needsRedraw = true;
    private final List<DrawCommand> drawCommands = new ArrayList<>();
    private boolean mouseWasOver = false;

    // Colors matching your InfoBox
    private static final Color BACKGROUND_COLOR = new Color(43, 37, 31, 200);
    private static final Color INNER_BORDER_COLOR = new Color(90, 82, 69);
    private static final Color OUTER_BORDER_COLOR = new Color(56, 48, 35);
    private static final Color HOVER_BACKGROUND = new Color(33, 27, 21, 220);

    public VitaOverlay() {
        super();
        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(PRIORITY_MED);
        setResizable(false);
        setMovable(true);
        clear();
    }

    /**
     * Clear the info box and reset (matching InfoBox API)
     */
    public void clear() {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(BACKGROUND_COLOR);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        y = null;
        drawCommands.clear();
        needsRedraw = true;
    }

    /**
     * Check if mouse is over this overlay
     */
    public boolean mouseOver() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return false;
        }

        Point mouse = new Point(
                MouseInfo.getPointerInfo().getLocation().x,
                MouseInfo.getPointerInfo().getLocation().y
        );

        // Convert screen coordinates to game coordinates if needed
        return bounds.contains(mouse.getX(), mouse.getY());
    }

    /**
     * Adjust height dynamically
     */
    private void adjustHeight() {
        if (y != null && height < y - 6) {
            setHeight(y - 6);
        }
    }

    /**
     * Add a new line with specified size (matching InfoBox API)
     */
    public int newLine(String text, int size) {
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setFont(new Font("Serif", Font.PLAIN, size));
        FontMetrics metrics = g2d.getFontMetrics();
        int textHeight = metrics.getHeight();
        if (y == null) {
            y = 10 + (textHeight / 2);
        }

        drawCommands.add(new SingleTextCommand(text, new Font("Serif", Font.PLAIN, size), Color.WHITE, 5, y));
        int returnY = y;
        y += textHeight - 2;
        adjustHeight();
        needsRedraw = true;
        g2d.dispose();
        return returnY - 6;
    }

    /**
     * Add a new line with specified size and color (matching InfoBox API)
     */
    public int newLine(String text, int size, Color color) {
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setFont(new Font("Serif", Font.PLAIN, size));
        FontMetrics metrics = g2d.getFontMetrics();
        int textHeight = metrics.getHeight();
        if (y == null) {
            y = 10 + (textHeight / 2);
        }

        drawCommands.add(new SingleTextCommand(text, new Font("Serif", Font.PLAIN, size), color, 5, y));
        int returnY = y;
        y += textHeight - 2;
        adjustHeight();
        needsRedraw = true;
        g2d.dispose();
        return returnY - 6;
    }

    /**
     * Add a new line with left and right text (matching InfoBox API)
     */
    public int newLineEx(String leftText, String rightText, int size, Color leftColor, Color rightColor) {
        int x = 5;
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setFont(new Font("Serif", Font.BOLD, size));
        FontMetrics fm = g2d.getFontMetrics();
        int textHeight = fm.getHeight();
        if (y == null) {
            y = 10 + (textHeight / 2);
        }

        Font leftFont = new Font("Serif", Font.BOLD, size);
        Font rightFont = new Font("Serif", Font.PLAIN, size);
        drawCommands.add(new DualTextCommand(leftText, rightText, leftFont, rightFont,
                leftColor, rightColor, x, y));

        int returnY = y;
        y += textHeight - 2;
        adjustHeight();
        needsRedraw = true;
        g2d.dispose();
        return returnY - 6;
    }

    /**
     * Add a new line with left and right text with default colors (matching InfoBox API)
     */
    public int newLineEx(String leftText, String rightText, int size) {
        return newLineEx(leftText, rightText, size, Color.CYAN, Color.GREEN);
    }

    /**
     * Add a new line with custom font (matching InfoBox API)
     */
    public int newLine(String text, Font font) {
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics();
        int textHeight = metrics.getHeight();
        if (y == null) {
            y = 10 + (textHeight / 2);
        }

        drawCommands.add(new SingleTextCommand(text, font, Color.WHITE, 5, y));
        int returnY = y;
        y += textHeight - 2;
        adjustHeight();
        needsRedraw = true;
        g2d.dispose();
        return returnY - 6;
    }

    /**
     * Add a new line with custom font and color (matching InfoBox API)
     */
    public int newLine(String text, Font font, Color color) {
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics();
        int textHeight = metrics.getHeight();
        if (y == null) {
            y = 10 + (textHeight / 2);
        }

        drawCommands.add(new SingleTextCommand(text, font, color, 5, y));
        int returnY = y;
        y += textHeight - 2;
        adjustHeight();
        needsRedraw = true;
        g2d.dispose();
        return returnY - 6;
    }

    /**
     * Set action for mouse interaction
     */
    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Don't render if hidden or no content
        if (hidden || drawCommands.isEmpty()) {
            return null;
        }

        // Check for mouse hover
        boolean mouseIsOver = mouseOver();

        if (needsRedraw || mouseIsOver != mouseWasOver)
        {
            redrawImage(mouseIsOver);
            needsRedraw = false;
            mouseWasOver = mouseIsOver;
        }

        // Draw the image
        graphics.drawImage(image, 0, 0, null);

        // Draw borders (matching InfoBox embed method)
        drawBorders(graphics, 0, 0);

        return new Dimension(width, height);
    }

    private void redrawImage(boolean isHovered) {
        // Create new image if dimensions changed
        if (image.getWidth() != width || image.getHeight() != height) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Use darker background on hover if action is set
        if (isHovered && action != null && !action.isBlank()) {
            g2d.setColor(HOVER_BACKGROUND);
        } else {
            g2d.setColor(BACKGROUND_COLOR);
        }
        g2d.fillRect(0, 0, width, height);

        // Execute all draw commands
        for (DrawCommand cmd : drawCommands) {
            cmd.execute(g2d);
        }

        g2d.dispose();
    }

    private void drawBorders(Graphics2D graphics, int x, int y) {
        // Draw inner border
        graphics.setColor(INNER_BORDER_COLOR);
        graphics.drawRect(x - 1, y - 1, width + 1, height + 1);

        // Draw outer border
        graphics.setColor(OUTER_BORDER_COLOR);
        graphics.drawRect(x - 2, y - 2, width + 3, height + 3);
    }

    /**
     * Handle mouse click events
     */
    @Override
    public void onMouseOver() {
        if (action != null && !action.isBlank()) {
            // You can trigger custom actions here
            // For example, notify the plugin about the click
        }
    }

    // Draw command interface
    private interface DrawCommand {
        void execute(Graphics2D g2d);
    }

    // Single text command with position
    private static class SingleTextCommand implements DrawCommand {
        final String text;
        final Font font;
        final Color color;
        final int x;
        final int y;

        SingleTextCommand(String text, Font font, Color color, int x, int y) {
            this.text = text;
            this.font = font;
            this.color = color;
            this.x = x;
            this.y = y;
        }

        @Override
        public void execute(Graphics2D g2d) {
            Color oldColor = g2d.getColor();
            Font oldFont = g2d.getFont();

            g2d.setFont(font);
            g2d.setColor(color);
            g2d.drawString(text, x, y);

            g2d.setColor(oldColor);
            g2d.setFont(oldFont);
        }
    }

    // Dual text command with position
    private static class DualTextCommand implements DrawCommand {
        final String leftText;
        final String rightText;
        final Font leftFont;
        final Font rightFont;
        final Color leftColor;
        final Color rightColor;
        final int x;
        final int y;

        DualTextCommand(String leftText, String rightText, Font leftFont, Font rightFont,
                        Color leftColor, Color rightColor, int x, int y) {
            this.leftText = leftText;
            this.rightText = rightText;
            this.leftFont = leftFont;
            this.rightFont = rightFont;
            this.leftColor = leftColor;
            this.rightColor = rightColor;
            this.x = x;
            this.y = y;
        }

        @Override
        public void execute(Graphics2D g2d) {
            Color oldColor = g2d.getColor();
            Font oldFont = g2d.getFont();

            // Draw left text
            g2d.setFont(leftFont);
            g2d.setColor(leftColor);
            int stringWidth = g2d.getFontMetrics().stringWidth(leftText);
            g2d.drawString(leftText, x, y);

            // Draw right text
            g2d.setFont(rightFont);
            g2d.setColor(rightColor);
            g2d.drawString(rightText, x + stringWidth, y);

            g2d.setColor(oldColor);
            g2d.setFont(oldFont);
        }
    }
}