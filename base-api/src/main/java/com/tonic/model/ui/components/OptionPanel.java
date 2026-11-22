package com.tonic.model.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A settings card: a title and a description that wrap to the card's width, with the control that sets the option, the card growing to fit its text.
 */
public class OptionPanel extends JPanel
{
    private static final int CARD_WIDTH = VPluginPanel.PANEL_WIDTH - 50;
    private static final int PADDING_X = 8;
    private static final int PADDING_Y = 5;
    private static final int GAP = 10;
    private static final int TEXT_WIDTH = CARD_WIDTH - PADDING_X * 2;
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font DESCRIPTION_FONT = new Font("Segoe UI", Font.PLAIN, 10);

    private final Color CARD_BACKGROUND = new Color(55, 55, 60);
    private final Color SEPARATOR_COLOR = new Color(70, 70, 75);
    private final Color TEXT_COLOR = new Color(200, 200, 205);
    private final Color HEADER_COLOR = new Color(64, 169, 211);
    private boolean isHovered = false;

    public OptionPanel()
    {
        setLayout(new BorderLayout(0, 2));
        setOpaque(false);
        setBorder(new EmptyBorder(PADDING_Y, PADDING_X, PADDING_Y, PADDING_X));
    }

    /**
     * Lays the option out with its title and a toggle on the top row and the description below.
     *
     * @param title the option's name
     * @param description what it does
     * @param toggle the toggle that sets it
     * @param onClick run when the toggle is clicked
     */
    public void init(String title, String description, ToggleSlider toggle, Runnable onClick)
    {
        JPanel toggleCell = new JPanel(new GridBagLayout());
        toggleCell.setOpaque(false);
        toggleCell.add(toggle);

        JPanel header = new JPanel(new BorderLayout(GAP, 0));
        header.setOpaque(false);
        header.add(wrapped(title, TITLE_FONT, HEADER_COLOR, TEXT_WIDTH - GAP - toggle.getPreferredSize().width), BorderLayout.CENTER);
        header.add(toggleCell, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(wrapped(description, DESCRIPTION_FONT, TEXT_COLOR, TEXT_WIDTH), BorderLayout.CENTER);

        toggle.addActionListener(e -> onClick.run());
        finish(toggle);
    }

    /**
     * Lays the option out with its title and description above a control that is not a toggle, such as a text field.
     *
     * @param title the option's name
     * @param description what it does
     * @param control the control that sets it
     */
    public void init(String title, String description, JComponent control)
    {
        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        controlRow.setOpaque(false);
        controlRow.setBorder(new EmptyBorder(4, 0, 0, 0));
        controlRow.add(control);

        add(wrapped(title, TITLE_FONT, HEADER_COLOR, TEXT_WIDTH), BorderLayout.NORTH);
        add(wrapped(description, DESCRIPTION_FONT, TEXT_COLOR, TEXT_WIDTH), BorderLayout.CENTER);
        add(controlRow, BorderLayout.SOUTH);

        finish(control);
    }

    private void finish(JComponent control)
    {
        MouseAdapter hover = hoverHighlight();
        addMouseListener(hover);
        control.addMouseListener(hover);
        setMaximumSize(new Dimension(VPluginPanel.PANEL_WIDTH - 20, getPreferredSize().height));
    }

    private static JLabel wrapped(String text, Font font, Color color, int width)
    {
        JLabel label = new JLabel();
        label.setFont(font);
        label.setForeground(color);
        FontMetrics metrics = label.getFontMetrics(font);
        StringBuilder html = new StringBuilder("<html>");
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+"))
        {
            if (line.length() > 0 && metrics.stringWidth(line + " " + word) > width)
            {
                html.append(escape(line.toString())).append("<br>");
                line.setLength(0);
            }
            if (line.length() > 0)
            {
                line.append(' ');
            }
            line.append(word);
        }
        html.append(escape(line.toString())).append("</html>");
        label.setText(html.toString());
        return label;
    }

    private static String escape(String text)
    {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private MouseAdapter hoverHighlight()
    {
        return new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                isHovered = false;
                repaint();
            }
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isHovered) {
            g2d.setColor(new Color(60, 60, 65));
        } else {
            g2d.setColor(CARD_BACKGROUND);
        }
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        g2d.setColor(SEPARATOR_COLOR);
        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
    }
}
