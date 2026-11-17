package com.tonic.ui;

import com.tonic.services.hotswapper.CycleButton;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

/**
 * VitaLite's sidebar look: RuneLite's fonts and colours with the Vita accent, shared icons, and the buttons panels are built from.
 */
public final class VitaTheme {
    public static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
    public static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
    public static final Color CARD_HOVER = ColorScheme.DARKER_GRAY_HOVER_COLOR;
    public static final Color TITLE = Color.WHITE;
    public static final Color TEXT = ColorScheme.TEXT_COLOR;
    public static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;
    public static final Color SUCCESS = new Color(0x28BE28);
    public static final Color WARNING = ColorScheme.PROGRESS_INPROGRESS_COLOR;
    public static final Color ERROR = new Color(0xE05050);
    public static final Color BUSY = ColorScheme.MEDIUM_GRAY_COLOR;

    public static final int GAP = 5;
    public static final int PADDING = 8;

    private static final String CONFIG = "/net/runelite/client/plugins/config/";
    private static ImageIcon cog;
    private static ImageIcon trash;
    private static ImageIcon reload;
    private static ImageIcon more;
    private static ImageIcon expanded;
    private static ImageIcon collapsed;

    private VitaTheme() {
    }

    /** @return the accent, read at use since VitaLite recolours RuneLite's brand colour at startup */
    public static Color accent() {
        return ColorScheme.BRAND_ORANGE;
    }

    /** @return the font for names and titles */
    public static Font titleFont() {
        return FontManager.getRunescapeBoldFont();
    }

    /** @return the font for descriptions, details and buttons */
    public static Font textFont() {
        return FontManager.getRunescapeSmallFont();
    }

    /** @return the regular RuneLite font */
    public static Font regularFont() {
        return FontManager.getRunescapeFont();
    }

    /** @return a muted cog */
    public static ImageIcon cog() {
        if (cog == null) {
            cog = icon(load(CONFIG + "config_edit_icon.png"), 16, MUTED);
        }
        return cog;
    }

    /** @return a muted bin */
    public static ImageIcon trash() {
        if (trash == null) {
            trash = icon(load(CONFIG + "mdi_delete.png"), 16, MUTED);
        }
        return trash;
    }

    /** @return a muted refresh arrow */
    public static ImageIcon reload() {
        if (reload == null) {
            reload = icon(ImageUtil.loadImageResource(CycleButton.class, "cycle.png"), 16, MUTED);
        }
        return reload;
    }

    /** @return three muted dots, for a menu of further actions */
    public static ImageIcon more() {
        if (more == null) {
            more = new ImageIcon(new BaseMultiResolutionImage(dots(1), dots(2)));
        }
        return more;
    }

    /**
     * @param open whether the section it toggles is open
     * @return a muted chevron, pointing down when open and right when closed
     */
    public static ImageIcon chevron(boolean open) {
        if (open) {
            if (expanded == null) {
                expanded = new ImageIcon(new BaseMultiResolutionImage(chevronGlyph(1, true), chevronGlyph(2, true)));
            }
            return expanded;
        }
        if (collapsed == null) {
            collapsed = new ImageIcon(new BaseMultiResolutionImage(chevronGlyph(1, false), chevronGlyph(2, false)));
        }
        return collapsed;
    }

    /**
     * @param image an image
     * @param size  the size to show it at
     * @param color the colour to paint its shape in, or null to keep its colours
     * @return it at that size, with a twice-as-large variant for scaled displays
     */
    public static ImageIcon icon(BufferedImage image, int size, Color color) {
        BufferedImage source = color == null ? image : ImageUtil.recolorImage(image, color);
        return new ImageIcon(new BaseMultiResolutionImage(ImageUtil.resizeImage(source, size, size, true), ImageUtil.resizeImage(source, size * 2, size * 2, true)));
    }

    /**
     * @param icon    its image
     * @param tooltip what it does
     * @param action  what happens on click
     * @return a borderless icon button that brightens under the mouse
     */
    public static JButton iconButton(ImageIcon icon, String tooltip, Runnable action) {
        JButton button = plainIconButton(icon, tooltip);
        button.addActionListener(e -> action.run());
        return button;
    }

    /**
     * @param tooltip what the menu holds
     * @param menu    builds the menu each time the button is clicked
     * @return a three-dot icon button that opens the menu under itself
     */
    public static JButton menuButton(String tooltip, Supplier<JPopupMenu> menu) {
        JButton button = plainIconButton(more(), tooltip);
        button.addActionListener(e -> menu.get().show(button, 0, button.getHeight()));
        return button;
    }

    /**
     * @param text   the label
     * @param color  its background
     * @param action what happens on click, or null for a button that cannot be pressed
     * @return a flat text button in RuneLite's Plugin Hub style
     */
    public static JButton textButton(String text, Color color, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(textFont());
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setBorder(new CompoundBorder(new LineBorder(color.darker()), new EmptyBorder(2, 8, 2, 8)));
        if (action == null) {
            button.setEnabled(false);
        } else {
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.addActionListener(e -> action.run());
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (button.isEnabled()) {
                        button.setBackground(color.brighter());
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (button.isEnabled()) {
                        button.setBackground(color);
                    }
                }
            });
        }
        return button;
    }

    /**
     * @param component a component that sits on a card
     * @return it, see-through so the card's colour shows behind it
     */
    public static <T extends JComponent> T clear(T component) {
        component.setOpaque(false);
        return component;
    }

    /**
     * @param layout its layout
     * @return a see-through panel for laying out a card's pieces
     */
    public static JPanel row(LayoutManager layout) {
        return clear(new JPanel(layout));
    }

    /**
     * @param button an icon button
     * @return it with no background, border or focus ring, so only its icon shows
     */
    public static <T extends AbstractButton> T bare(T button) {
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(2, 2, 2, 2));
        return button;
    }

    private static JButton plainIconButton(ImageIcon icon, String tooltip) {
        JButton button = new JButton(icon);
        SwingUtil.removeButtonDecorations(button);
        button.setRolloverIcon(brighter(icon));
        button.setToolTipText(tooltip);
        bare(button);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static ImageIcon brighter(ImageIcon icon) {
        Image image = icon.getImage();
        if (image instanceof BaseMultiResolutionImage) {
            BaseMultiResolutionImage variants = (BaseMultiResolutionImage) image;
            Image[] brighter = variants.getResolutionVariants().stream().map(variant -> ImageUtil.luminanceScale(variant, 1.6f)).toArray(Image[]::new);
            return new ImageIcon(new BaseMultiResolutionImage(brighter));
        }
        return new ImageIcon(ImageUtil.luminanceScale(image, 1.6f));
    }

    private static BufferedImage dots(int scale) {
        BufferedImage image = new BufferedImage(16 * scale, 16 * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(MUTED);
        int size = 3 * scale;
        for (int index = 0; index < 3; index++) {
            graphics.fillOval((2 + index * 5) * scale - 1 + scale, 8 * scale - size / 2, size, size);
        }
        graphics.dispose();
        return image;
    }

    private static BufferedImage chevronGlyph(int scale, boolean open) {
        BufferedImage image = new BufferedImage(16 * scale, 16 * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setColor(MUTED);
        graphics.setStroke(new BasicStroke(1.8f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Float path = new Path2D.Float();
        if (open) {
            path.moveTo(4.5f * scale, 6.5f * scale);
            path.lineTo(8f * scale, 10f * scale);
            path.lineTo(11.5f * scale, 6.5f * scale);
        } else {
            path.moveTo(6.5f * scale, 4.5f * scale);
            path.lineTo(10f * scale, 8f * scale);
            path.lineTo(6.5f * scale, 11.5f * scale);
        }
        graphics.draw(path);
        graphics.dispose();
        return image;
    }

    private static BufferedImage load(String path) {
        return ImageUtil.loadImageResource(PluginPanel.class, path);
    }
}
