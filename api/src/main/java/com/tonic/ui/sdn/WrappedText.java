package com.tonic.ui.sdn;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Text that wraps at whatever width it is given and grows tall enough for every line, so nothing is ever cut off; with a
 * line limit, the last line ends in an ellipsis and the full text shows as a tooltip.
 */
final class WrappedText extends JComponent {
    private static final String ELLIPSIS = "…";

    private String text = "";
    private int maxLines;
    private boolean centered;
    private int laidOutWidth = -1;
    private List<String> lines = List.of();
    private boolean truncated;

    /**
     * @param text     the text
     * @param font     its font
     * @param color    its colour
     * @param maxLines the most lines to show, or 0 for no limit
     */
    WrappedText(String text, Font font, Color color, int maxLines) {
        setFont(font);
        setForeground(color);
        setOpaque(false);
        this.maxLines = maxLines;
        setText(text);
    }

    /**
     * @param text the text, with \n for line breaks
     */
    void setText(String text) {
        this.text = text == null ? "" : text;
        laidOutWidth = -1;
        revalidate();
        repaint();
    }

    /**
     * @param maxLines the most lines to show, or 0 for no limit
     */
    void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
        laidOutWidth = -1;
        revalidate();
        repaint();
    }

    /**
     *
     */
    void setCentered() {
        this.centered = true;
        repaint();
    }

    /** @return whether the line limit hid some of the text */
    boolean isTruncated() {
        layout(currentWidth());
        return truncated;
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        int width = currentWidth();
        layout(width);
        Insets insets = getInsets();
        FontMetrics metrics = getFontMetrics(getFont());
        return new Dimension(Math.min(width, naturalWidth(metrics)) + insets.left + insets.right, Math.max(1, lines.size()) * metrics.getHeight() + insets.top + insets.bottom);
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension preferred = getPreferredSize();
        return new Dimension(0, preferred.height);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean resized = width != getWidth();
        super.setBounds(x, y, width, height);
        if (resized && laidOutWidth != innerWidth(width)) {
            int before = lines.size();
            layout(innerWidth(width));
            if (lines.size() != before) {
                SwingUtilities.invokeLater(this::revalidate);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        layout(innerWidth(getWidth()));
        Graphics2D g = (Graphics2D) graphics.create();
        Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (hints instanceof Map) {
            g.addRenderingHints((Map<?, ?>) hints);
        } else {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        g.setFont(getFont());
        g.setColor(isEnabled() ? getForeground() : getForeground().darker());
        FontMetrics metrics = g.getFontMetrics(getFont());
        Insets insets = getInsets();
        int width = innerWidth(getWidth());
        int y = insets.top + metrics.getAscent();
        for (String line : lines) {
            int x = insets.left + (centered ? Math.max(0, (width - metrics.stringWidth(line)) / 2) : 0);
            g.drawString(line, x, y);
            y += metrics.getHeight();
        }
        g.dispose();
    }

    private int currentWidth() {
        if (getWidth() > 0) {
            return innerWidth(getWidth());
        }
        Container parent = getParent();
        while (parent != null && parent.getWidth() <= 0) {
            parent = parent.getParent();
        }
        int width = parent == null ? HubTheme.fallbackWidth() : parent.getWidth();
        return Math.max(1, width - getInsets().left - getInsets().right);
    }

    private int innerWidth(int width) {
        Insets insets = getInsets();
        return Math.max(1, width - insets.left - insets.right);
    }

    private int naturalWidth(FontMetrics metrics) {
        int widest = 1;
        for (String paragraph : text.split("\n", -1)) {
            widest = Math.max(widest, metrics.stringWidth(paragraph));
        }
        return widest;
    }

    private void layout(int width) {
        if (width == laidOutWidth) {
            return;
        }
        laidOutWidth = width;
        FontMetrics metrics = getFontMetrics(getFont());
        List<String> wrapped = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            wrap(paragraph, width, metrics, wrapped);
        }
        truncated = maxLines > 0 && wrapped.size() > maxLines;
        if (truncated) {
            List<String> kept = new ArrayList<>(wrapped.subList(0, maxLines));
            kept.set(maxLines - 1, ellipsize(kept.get(maxLines - 1), width, metrics));
            wrapped = kept;
        }
        lines = wrapped;
        setToolTipText(truncated ? "<html><p width=\"260\">" + escape(text).replace("\n", "<br>") + "</p></html>" : null);
    }

    private static void wrap(String paragraph, int width, FontMetrics metrics, List<String> lines) {
        String[] words = paragraph.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (metrics.stringWidth(candidate) <= width) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
            }
            String rest = word;
            while (metrics.stringWidth(rest) > width && rest.length() > 1) {
                int cut = rest.length() - 1;
                while (cut > 1 && metrics.stringWidth(rest.substring(0, cut)) > width) {
                    cut--;
                }
                lines.add(rest.substring(0, cut));
                rest = rest.substring(cut);
            }
            line.append(rest);
        }
        lines.add(line.toString());
    }

    private static String ellipsize(String line, int width, FontMetrics metrics) {
        String shortened = line;
        while (!shortened.isEmpty() && metrics.stringWidth(shortened + ELLIPSIS) > width) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened.stripTrailing() + ELLIPSIS;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
